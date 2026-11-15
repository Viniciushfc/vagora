package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;

public interface UpdateEmailFolderUseCase {
    Profile execute(String folder);
}
