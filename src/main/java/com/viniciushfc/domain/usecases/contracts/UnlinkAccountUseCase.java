package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;

public interface UnlinkAccountUseCase {
    Profile execute(ExternalProvider provider);
}
