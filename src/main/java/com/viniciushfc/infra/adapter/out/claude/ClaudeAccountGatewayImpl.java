package com.viniciushfc.infra.adapter.out.claude;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.enums.ClaudeLoginMethod;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.gateways.ClaudeAccountGateway;
import com.viniciushfc.domain.vo.ClaudeStatus;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ApplicationScoped
public class ClaudeAccountGatewayImpl implements ClaudeAccountGateway {

    static final Duration STATUS_TIMEOUT = Duration.ofSeconds(30);
    static final Duration LOGIN_TIMEOUT = Duration.ofMinutes(10);

    private static final Pattern URL = Pattern.compile("https://[^\\s\\u001b\\u0007]+");
    private static final Pattern ESCAPES = Pattern.compile("\\u001b\\][^\\u0007\\u001b]*(\\u0007|\\u001b\\\\)|\\u001b\\[[0-9;]*[A-Za-z]");

    private final ObjectMapper objectMapper;

    private volatile Process login;
    private volatile String loginUrl;
    private volatile String loginError;

    public ClaudeAccountGatewayImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ClaudeStatus status() {
        String executable = ClaudeCli.executable();
        if (executable == null) {
            return new ClaudeStatus(false, false, null, null, false, null, null);
        }
        boolean loggingIn = login != null && login.isAlive();
        try {
            JsonNode json = objectMapper.readTree(
                    ClaudeCli.run(List.of(executable, "auth", "status", "--json"), null, STATUS_TIMEOUT).output());
            return new ClaudeStatus(true, json.path("loggedIn").asBoolean(),
                    json.path("email").asText(null), json.path("subscriptionType").asText(null),
                    loggingIn, loggingIn ? loginUrl : null, loginError);
        } catch (IOException | TimeoutException e) {
            Log.warn("Não foi possível ler o login do Claude Code", e);
            return new ClaudeStatus(true, false, null, null, loggingIn, loggingIn ? loginUrl : null, loginError);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalServiceException("A consulta ao Claude Code foi interrompida.");
        }
    }

    @Override
    public synchronized void login(ClaudeLoginMethod method, Runnable onLoggedIn) {
        String executable = ClaudeCli.executable();
        if (executable == null) {
            throw new BusinessException("Instale o Claude Code neste computador para usar o Claude.",
                    ErrorCode.CLAUDE_NOT_INSTALLED);
        }
        if (login != null && login.isAlive()) {
            login.destroy();
        }
        loginUrl = null;
        loginError = null;
        try {
            Process process = ClaudeCli.start(List.of(executable, "auth", "login", "--" + method.name().toLowerCase()));
            login = process;
            CompletableFuture.delayedExecutor(LOGIN_TIMEOUT.toMinutes(), TimeUnit.MINUTES).execute(process::destroy);
            Thread.ofVirtual().start(() -> watchLogin(process, onLoggedIn));
        } catch (IOException e) {
            Log.error("Não foi possível iniciar o login do Claude Code", e);
            throw new ExternalServiceException("Não foi possível abrir o login do Claude. Os detalhes estão no terminal do Vagora.");
        }
    }

    private void watchLogin(Process process, Runnable onLoggedIn) {
        String last = null;
        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher url = URL.matcher(line);
                if (loginUrl == null && url.find()) {
                    loginUrl = url.group();
                }
                String text = ESCAPES.matcher(line).replaceAll("").strip();
                if (!text.isEmpty()) {
                    last = text;
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                onLoggedIn.run();
            } else if (process == login) {
                Log.warnf("Login do Claude Code terminou com código %d: %s", exitCode, last);
                loginError = loginFailure(last);
            }
        } catch (IOException | RuntimeException e) {
            Log.error("Falha no login do Claude Code", e);
            loginError = "Não foi possível terminar o login do Claude. Os detalhes estão no terminal do Vagora.";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static String loginFailure(String lastLine) {
        if (lastLine == null) {
            return "O login do Claude foi cancelado ou demorou demais. Tente de novo.";
        }
        String message = lastLine.replaceFirst("^.*prompted >\\s*", "");
        return message.isBlank() ? "O login do Claude foi cancelado ou demorou demais. Tente de novo." : message;
    }

    @Override
    public synchronized void submitCode(String code) {
        Process process = login;
        if (process == null || !process.isAlive()) {
            throw new BusinessException("O login do Claude não está aberto. Clique em Entrar no Claude de novo.");
        }
        try {
            var in = process.getOutputStream();
            in.write((code + "\n").getBytes(StandardCharsets.UTF_8));
            in.flush();
        } catch (IOException e) {
            Log.warn("Não foi possível enviar o código ao login do Claude Code", e);
            throw new ExternalServiceException("O login do Claude foi fechado. Clique em Entrar no Claude de novo.");
        }
    }
}
