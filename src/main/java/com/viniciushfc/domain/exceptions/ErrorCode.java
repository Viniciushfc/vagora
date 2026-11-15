package com.viniciushfc.domain.exceptions;

/** Falhas que quem mostra o erro trata de forma específica (ex.: o front oferece o atalho certo). */
public enum ErrorCode {
    LINKEDIN_CHECKPOINT,
    LINKEDIN_LOGIN_FAILED,
    LINKEDIN_NOT_CONNECTED,
    LINKEDIN_PAGE_UNREADABLE,
    SCRAPER_FAILED,
    GEMINI_KEY_MISSING,
    GEMINI_KEY_REJECTED
}
