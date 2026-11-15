package com.viniciushfc.domain.usecases.contracts;

import java.util.UUID;

public interface DeleteUserAccountUseCase {
    void execute(UUID id);
}
