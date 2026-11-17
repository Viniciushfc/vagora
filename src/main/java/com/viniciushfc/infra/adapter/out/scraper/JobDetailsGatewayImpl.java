package com.viniciushfc.infra.adapter.out.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.exceptions.ConflictException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.ScraperActivity;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@ApplicationScoped
public class JobDetailsGatewayImpl implements JobDetailsGateway {

    static final int EXIT_CHECKPOINT = 3;
    static final int EXIT_LOGIN_FAILED = 4;
    static final int EXIT_PAGE_UNREADABLE = 5;
    static final int EXIT_BROWSER_CLOSED = 6;

    static final Duration TIMEOUT = Duration.ofMinutes(30);
    static final Duration LOGIN_TIMEOUT = Duration.ofMinutes(8);

    private static final String EVENT_PREFIX = "@@ ";
    private static final String PYTHON = "python/.venv/bin/python";
    private static final String SCRIPT = "python/scraping_detalis_job.py";
    private static final Path SCREENSHOT = Path.of("python/last-failure.png");

    private final ObjectMapper mapper;
    private final ScraperActivityRepository activity;
    private final Semaphore browserInUse = new Semaphore(1);

    public JobDetailsGatewayImpl(ObjectMapper mapper, ScraperActivityRepository activity) {
        this.mapper = mapper;
        this.activity = activity;
    }

    @Override
    public void extractJobsDetails(Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser,
                                   Consumer<JobDetail> onJobRead) {
        browserInUse.acquireUninterruptibly();
        try {
            readJobs(jobLinks, linkedinAccount, showBrowser, onJobRead);
        } finally {
            browserInUse.release();
        }
    }

    @Override
    public void openLoginWindow(LinkedAccount linkedinAccount, Runnable onSessionSaved) {
        if (!browserInUse.tryAcquire()) {
            throw new ConflictException("O leitor do LinkedIn está usando o navegador agora. Espere ele terminar e tente de novo.");
        }
        update(ScraperActivity::loggingIn);
        Thread.ofVirtual().start(() -> {
            try {
                if (login(linkedinAccount)) {
                    onSessionSaved.run();
                }
            } finally {
                browserInUse.release();
            }
        });
    }

    private boolean login(LinkedAccount linkedinAccount) {
        List<String> command = command(false);
        command.add("--login");
        try {
            Process process = processBuilder(command, linkedinAccount, true, false).start();
            process.getOutputStream().close();
            Thread stderr = Thread.ofVirtual().start(() -> process.errorReader().lines().forEach(this::onScraperLine));
            if (!process.waitFor(LOGIN_TIMEOUT.toMinutes(), TimeUnit.MINUTES)) {
                kill(process);
            }
            stderr.join(Duration.ofSeconds(5));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                fail(loginFailure(exitCode));
            }
            return exitCode == 0;
        } catch (IOException e) {
            Log.error("Não foi possível abrir o navegador para o login", e);
            fail(new ExternalServiceException(
                    "Não foi possível abrir o navegador. Rode ./run.sh para reinstalar as dependências.", ErrorCode.SCRAPER_FAILED));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    private void readJobs(Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser, Consumer<JobDetail> onJobRead) {
        boolean useVirtualDisplay = !showBrowser && xvfbAvailable();
        ProcessBuilder pb = processBuilder(command(useVirtualDisplay), linkedinAccount, showBrowser, useVirtualDisplay);

        Process process = null;
        try {
            process = pb.start();
            Process started = process;
            Thread stderr = Thread.ofVirtual().start(() -> started.errorReader().lines().forEach(this::onScraperLine));

            AtomicBoolean timedOut = new AtomicBoolean();
            CompletableFuture.delayedExecutor(TIMEOUT.toMinutes(), TimeUnit.MINUTES).execute(() -> {
                if (started.isAlive()) {
                    timedOut.set(true);
                    kill(started);
                }
            });

            mapper.writeValue(process.getOutputStream(), jobLinks);
            try (BufferedReader stdout = process.inputReader()) {
                for (String line = stdout.readLine(); line != null; line = stdout.readLine()) {
                    if (!line.isBlank()) {
                        onJobRead.accept(mapper.readValue(line, JobDetail.class));
                    }
                }
            }
            int exitCode = process.waitFor();
            stderr.join(Duration.ofSeconds(5));

            if (timedOut.get()) {
                throw fail(new ExternalServiceException("O leitor de vagas passou de " + TIMEOUT.toMinutes()
                        + " minutos e foi interrompido. O alerta será tentado de novo.", ErrorCode.SCRAPER_FAILED));
            }
            if (exitCode != 0) {
                throw fail(failure(exitCode));
            }
        } catch (IOException e) {
            Log.error("Não foi possível executar o scraper", e);
            throw fail(new ExternalServiceException(
                    "Não foi possível iniciar o leitor de vagas. Rode ./run.sh para reinstalar as dependências.", ErrorCode.SCRAPER_FAILED));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Leitura das vagas interrompida.", e);
        } finally {
            if (process != null && process.isAlive()) {
                kill(process);
            }
        }
    }

    private static void kill(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
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
            case "checkpoint_wait" -> a -> a.event("O LinkedIn pediu uma verificação de segurança: aprove no app do celular ou resolva na janela do navegador (até 5 minutos)", true);
            case "checkpoint_resolved" -> a -> a.event("Verificação de segurança resolvida", false);
            case "session_wait" -> a -> a.event("Entre no LinkedIn na janela que abriu: login e senha já vão preenchidos, "
                    + "resolva o captcha, o código ou a aprovação no app do celular se aparecer (até 5 minutos)", true);
            case "session_saved" -> a -> a.finished("Sessão do LinkedIn salva: as próximas análises abrem as vagas direto");
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

    private static ProcessBuilder processBuilder(List<String> command, LinkedAccount linkedinAccount,
                                                 boolean showBrowser, boolean useVirtualDisplay) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.environment().put("LINKEDIN_LOGIN", linkedinAccount.username());
        pb.environment().put("LINKEDIN_PASSWORD", linkedinAccount.password());
        pb.environment().put("VAGORA_HEADLESS", !showBrowser && !useVirtualDisplay ? "1" : "0");
        if (useVirtualDisplay) {
            pb.environment().remove("WAYLAND_DISPLAY");
            pb.environment().put("XDG_SESSION_TYPE", "x11");
        }
        return pb;
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
                    "O LinkedIn pediu uma verificação de segurança. Clique em \"Entrar no LinkedIn\" em Contas "
                            + "e resolva na janela que abrir.", ErrorCode.LINKEDIN_CHECKPOINT);
            case EXIT_LOGIN_FAILED -> new ExternalServiceException(
                    "Não foi possível entrar no LinkedIn. Confira o login e a senha em Contas.", ErrorCode.LINKEDIN_LOGIN_FAILED);
            case EXIT_PAGE_UNREADABLE -> new ExternalServiceException(
                    "O LinkedIn não mostrou a descrição da vaga a tempo. Pode ser lentidão, uma mudança na página "
                            + "ou uma tela pedindo login. Veja a captura da tela na página do LinkedIn.", ErrorCode.LINKEDIN_PAGE_UNREADABLE);
            case EXIT_BROWSER_CLOSED -> new ExternalServiceException(
                    "O navegador do Vagora foi fechado no meio da leitura. As vagas já lidas ficaram guardadas "
                            + "e as que faltam serão abertas na próxima tentativa.", ErrorCode.LINKEDIN_BROWSER_CLOSED);
            default -> new ExternalServiceException(
                    "O leitor de vagas parou por um erro inesperado. Os detalhes técnicos estão no terminal do Vagora.",
                    ErrorCode.SCRAPER_FAILED);
        };
    }

    static ExternalServiceException loginFailure(int exitCode) {
        return switch (exitCode) {
            case EXIT_CHECKPOINT -> new ExternalServiceException("O login no LinkedIn não terminou em 5 minutos. "
                    + "Clique em \"Entrar no LinkedIn\" em Contas para tentar de novo.", ErrorCode.LINKEDIN_CHECKPOINT);
            case EXIT_BROWSER_CLOSED -> new ExternalServiceException("A janela foi fechada antes de o login terminar. "
                    + "Clique em \"Entrar no LinkedIn\" em Contas para tentar de novo.", ErrorCode.LINKEDIN_BROWSER_CLOSED);
            default -> failure(exitCode);
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
