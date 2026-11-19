package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ClaudeLoginMethod;
import com.viniciushfc.domain.vo.ClaudeStatus;

public interface ConnectClaudeUseCase {

    ClaudeStatus status();

    Profile execute(ClaudeLoginMethod method);

    void submitCode(String code);
}
