package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ClaudeLoginMethod;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.BusinessException;
import com.viniciushfc.domain.exceptions.ErrorCode;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.ClaudeAccountGateway;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.ConnectClaudeUseCase;
import com.viniciushfc.domain.vo.ClaudeStatus;

import java.util.ArrayList;
import java.util.List;

@UseCase
public class ConnectClaudeUseCaseImpl implements ConnectClaudeUseCase {

    private final ProfileRepository profileRepository;
    private final ClaudeAccountGateway claudeGateway;

    public ConnectClaudeUseCaseImpl(ProfileRepository profileRepository, ClaudeAccountGateway claudeGateway) {
        this.profileRepository = profileRepository;
        this.claudeGateway = claudeGateway;
    }

    @Override
    public ClaudeStatus status() {
        return claudeGateway.status();
    }

    @Override
    public Profile execute(ClaudeLoginMethod method) {
        Profile profile = profileRepository.require();
        ClaudeStatus status = claudeGateway.status();
        if (!status.installed()) {
            throw new BusinessException("Instale o Claude Code neste computador para usar o Claude.",
                    ErrorCode.CLAUDE_NOT_INSTALLED);
        }
        if (status.loggedIn()) {
            return link(profile, status.email());
        }
        claudeGateway.login(method == null ? ClaudeLoginMethod.CLAUDEAI : method,
                () -> link(profileRepository.require(), claudeGateway.status().email()));
        return profile;
    }

    @Override
    public ClaudeStatus install() {
        claudeGateway.install();
        return claudeGateway.status();
    }

    @Override
    public void submitCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Cole o código que apareceu na página do Claude.");
        }
        claudeGateway.submitCode(code.strip());
    }

    private Profile link(Profile profile, String email) {
        List<LinkedAccount> accounts = new ArrayList<>(profile.linkedAccounts().stream()
                .filter(account -> account.externalProvider() != ExternalProvider.CLAUDE)
                .toList());
        accounts.add(LinkedAccount.create(email, null, ExternalProvider.CLAUDE, null));
        return profileRepository.updateLinkedAccounts(profile.id(), accounts);
    }
}
