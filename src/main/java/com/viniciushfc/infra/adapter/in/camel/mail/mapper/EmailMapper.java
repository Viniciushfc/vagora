package com.viniciushfc.infra.adapter.in.camel.mail.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.Email;
import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.component.mail.MailMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class EmailMapper {

    public EmailReceived toDomain(Exchange exchange) {
        var msg = exchange.getMessage();

        String messageId = normalizarMessageId(msg.getHeader("Message-ID", String.class));
        String to = msg.getHeader("To", String.class);
        String from = msg.getHeader("From", String.class);
        String subject = msg.getHeader("Subject", String.class);
        String body = extrairCorpo(exchange);

        Email email = new Email(from, to, subject, body, extrairAnexos(exchange));
        return EmailReceived.create(messageId, email);
    }

    private String normalizarMessageId(String messageId) {
        return messageId == null ? null : messageId.trim().replaceAll("^<|>$", "");
    }

    private String extrairCorpo(Exchange exchange) {
        MailMessage mailMessage = exchange.getMessage(MailMessage.class);
        if (mailMessage == null || mailMessage.getOriginalMessage() == null) {
            return exchange.getMessage().getBody(String.class);
        }
        try {
            Part original = mailMessage.getOriginalMessage();
            String html = buscarParte(original, "text/html");
            return html != null ? html : buscarParte(original, "text/plain");
        } catch (MessagingException | IOException e) {
            throw new IllegalStateException("Erro lendo corpo do e-mail", e);
        }
    }

    private String buscarParte(Part part, String mimeType) throws MessagingException, IOException {
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
            return null;
        }
        if (part.isMimeType(mimeType)) {
            return (String) part.getContent();
        }
        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                String conteudo = buscarParte(multipart.getBodyPart(i), mimeType);
                if (conteudo != null) {
                    return conteudo;
                }
            }
        }
        if (part.isMimeType("message/rfc822")) {
            return buscarParte((Part) part.getContent(), mimeType);
        }
        return null;
    }

    private List<Attachment> extrairAnexos(Exchange exchange) {
        List<Attachment> anexos = new ArrayList<>();
        AttachmentMessage attMsg = exchange.getMessage(AttachmentMessage.class);

        if (attMsg == null || !attMsg.hasAttachments()) {
            return anexos;
        }

        for (Map.Entry<String, DataHandler> entry : attMsg.getAttachments().entrySet()) {
            DataHandler dh = entry.getValue();
            try (InputStream is = dh.getInputStream()) {
                anexos.add(new Attachment(dh.getName(), dh.getContentType(), is.readAllBytes()));
            } catch (IOException e) {
                throw new UncheckedIOException("Erro lendo anexo " + entry.getKey(), e);
            }
        }
        return anexos;
    }
}