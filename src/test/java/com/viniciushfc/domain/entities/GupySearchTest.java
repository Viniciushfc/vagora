package com.viniciushfc.domain.entities;

import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GupySearchTest {

    private static GupySearch search(String jobName) {
        return new GupySearch(null, null, jobName, null, null, Set.of(WorkplaceType.REMOTE), Set.of(ContractType.CLT), 4, true, null);
    }

    @Test
    void tiraONivelDaBuscaEFiltraPeloTitulo() {
        GupySearch search = search("Desenvolvedor Backend Jr");

        assertEquals("Desenvolvedor Backend", search.query());
        assertTrue(search.matchesLevel("Desenvolvedor(a) Backend Júnior"));
        assertTrue(search.matchesLevel("Dev Backend JR."));
        assertFalse(search.matchesLevel("Desenvolvedor Backend Sênior"));
        assertFalse(search.matchesLevel("Desenvolvedor Backend"));
    }

    @Test
    void semNivelNaoFiltraPeloTitulo() {
        GupySearch search = search("Desenvolvedor Java");

        assertEquals("Desenvolvedor Java", search.query());
        assertTrue(search.matchesLevel("Desenvolvedor Java Sênior"));
        assertEquals("Junior", search("Junior").query());
        assertEquals("Desenvolvedor PL/SQL", search("Desenvolvedor PL/SQL").query());
        assertTrue(search("Desenvolvedor PL/SQL").matchesLevel("Desenvolvedor PL/SQL Sênior"));
    }
}
