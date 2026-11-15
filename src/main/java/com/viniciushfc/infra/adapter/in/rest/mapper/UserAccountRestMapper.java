package com.viniciushfc.infra.adapter.in.rest.mapper;

import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.UserAccount;
import com.viniciushfc.domain.utils.Utils;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.infra.adapter.in.rest.dto.LinkedAccountRequest;
import com.viniciushfc.infra.adapter.in.rest.dto.LinkedAccountResponse;
import com.viniciushfc.infra.adapter.in.rest.dto.UserAccountForm;
import com.viniciushfc.infra.adapter.in.rest.dto.UserAccountResponse;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;

public class UserAccountRestMapper {

    public static UserAccount toDomain(UserAccountForm form) {
        return new UserAccount(null, form.name, form.username, form.password, toAttachment(form.curriculum), null);
    }

    public static LinkedAccount toDomain(LinkedAccountRequest request) {
        return new LinkedAccount(null, request.username(), request.password(), request.externalProvider());
    }

    public static UserAccountResponse toResponse(UserAccount userAccount) {
        return new UserAccountResponse(
                userAccount.id(),
                userAccount.name(),
                userAccount.username(),
                Utils.isNull(userAccount.curriculum()) ? null : userAccount.curriculum().name(),
                userAccount.linkedAccounts().stream()
                        .map(l -> new LinkedAccountResponse(l.id(), l.username(), l.externalProvider()))
                        .toList()
        );
    }

    private static Attachment toAttachment(FileUpload file) {
        if (Utils.isNull(file)) {
            return null;
        }

        try {
            return new Attachment(file.fileName(), file.contentType(), Files.readAllBytes(file.uploadedFile()));
        } catch (IOException e) {
            throw new UncheckedIOException("Erro lendo currículo", e);
        }
    }
}
