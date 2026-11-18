package com.viniciushfc.infra.adapter.out.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.infra.adapter.out.llm.mapper.CvMatchMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CvMatchMapperTest {

    private final CvMatchMapper mapper = new CvMatchMapper(new ObjectMapper());

    private List<CvMatchResult> map(String text, int expected) {
        return mapper.toDomain("Gemini", text, expected);
    }

    @Test
    void ordenaPeloIdDaVaga() {
        String json = """
                {"resultados": [{"id": 2, "match": false, "score": 30, "motivos": ["b"]},
                 {"id": 1, "match": true, "score": 80, "motivos": ["a"]}]}""";
        assertEquals(List.of(new CvMatchResult(true, 80, List.of("a"), null), new CvMatchResult(false, 30, List.of("b"), null)),
                map("```json\n" + json + "\n```", 2));
    }

    @Test
    void recusaIdFaltandoRepetidoOuForaDoAlerta() {
        assertThrows(ExternalServiceException.class, () -> map("[{\"id\": 1, \"score\": 1}]", 2));
        assertThrows(ExternalServiceException.class, () -> map("[{\"id\": 1, \"score\": 1}, {\"id\": 1, \"score\": 2}]", 2));
        assertThrows(ExternalServiceException.class, () -> map("[{\"id\": 3, \"score\": 1}]", 1));
    }

    @Test
    void respostaVaziaOuInvalidaLancaErro() {
        assertThrows(ExternalServiceException.class, () -> map("  ", 1));
        assertThrows(ExternalServiceException.class, () -> map(null, 1));
        assertThrows(ExternalServiceException.class, () -> map("não é json", 1));
    }
}
