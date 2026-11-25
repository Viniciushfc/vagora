package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.vo.ChatMessage;

import java.util.List;
import java.util.UUID;

public interface ChatJobMatchUseCase {
    String execute(UUID jobMatchId, List<ChatMessage> messages);
}
