package com.viniciushfc.infra.adapter.out.mail.gateway;

import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.vo.MailFolder;
import com.viniciushfc.infra.adapter.out.mail.mapper.MailMessageMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.*;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromStringTerm;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Acessa a caixa de email por IMAP. A leitura abre as pastas em modo somente leitura,
 * então nenhum email é marcado como lido sem chamar {@link #markAsRead}.
 */
@ApplicationScoped
public class ImapMailboxGatewayImpl implements MailboxGateway {

    static final String JOB_ALERTS_SENDER = "jobalerts-noreply@linkedin.com";
    private static final int MAX_ALERTS_PER_POLL = 10;

    private final MailMessageMapper mapper;

    public ImapMailboxGatewayImpl(MailMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<MailFolder> listFolders(LinkedAccount emailAccount) {
        try (Store store = connect(emailAccount)) {
            List<MailFolder> folders = new ArrayList<>();

            for (Folder folder : store.getDefaultFolder().list("*")) {
                if ((folder.getType() & Folder.HOLDS_MESSAGES) == 0) {
                    continue;
                }
                folder.open(Folder.READ_ONLY);
                try {
                    folders.add(new MailFolder(folder.getFullName(), folder.search(new FromStringTerm(JOB_ALERTS_SENDER)).length));
                } finally {
                    folder.close(false);
                }
            }

            return folders;
        } catch (MessagingException e) {
            throw translate(e);
        }
    }

    @Override
    public List<EmailReceived> fetchUnreadJobAlerts(LinkedAccount emailAccount, LocalDate since) {
        try (Store store = connect(emailAccount)) {
            Folder folder = store.getFolder(emailAccount.folder());
            folder.open(Folder.READ_ONLY);
            try {
                Date sinceDate = Date.from(since.atStartOfDay(ZoneId.systemDefault()).toInstant());
                SearchTerm unreadAlerts = new AndTerm(new SearchTerm[]{
                        new FlagTerm(new Flags(Flags.Flag.SEEN), false),
                        new FromStringTerm(JOB_ALERTS_SENDER),
                        new ReceivedDateTerm(ComparisonTerm.GE, sinceDate)});
                Message[] messages = folder.search(unreadAlerts);

                // a pasta vem do mais antigo para o mais novo: as vagas recentes aparecem primeiro
                List<EmailReceived> alerts = new ArrayList<>();
                for (int i = messages.length - 1; i >= 0 && alerts.size() < MAX_ALERTS_PER_POLL; i--) {
                    alerts.add(mapper.toDomain(messages[i]));
                }
                return alerts;
            } finally {
                folder.close(false);
            }
        } catch (MessagingException e) {
            throw translate(e);
        } catch (IOException e) {
            Log.warn("Erro lendo um alerta do email", e);
            throw new ExternalServiceException("Não foi possível ler um alerta do email. Ele será tentado de novo.", e);
        }
    }

    @Override
    public void markAsRead(LinkedAccount emailAccount, String messageId) {
        try (Store store = connect(emailAccount)) {
            Folder folder = store.getFolder(emailAccount.folder());
            folder.open(Folder.READ_WRITE);
            try {
                for (Message message : folder.search(new HeaderTerm("Message-ID", messageId))) {
                    message.setFlag(Flags.Flag.SEEN, true);
                }
            } finally {
                folder.close(false);
            }
        } catch (MessagingException e) {
            throw translate(e);
        }
    }

    private Store connect(LinkedAccount emailAccount) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.connectiontimeout", "15000");
        props.put("mail.imaps.timeout", "30000");

        Store store = Session.getInstance(props).getStore("imaps");
        store.connect(imapHost(emailAccount.username()), 993, emailAccount.username(), emailAccount.password());
        return store;
    }

    static String imapHost(String email) {
        String domain = email.substring(email.indexOf('@') + 1).toLowerCase();
        return switch (domain) {
            case "gmail.com", "googlemail.com" -> "imap.gmail.com";
            case "outlook.com", "hotmail.com", "live.com", "msn.com" -> "outlook.office365.com";
            case "yahoo.com", "yahoo.com.br" -> "imap.mail.yahoo.com";
            case "icloud.com", "me.com", "mac.com" -> "imap.mail.me.com";
            default -> "imap." + domain;
        };
    }

    private RuntimeException translate(MessagingException e) {
        if (e instanceof AuthenticationFailedException) {
            return new ValidationException("Email ou senha inválidos. No Gmail, use uma senha de app: https://myaccount.google.com/apppasswords");
        }
        Log.warn("Falha ao acessar o email por IMAP", e);
        return new ExternalServiceException("Não foi possível acessar o email agora. O Vagora tenta de novo no próximo ciclo.", e);
    }
}
