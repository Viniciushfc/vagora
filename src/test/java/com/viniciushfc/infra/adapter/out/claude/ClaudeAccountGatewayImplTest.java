package com.viniciushfc.infra.adapter.out.claude;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClaudeAccountGatewayImplTest {

    @Test
    void erroDoLoginSemOPromptDoCodigo() {
        assertEquals("Couldn't log in: That code wasn't accepted.",
                ClaudeAccountGatewayImpl.loginFailure("Paste code here if prompted > Couldn't log in: That code wasn't accepted."));
    }
}
