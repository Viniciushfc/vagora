package com.viniciushfc.infra.adapter.out.browser;

import com.microsoft.playwright.*;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

@ApplicationScoped
public class BrowserManager {

    private static final int PORT = 8085;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "playwright-thread"));

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    void onStart(@Observes StartupEvent ev) {
        executor.submit(() -> {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setChannel("chrome")
                            .setHeadless(false)
                            .setArgs(List.of(
                                    "--disable-blink-features=AutomationControlled",
                                    "--remote-debugging-port=" + PORT
                            ))
            );

            context = browser.newContext(
                    new Browser.NewContextOptions()
                            .setLocale("pt-BR")
                            .setTimezoneId("America/Sao_Paulo")
                            .setViewportSize(1366, 768)
                            .setExtraHTTPHeaders(Map.of(
                                    "Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7"))
            );

            context.addInitScript(
                    "Object.defineProperty(navigator, 'webdriver', { get: () => undefined });");

            System.out.println("Browser rodando em http://localhost:" + PORT);
        });
    }

    public <T> T execute(Function<BrowserContext, T> action) {
        try {
            return executor.submit(() -> action.apply(context)).get(60, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Erro executando ação no browser", e);
        }
    }

    void onStop(@Observes ShutdownEvent ev) throws Exception {
        executor.submit(() -> {
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
        }).get();
        executor.shutdown();
    }
}