package com.viniciushfc.domain.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlainTextTest {

    @Test
    void tiraMarcacaoDeMarkdownESemMexerNoResto() {
        String ia = """
                ## RESUMO PROFISSIONAL
                Dev **Java 21** com *Quarkus* e `Kafka`.
                * Liderou time de 5 pessoas
                - Usa C# e calcula 2 * 3 * 4
                ```
                fim
                ```""";

        assertEquals("""
                RESUMO PROFISSIONAL
                Dev Java 21 com Quarkus e Kafka.
                - Liderou time de 5 pessoas
                - Usa C# e calcula 2 * 3 * 4
                fim
                """, PlainText.clean(ia));
    }

    @Test
    void negritoSemFechamentoSomeENuloContinuaNulo() {
        assertEquals("Nota baixa", PlainText.clean("**Nota baixa"));
        assertEquals(List.of("ok", "falta Go"), PlainText.cleanAll(List.of("**ok**", "falta Go")));
        assertNull(PlainText.clean(null));
    }

    @Test
    void hifenEEspacoEspeciaisViramComuns() {
        assertEquals("multi-tenant orchestrator-workers 10 000", PlainText.clean("multi\u2011tenant orchestrator\u2010workers 10\u202F000"));
    }

    @Test
    void numerosQueFaltamIgnoramZeroAEsquerdaEPontoFinal() {
        String original = "Reverti 3 correções. Reduzi 40% do tempo em 07/2025. Atendi 1.200 clientes.";
        String gerado = "Reduzi 40% do tempo em 7/2025 e atendi 1.200 clientes";

        assertEquals(List.of("3"), PlainText.missingNumbers(original, gerado));
        assertEquals(List.of(), PlainText.missingNumbers(original, gerado + ", 3 correções"));
    }

    @Test
    void numerosDeDadosPessoaisNaoContamComoFaltando() {
        String original = """
                João Silva | joao@email.com | (11) 98765-4321
                CPF: 123.456.789-00
                Data de nascimento: 15/03/1995
                Endereço: Rua das Flores, 120
                RG 12345678
                Reduzi 40% do tempo em 2019-2021.
                """;

        assertEquals(List.of("40"), PlainText.missingNumbers(original, "Reduzi o tempo de 2019 a 2021."));
    }
}
