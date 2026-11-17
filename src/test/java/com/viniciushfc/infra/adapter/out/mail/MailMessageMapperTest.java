package com.viniciushfc.infra.adapter.out.mail;

import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.infra.adapter.out.mail.mapper.MailMessageMapper;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailMessageMapperTest {

    private final MailMessageMapper mapper = new MailMessageMapper();

    private MimeMessage message() throws Exception {
        var message = new MimeMessage(Session.getInstance(new Properties())) {
            @Override
            protected void updateMessageID() throws jakarta.mail.MessagingException {
                setHeader("Message-ID", "<abc@linkedin.com>");
            }
        };
        message.setFrom(new InternetAddress("Alertas de vaga do LinkedIn <jobalerts-noreply@linkedin.com>"));
        message.setRecipients(Message.RecipientType.TO, "Fulano de Tal <fulano@gmail.com>");
        message.setSubject("Novas vagas: Desenvolvedor Java", "UTF-8");
        return message;
    }

    @Test
    void mapeiaCabecalhosEPrefereCorpoHtml() throws Exception {
        var message = message();
        var alternative = new MimeMultipart("alternative");
        var text = new MimeBodyPart();
        text.setText("versão texto", "UTF-8");
        var html = new MimeBodyPart();
        html.setContent("<a href=\"x\">versão html</a>", "text/html; charset=UTF-8");
        alternative.addBodyPart(text);
        alternative.addBodyPart(html);
        message.setContent(alternative);
        message.saveChanges();

        var received = mapper.toDomain(message);

        assertEquals("abc@linkedin.com", received.messageId());
        assertEquals("jobalerts-noreply@linkedin.com", received.email().from());
        assertEquals("fulano@gmail.com", received.email().to());
        assertEquals("Novas vagas: Desenvolvedor Java", received.email().subject());
        assertEquals("<a href=\"x\">versão html</a>", received.email().body());
        assertTrue(received.isFrom(AlertSource.LINKEDIN));
        assertTrue(received.email().attachments().isEmpty());
    }

    @Test
    void usaTextoQuandoNaoHaHtml() throws Exception {
        var message = message();
        message.setText("só texto", "UTF-8");
        message.saveChanges();

        assertEquals("só texto", mapper.toDomain(message).email().body());
    }

    @Test
    void extraiAnexos() throws Exception {
        var message = message();
        var mixed = new MimeMultipart("mixed");
        var body = new MimeBodyPart();
        body.setText("corpo", "UTF-8");
        var attachment = new MimeBodyPart();
        attachment.setDataHandler(new DataHandler(new ByteArrayDataSource(new byte[]{1, 2, 3}, "application/pdf")));
        attachment.setFileName("vaga.pdf");
        attachment.setDisposition(MimeBodyPart.ATTACHMENT);
        mixed.addBodyPart(body);
        mixed.addBodyPart(attachment);
        message.setContent(mixed);
        message.saveChanges();

        var received = mapper.toDomain(message);

        assertEquals("corpo", received.email().body());
        assertEquals(1, received.email().attachments().size());
        var anexo = received.email().attachments().getFirst();
        assertEquals("vaga.pdf", anexo.name());
        assertEquals("application/pdf", anexo.contentType());
        assertArrayEquals(new byte[]{1, 2, 3}, anexo.content());
    }

    @Test
    void normalizaMessageId() {
        assertEquals("abc@mail", mapper.normalizeMessageId("  <abc@mail> "));
        assertEquals("abc@mail", mapper.normalizeMessageId("abc@mail"));
        assertNull(mapper.normalizeMessageId(null));
    }
}
