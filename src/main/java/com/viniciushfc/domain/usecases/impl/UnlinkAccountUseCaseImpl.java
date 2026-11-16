package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.UnlinkAccountUseCase;

@UseCase
public class UnlinkAccountUseCaseImpl implements UnlinkAccountUseCase {

    private final ProfileRepository profileRepository;

    public UnlinkAccountUseCaseImpl(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public Profile execute(ExternalProvider provider) {
        Profile profile = profileRepository.require();
        if (profile.findLinkedAccount(provider) == null) {
            throw new NotFoundException("Nenhuma conta de " + provider.label + " conectada.");
        }
        return profileRepository.updateLinkedAccounts(profile.id(), profile.linkedAccounts().stream()
                .filter(account -> account.externalProvider() != provider)
                .toList());
    }
}
