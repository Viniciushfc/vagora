package com.viniciushfc.infra.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.types.GenerateContentResponse;
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
            [{"match": true, "score": 80, "motivos": ["a", "b"], "curriculoAts": "cv"},
             {"match": false, "score": 20, "motivos": ["c"], "curriculoAts": null}]""";

    @Test
    void converteListaDeResultados() {
        List<CvMatchResult> results = mapper.toDomain(response(JSON));

        assertEquals(2, results.size());
        assertEquals(new CvMatchResult(true, 80, List.of("a", "b"), "cv"), results.get(0));
        assertFalse(results.get(1).match());
        assertNull(results.get(1).curriculoAts());
    }

    @Test
    void removeBlocoMarkdown() {
        assertEquals(2, mapper.toDomain(response("```json\n" + JSON + "\n```")).size());
    }

    @Test
    void respostaVaziaLancaErro() {
        assertThrows(IllegalStateException.class, () -> mapper.toDomain(response("  ")));
        assertThrows(IllegalStateException.class, () -> mapper.toDomain(response(null)));
    }

    @Test
    void jsonInvalidoLancaErro() {
        assertThrows(IllegalStateException.class, () -> mapper.toDomain(response("não é json")));
    }
}
