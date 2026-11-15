package com.viniciushfc.domain.gateways;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.vo.MailFolder;

import java.util.List;

public interface MailboxGateway {

    List<MailFolder> listFolders(LinkedAccount emailAccount);

    List<EmailReceived> fetchUnreadJobAlerts(LinkedAccount emailAccount);

    void markAsRead(LinkedAccount emailAccount, String messageId);
}
