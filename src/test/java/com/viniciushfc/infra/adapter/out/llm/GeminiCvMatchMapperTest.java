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

    @Test
    void ordenaPeloIdDaVaga() {
        String json = """
                {"resultados": [{"id": 2, "match": false, "score": 30, "motivos": ["b"]},
                 {"id": 1, "match": true, "score": 80, "motivos": ["a"]}]}""";
        assertEquals(List.of(new CvMatchResult(true, 80, List.of("a"), null), new CvMatchResult(false, 30, List.of("b"), null)),
                mapper.toDomain(response("```json\n" + json + "\n```"), 2));
    }

    @Test
    void recusaIdFaltandoRepetidoOuForaDoAlerta() {
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("[{\"id\": 1, \"score\": 1}]"), 2));
        assertThrows(ExternalServiceException.class,
                () -> mapper.toDomain(response("[{\"id\": 1, \"score\": 1}, {\"id\": 1, \"score\": 2}]"), 2));
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("[{\"id\": 3, \"score\": 1}]"), 1));
    }

    @Test
    void respostaVaziaOuInvalidaLancaErro() {
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("  "), 1));
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response(null), 1));
        assertThrows(ExternalServiceException.class, () -> mapper.toDomain(response("não é json"), 1));
    }
}
