package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.vo.MailFolder;

import java.time.LocalDate;
import java.util.List;

public interface MailboxGateway {

    List<MailFolder> listFolders(LinkedAccount emailAccount);

    boolean folderExists(LinkedAccount emailAccount, String folder);

    List<EmailReceived> fetchUnreadJobAlerts(LinkedAccount emailAccount, AlertSource source, LocalDate since);

    void markAsRead(LinkedAccount emailAccount, AlertSource source, String messageId);
}
