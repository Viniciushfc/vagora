package com.viniciushfc.infra.adapter.out.scraper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.ScraperActivity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class JobDetailsGatewayImpl implements JobDetailsGateway {

    static final int EXIT_CHECKPOINT = 3;
    static final int EXIT_LOGIN_FAILED = 4;
    static final int EXIT_PAGE_UNREADABLE = 5;

    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private static final String EVENT_PREFIX = "@@ ";
    private static final String PYTHON = "python/.venv/bin/python";
    private static final String SCRIPT = "python/scraping_detalis_job.py";
    private static final Path SCREENSHOT = Path.of("python/last-failure.png");

    private final ObjectMapper mapper;
    private final ScraperActivityRepository activity;

    public JobDetailsGatewayImpl(ObjectMapper mapper, ScraperActivityRepository activity) {
        this.mapper = mapper;
        this.activity = activity;
    }

    @Override
    public List<JobDetail> extractJobsDetails(Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser) {
        boolean useVirtualDisplay = !showBrowser && xvfbAvailable();

        ProcessBuilder pb = new ProcessBuilder(command(useVirtualDisplay));
        pb.environment().put("LINKEDIN_LOGIN", linkedinAccount.username());
        pb.environment().put("LINKEDIN_PASSWORD", linkedinAccount.password());
        pb.environment().put("VAGORA_HEADLESS", !showBrowser && !useVirtualDisplay ? "1" : "0");
        pb.environment().put("VAGORA_INTERACTIVE", showBrowser ? "1" : "0");
        if (useVirtualDisplay) {
            pb.environment().remove("WAYLAND_DISPLAY");
            pb.environment().put("XDG_SESSION_TYPE", "x11");
        }

        try {
            Process process = pb.start();
            Thread stderr = Thread.ofVirtual().start(() -> process.errorReader().lines().forEach(this::onScraperLine));

            AtomicBoolean timedOut = new AtomicBoolean();
            CompletableFuture.delayedExecutor(TIMEOUT.toMinutes(), TimeUnit.MINUTES).execute(() -> {
                if (process.isAlive()) {
                    timedOut.set(true);
                    process.descendants().forEach(ProcessHandle::destroyForcibly);
                    process.destroyForcibly();
                }
            });

            mapper.writeValue(process.getOutputStream(), jobLinks);
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            stderr.join(Duration.ofSeconds(5));

            if (timedOut.get()) {
                throw fail(new ExternalServiceException("O leitor de vagas passou de " + TIMEOUT.toMinutes()
                        + " minutos e foi interrompido. O alerta será tentado de novo.", ErrorCode.SCRAPER_FAILED));
            }
            if (exitCode != 0) {
                throw fail(failure(exitCode));
            }
            return mapper.readValue(output, new TypeReference<>() {
            });
        } catch (IOException e) {
            Log.error("Não foi possível executar o scraper", e);
            throw fail(new ExternalServiceException(
                    "Não foi possível iniciar o leitor de vagas. Rode ./run.sh para reinstalar as dependências.", ErrorCode.SCRAPER_FAILED));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Leitura das vagas interrompida.", e);
        }
    }

    @Override
    public byte[] lastFailureScreenshot() {
        try {
            return Files.exists(SCREENSHOT) ? Files.readAllBytes(SCREENSHOT) : null;
        } catch (IOException e) {
            return null;
        }
    }

    private void onScraperLine(String line) {
        if (!line.startsWith(EVENT_PREFIX)) {
            System.err.println(line);
            return;
        }
        try {
            update(applyEvent(mapper.readTree(line.substring(EVENT_PREFIX.length()))));
        } catch (IOException e) {
            Log.debugf("Evento do scraper ilegível: %s", line);
        }
    }

    static UnaryOperator<ScraperActivity> applyEvent(JsonNode event) {
        int index = event.path("index").asInt();
        int total = event.path("total").asInt();
        return switch (event.path("event").asText()) {
            case "start" -> a -> a.started(total);
            case "job" -> a -> a.reading(index, "Abrindo a vaga " + index + " de " + total);
            case "read" -> a -> a.event("Vaga " + index + " lida: " + event.path("title").asText(), false);
            case "closed" -> a -> a.event("Vaga " + index + " não aceita mais candidaturas: pulada", false);
            case "unreadable" -> a -> a.skipped("Vaga " + index + ": a descrição não carregou a tempo, pulada");
            case "pause" -> a -> a.event("Pausa de " + event.path("seconds").asInt() + " s antes da próxima vaga, como uma pessoa faria", false);
            case "login" -> a -> a.event("Sessão do LinkedIn expirada: entrando com a sua conta", false);
            case "checkpoint_wait" -> a -> a.event("O LinkedIn pediu uma verificação de segurança: resolva na janela do navegador (até 5 minutos)", true);
            case "checkpoint_resolved" -> a -> a.event("Verificação de segurança resolvida", false);
            case "done" -> a -> a.finished("Leitura concluída: " + event.path("count").asInt() + " vagas lidas");
            default -> a -> a; // "fail": a explicação vem do código de saída
        };
    }

    private void update(UnaryOperator<ScraperActivity> change) {
        activity.save(change.apply(activity.get()));
    }

    private ExternalServiceException fail(ExternalServiceException e) {
        update(a -> a.failed(e.getMessage(), Files.exists(SCREENSHOT)));
        return e;
    }

    static List<String> command(boolean useVirtualDisplay) {
        List<String> command = new ArrayList<>();
        if (useVirtualDisplay) {
            command.add("xvfb-run");
            command.add("-a");
        }
        command.add(PYTHON);
        command.add(SCRIPT);
        return command;
    }

    static ExternalServiceException failure(int exitCode) {
        return switch (exitCode) {
            case EXIT_CHECKPOINT -> new ExternalServiceException(
                    "O LinkedIn pediu uma verificação de segurança. Ative \"Mostrar o navegador\" no Perfil "
                            + "e resolva na janela que abrir na próxima análise.", ErrorCode.LINKEDIN_CHECKPOINT);
            case EXIT_LOGIN_FAILED -> new ExternalServiceException(
                    "Não foi possível entrar no LinkedIn. Confira o login e a senha em Contas.", ErrorCode.LINKEDIN_LOGIN_FAILED);
            case EXIT_PAGE_UNREADABLE -> new ExternalServiceException(
                    "O LinkedIn não mostrou a descrição da vaga a tempo. Pode ser lentidão, uma mudança na página "
                            + "ou uma tela pedindo login. Veja a captura da tela na página do LinkedIn.", ErrorCode.LINKEDIN_PAGE_UNREADABLE);
            default -> new ExternalServiceException(
                    "O leitor de vagas parou por um erro inesperado. Os detalhes técnicos estão no terminal do Vagora.",
                    ErrorCode.SCRAPER_FAILED);
        };
    }

    private static boolean xvfbAvailable() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (new File(dir, "xvfb-run").canExecute()) {
                return true;
            }
        }
        return false;
    }
}
