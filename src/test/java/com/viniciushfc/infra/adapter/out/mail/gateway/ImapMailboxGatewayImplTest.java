package com.viniciushfc.infra.adapter.out.mail.gateway;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImapMailboxGatewayImplTest {

    @Test
    void descobreHostImapPeloDominioDoEmail() {
        assertEquals("imap.gmail.com", ImapMailboxGatewayImpl.imapHost("fulano@gmail.com"));
        assertEquals("imap.gmail.com", ImapMailboxGatewayImpl.imapHost("Fulano@GMAIL.com"));
        assertEquals("outlook.office365.com", ImapMailboxGatewayImpl.imapHost("fulano@hotmail.com"));
        assertEquals("outlook.office365.com", ImapMailboxGatewayImpl.imapHost("fulano@outlook.com"));
        assertEquals("imap.mail.yahoo.com", ImapMailboxGatewayImpl.imapHost("fulano@yahoo.com.br"));
        assertEquals("imap.mail.me.com", ImapMailboxGatewayImpl.imapHost("fulano@icloud.com"));
        assertEquals("imap.empresa.com.br", ImapMailboxGatewayImpl.imapHost("fulano@empresa.com.br"));
    }
}
