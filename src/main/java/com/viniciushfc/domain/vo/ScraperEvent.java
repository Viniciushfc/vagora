package com.viniciushfc.domain.vo;

import java.time.LocalDateTime;

/** Um passo do leitor de vagas, já em texto para mostrar a quem acompanha. */
public record ScraperEvent(LocalDateTime at, String message, boolean problem) {
}
