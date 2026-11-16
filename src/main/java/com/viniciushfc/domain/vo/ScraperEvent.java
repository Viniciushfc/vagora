package com.viniciushfc.domain.vo;

import java.time.LocalDateTime;

public record ScraperEvent(LocalDateTime at, String message, boolean problem) {
}
