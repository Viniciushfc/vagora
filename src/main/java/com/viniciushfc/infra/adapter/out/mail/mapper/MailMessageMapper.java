package com.viniciushfc.infra.adapter.out.mail.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.Email;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class MailMessageMapper {

    public EmailReceived toDomain(Message message) throws MessagingException, IOException {
        String messageId = message instanceof MimeMessage mime ? normalizeMessageId(mime.getMessageID()) : null;

        Email email = new Email(
                firstAddress(message.getFrom()),
                firstAddress(message.getRecipients(Message.RecipientType.TO)),
                message.getSubject(),
                body(message),
                attachments(message)
        );

        return EmailReceived.create(messageId, email);
    }

    public String normalizeMessageId(String messageId) {
        return messageId == null ? null : messageId.trim().replaceAll("^<|>$", "");
    }

    private String firstAddress(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return null;
        }
        return addresses[0] instanceof InternetAddress internet ? internet.getAddress() : addresses[0].toString();
    }

    private String body(Part message) throws MessagingException, IOException {
        String html = findPart(message, "text/html");
        return html != null ? html : findPart(message, "text/plain");
    }

    private String findPart(Part part, String mimeType) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            return null;
        }
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String content = findPart(multipart.getBodyPart(i), mimeType);
                if (content != null) {
                    return content;
                }
            }
        }
        if (part.isMimeType("message/rfc822")) {
            return findPart((Part) part.getContent(), mimeType);
        }
        return null;
    }

    private List<Attachment> attachments(Part part) throws MessagingException, IOException {
        List<Attachment> attachments = new ArrayList<>();

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                attachments.addAll(attachments(multipart.getBodyPart(i)));
            }
        } else if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            try (InputStream is = part.getInputStream()) {
                String contentType = part.getContentType().split(";")[0].trim();
                attachments.add(new Attachment(part.getFileName(), contentType, is.readAllBytes()));
            }
        }

        return attachments;
    }
}
