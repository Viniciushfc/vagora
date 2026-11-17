package com.viniciushfc.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record ScraperActivity(boolean running,
                              boolean analyzing,
                              int jobIndex,
                              int jobTotal,
                              List<ScraperEvent> events,
                              String failure,
                              boolean screenshot) {

    static final int MAX_EVENTS = 60;

    public static ScraperActivity idle() {
        return new ScraperActivity(false, false, 0, 0, List.of(), null, false);
    }

    public ScraperActivity started(int total) {
        return new ScraperActivity(true, false, 0, total, events, null, false)
                .event("Iniciando o navegador para ler " + total + (total == 1 ? " vaga" : " vagas"), false);
    }

    public ScraperActivity loggingIn() {
        return new ScraperActivity(true, false, 0, 0, events, null, false)
                .event("Abrindo o navegador para você entrar no LinkedIn", false);
    }

    public ScraperActivity reading(int index, String message) {
        return new ScraperActivity(running, false, index, jobTotal, events, failure, screenshot).event(message, false);
    }

    public ScraperActivity event(String message, boolean problem) {
        List<ScraperEvent> next = new ArrayList<>(MAX_EVENTS);
        next.add(new ScraperEvent(LocalDateTime.now(), message, problem));
        next.addAll(events.subList(0, Math.min(events.size(), MAX_EVENTS - 1)));
        return new ScraperActivity(running, analyzing, jobIndex, jobTotal, List.copyOf(next), failure, screenshot);
    }

    public ScraperActivity skipped(String message) {
        return new ScraperActivity(running, false, jobIndex, jobTotal, events, failure, true).event(message, true);
    }

    public ScraperActivity analyzing(int total) {
        return new ScraperActivity(false, true, 0, total, events, null, screenshot)
                .event("Gemini analisando " + total + (total == 1 ? " vaga" : " vagas") + " de uma vez", false);
    }

    public ScraperActivity adapting(String title) {
        return event("Vaga compatível: Gemini escrevendo o currículo adaptado para " + title, false);
    }

    public ScraperActivity analysisFinished(int matches, int total) {
        return new ScraperActivity(false, false, jobIndex, jobTotal, events, null, screenshot)
                .event("Análise concluída: " + matches + (matches == 1 ? " compatível" : " compatíveis") + " de " + total, false);
    }

    public ScraperActivity finished(String message) {
        return new ScraperActivity(false, false, jobIndex, jobTotal, events, null, screenshot).event(message, false);
    }

    public ScraperActivity failed(String explanation, boolean screenshot) {
        return new ScraperActivity(false, false, jobIndex, jobTotal, events, explanation, screenshot).event(explanation, true);
    }
}
