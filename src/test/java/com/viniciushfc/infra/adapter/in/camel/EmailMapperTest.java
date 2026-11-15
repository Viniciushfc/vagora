package com.viniciushfc.infra.adapter.in.camel;

import com.viniciushfc.infra.adapter.in.camel.mail.mapper.EmailMapper;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailMapperTest {

    private final EmailMapper mapper = new EmailMapper();

    private Exchange exchange(String messageId) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.getMessage().setHeader("Message-ID", messageId);
        exchange.getMessage().setHeader("From", "jobalerts-noreply@linkedin.com");
        exchange.getMessage().setHeader("To", "fulano@gmail.com");
        exchange.getMessage().setHeader("Subject", "Novas vagas");
        exchange.getMessage().setBody("corpo do email");
        return exchange;
    }

    @Test
    void mapeiaCabecalhosECorpo() {
        var received = mapper.toDomain(exchange("<abc@mail>"));

        assertNotNull(received.id());
        assertFalse(received.successProcessed());
        assertEquals("jobalerts-noreply@linkedin.com", received.email().from());
        assertEquals("fulano@gmail.com", received.email().to());
        assertEquals("Novas vagas", received.email().subject());
        assertEquals("corpo do email", received.email().body());
        assertTrue(received.email().attachments().isEmpty());
    }

    @Test
    void normalizaMessageId() {
        assertEquals("abc@mail", mapper.toDomain(exchange("  <abc@mail> ")).messageId());
        assertEquals("abc@mail", mapper.toDomain(exchange("abc@mail")).messageId());
        assertNull(mapper.toDomain(exchange(null)).messageId());
    }
}
