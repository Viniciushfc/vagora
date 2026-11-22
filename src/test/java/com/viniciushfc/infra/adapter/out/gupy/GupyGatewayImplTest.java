package com.viniciushfc.infra.adapter.out.gupy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;
import com.viniciushfc.domain.vo.GupyJob;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GupyGatewayImplTest {

    @Test
    void montaABuscaComOsFiltrosDaGupy() {
        GupySearch search = new GupySearch(null, null, "Desenvolvedor Java", "Paraná", "Cascavel",
                Set.of(WorkplaceType.ON_SITE, WorkplaceType.REMOTE), Set.of(ContractType.CLT), 4, true, null);

        assertEquals(GupyGatewayImpl.ENDPOINT + "?jobName=Desenvolvedor+Java&state=Paran%C3%A1&city=Cascavel"
                + "&workplaceType=on-site,remote&limit=100&offset=200", GupyGatewayImpl.url(search, 200));
    }

    @Test
    void leAVagaComTipoDeContratacaoLocalEPrazo() throws Exception {
        var data = new ObjectMapper().readTree("""
                [{"name": "Dev Java", "description": "Spring e AWS", "careerPageName": "Acme", "type": "vacancy_legal_entity",
                  "publishedDate": "2026-11-20T18:02:17.240Z", "applicationDeadline": "2027-01-03T12:00:00.000Z",
                  "workplaceType": "hybrid", "city": "Cascavel", "state": "Paraná", "jobUrl": "https://acme.gupy.io/job/1"}]""");

        GupyJob job = GupyGatewayImpl.toJobs(data).getFirst();

        assertEquals(ContractType.PJ, job.contract());
        assertEquals(LocalDate.of(2027, 1, 3), job.deadline());
        assertEquals("https://acme.gupy.io/job/1", job.job().link());
        assertEquals("Acme", job.job().companyDetails().name());
        assertEquals("Modalidade: Híbrido. Local: Cascavel - Paraná. Contratação: PJ.\n\nSpring e AWS", job.job().description());
        assertEquals(List.of(), GupyGatewayImpl.toJobs(new ObjectMapper().readTree("[{\"name\": \"Sem link\"}]")));
    }
}
