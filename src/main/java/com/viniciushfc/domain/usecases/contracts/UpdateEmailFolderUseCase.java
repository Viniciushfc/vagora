package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;

public interface UpdateEmailFolderUseCase {
    Profile execute(AlertSource source, String folder);
}
