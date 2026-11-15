package com.viniciushfc.infra.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentResponse;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.gemini.mapper.GeminiCvMatchMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiCvMatchMapperTest {

    private final GeminiCvMatchMapper mapper = new GeminiCvMatchMapper(new ObjectMapper());

    private GenerateContentResponse response(String text) {
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn(text);
        return response;
    }

    private static final String JSON = """
            {"match": true, "score": 80, "motivos": ["a", "b"], "curriculoAts": "cv"}""";

    @Test
    void converteOResultado() {
        assertEquals(new CvMatchResult(true, 80, List.of("a", "b"), "cv"), mapper.toDomain(response(JSON)));
    }

    @Test
    void removeBlocoMarkdown() {
        assertEquals(80, mapper.toDomain(response("```json\n" + JSON + "\n```")).score());
    }

    @Test
    void aceitaListaComUmResultado() {
        assertEquals(80, mapper.toDomain(response("[" + JSON + "]")).score());
    }

    @Test
    void respostaVaziaLancaErro() {
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("  ")));
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response(null)));
    }

    @Test
    void jsonInvalidoLancaErro() {
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("não é json")));
    }
}
