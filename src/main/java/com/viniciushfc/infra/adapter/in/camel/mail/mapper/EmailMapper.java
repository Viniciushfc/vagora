package com.viniciushfc.infra.adapter.in.camel.mail.mapper;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.Email;
import jakarta.activation.DataHandler;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.camel.Exchange;
import org.apache.camel.attachment.AttachmentMessage;

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

        String messageId = msg.getHeader("Message-ID", String.class);
        String from = msg.getHeader("From", String.class);
        String subject = msg.getHeader("Subject", String.class);
        String body = msg.getBody(String.class);

        Email email = new Email(from, null, subject, body, extrairAnexos(exchange));
        return EmailReceived.create(messageId, email);
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