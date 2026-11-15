package com.viniciushfc.domain.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * O que o leitor de vagas do LinkedIn está fazendo, para acompanhar pela interface.
 *
 * @param running           lendo vagas agora
 * @param analyzing         o Gemini está analisando as vagas lidas
 * @param jobIndex          vaga sendo lida (começa em 1)
 * @param jobTotal          vagas do alerta atual
 * @param events            passos mais recentes primeiro
 * @param failure           explicação da última falha, ou null
 * @param screenshot existe captura da tela do último problema (falha ou vaga pulada)
 */
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

    public ScraperActivity reading(int index, String message) {
        return new ScraperActivity(running, false, index, jobTotal, events, failure, screenshot).event(message, false);
    }

    public ScraperActivity event(String message, boolean problem) {
        List<ScraperEvent> next = new ArrayList<>(MAX_EVENTS);
        next.add(new ScraperEvent(LocalDateTime.now(), message, problem));
        next.addAll(events.subList(0, Math.min(events.size(), MAX_EVENTS - 1)));
        return new ScraperActivity(running, analyzing, jobIndex, jobTotal, List.copyOf(next), failure, screenshot);
    }

    /** Uma vaga não carregou e foi pulada; a captura da tela fica disponível. */
    public ScraperActivity skipped(String message) {
        return new ScraperActivity(running, false, jobIndex, jobTotal, events, failure, true).event(message, true);
    }

    public ScraperActivity analyzing(int index, int total, String title) {
        return new ScraperActivity(false, true, index, total, events, null, screenshot)
                .event("Gemini analisando a vaga " + index + " de " + total + ": " + title, false);
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
