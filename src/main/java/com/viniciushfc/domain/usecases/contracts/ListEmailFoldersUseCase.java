package com.viniciushfc.domain.usecases.contracts;

import com.viniciushfc.domain.vo.MailFolder;

import java.util.List;

public interface ListEmailFoldersUseCase {
    List<MailFolder> execute();
}
