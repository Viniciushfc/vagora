package com.viniciushfc.infra.adapter.out.scraper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.JobDetailsGateway;
import com.viniciushfc.domain.vo.JobDetail;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.time.Duration;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executa o scraper em Python. Com o navegador escondido, usa uma tela virtual (xvfb-run)
 * quando existe: o Chrome continua "normal" para o LinkedIn. Sem xvfb, cai no modo headless.
 */
@ApplicationScoped
public class JobDetailsGatewayImpl implements JobDetailsGateway {

    // códigos de saída combinados com python/scraping_detalis_job.py
    static final int EXIT_CHECKPOINT = 3;
    static final int EXIT_LOGIN_FAILED = 4;

    // shortcut: limite fixo; ~10 vagas com pausas + 5 min de verificação cabem com folga. Aumente se os alertas crescerem.
    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private static final String PYTHON = "python/.venv/bin/python";
    private static final String SCRIPT = "python/scraping_detalis_job.py";

    private final ObjectMapper mapper;

    public JobDetailsGatewayImpl(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<JobDetail> extractJobsDetails(Set<String> jobLinks, LinkedAccount linkedinAccount, boolean showBrowser) {
        boolean useVirtualDisplay = !showBrowser && xvfbAvailable();

        ProcessBuilder pb = new ProcessBuilder(command(useVirtualDisplay)).redirectError(ProcessBuilder.Redirect.INHERIT);
        pb.environment().put("LINKEDIN_LOGIN", linkedinAccount.username());
        pb.environment().put("LINKEDIN_PASSWORD", linkedinAccount.password());
        pb.environment().put("VAGORA_HEADLESS", !showBrowser && !useVirtualDisplay ? "1" : "0");
        pb.environment().put("VAGORA_INTERACTIVE", showBrowser ? "1" : "0");

        try {
            Process process = pb.start();
            // sem isso, um Chrome travado prende o monitoramento para sempre
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

            if (timedOut.get()) {
                throw new ExternalServiceException("O leitor de vagas do LinkedIn passou de " + TIMEOUT.toMinutes()
                        + " minutos e foi interrompido. O alerta será tentado de novo.");
            }
            if (exitCode != 0) {
                throw failure(exitCode);
            }
            return mapper.readValue(output, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new ExternalServiceException("Falha ao executar o leitor de vagas do LinkedIn: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("Leitura das vagas interrompida.", e);
        }
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
            default -> new ExternalServiceException(
                    "O leitor de vagas do LinkedIn falhou (código " + exitCode + "). O alerta será tentado de novo.");
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
