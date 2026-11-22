package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.enums.ClaudeLoginMethod;
import com.viniciushfc.domain.vo.ClaudeStatus;

public interface ClaudeAccountGateway {

    ClaudeStatus status();

    void login(ClaudeLoginMethod method, Runnable onLoggedIn);

    void submitCode(String code);

    void install();
}
