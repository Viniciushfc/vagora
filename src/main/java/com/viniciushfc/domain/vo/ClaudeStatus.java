package com.viniciushfc.domain.vo;

public record ClaudeStatus(boolean installed,
                           boolean loggedIn,
                           String email,
                           String plan,
                           boolean loggingIn,
                           String loginUrl,
                           String loginError) {
}
