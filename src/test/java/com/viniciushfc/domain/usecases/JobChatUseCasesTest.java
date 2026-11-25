package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.exceptions.ExternalServiceException;
import com.viniciushfc.domain.exceptions.ValidationException;
import com.viniciushfc.domain.gateways.AnalyzeJobsMatchCvGateway;
import com.viniciushfc.domain.gateways.PdfGateway;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.impl.AnalyzeExternalJobUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.ChatJobMatchUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.GenerateCurriculumUseCaseImpl;
import com.viniciushfc.domain.usecases.impl.ReanalyzeJobMatchUseCaseImpl;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.ChatMessage;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.CvMatchResult;
import com.viniciushfc.domain.vo.JobDetail;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JobChatUseCasesTest {

    private final ProfileRepository profileRepository = mock(ProfileRepository.class);
    private final JobMatchRepository jobMatchRepository = mock(JobMatchRepository.class);
    private final AnalyzeJobsMatchCvGateway gateway = mock(AnalyzeJobsMatchCvGateway.class);
    private final PdfGateway pdfGateway = mock(PdfGateway.class);
    private final Profile profile = new Profile(UUID.randomUUID(), "F", "k", new Attachment("cv.pdf", "application/pdf", new byte[]{1}),
            new ArrayList<>(), null, null, null, null, null, null, null, null, null, null, null, null, null);

    JobChatUseCasesTest() {
        when(profileRepository.require()).thenReturn(profile);
        when(pdfGateway.extractTextPdf(any())).thenReturn("cv");
    }

    private final AnalyzeExternalJobUseCaseImpl external = new AnalyzeExternalJobUseCaseImpl(profileRepository, jobMatchRepository, gateway, pdfGateway);
    private final ChatJobMatchUseCaseImpl chat = new ChatJobMatchUseCaseImpl(profileRepository, jobMatchRepository, gateway, pdfGateway);

    private static JobDetail pasted(String link, String company) {
        return new JobDetail(link, " Dev Java ", "descrição", new CompanyDetails(company, null));
    }

    @Test
    void vagaColadaGanhaNotaECurriculoMesmoAbaixoDoCorte() {
        when(gateway.analyze(anyList(), eq("cv"), eq(profile))).thenReturn(List.of(new CvMatchResult(false, 40, List.of("falta Go"), null)));
        when(gateway.adaptCurriculum(any(), eq("cv"), eq(profile))).thenReturn("cv adaptado");

        JobMatch created = external.execute(pasted(" ", "  "));

        assertEquals("Dev Java", created.jobTitle());
        assertNull(created.jobLink());
        assertNull(created.companyName());
        assertNull(created.emailReceivedId());
        assertEquals(40, created.score());
        assertEquals("cv adaptado", created.curriculoAts());
        verify(jobMatchRepository).saveAll(anyList());
        verify(jobMatchRepository).updateCurriculum(created.id(), "cv adaptado");
    }

    @Test
    void falhaNoCurriculoNaoPerdeANota() {
        when(gateway.analyze(anyList(), eq("cv"), eq(profile))).thenReturn(List.of(new CvMatchResult(true, 85, List.of("ok"), null)));
        when(gateway.adaptCurriculum(any(), eq("cv"), eq(profile))).thenThrow(new ExternalServiceException("fora do ar"));

        JobMatch created = external.execute(pasted("https://vaga", "ACME"));

        assertEquals(85, created.score());
        assertNull(created.curriculoAts());
        verify(jobMatchRepository).saveAll(List.of(created));
    }

    @Test
    void vagaSemDescricaoNemChegaNaIa() {
        assertThrows(ValidationException.class, () -> external.execute(new JobDetail(null, "Dev", " ", new CompanyDetails(null, null))));
        verify(gateway, never()).analyze(anyList(), any(), any());
    }

    @Test
    void chatExigeQueAUltimaMensagemSejaDoCandidato() {
        var id = UUID.randomUUID();
        assertThrows(ValidationException.class, () -> chat.execute(id, List.of()));
        assertThrows(ValidationException.class, () -> chat.execute(id, List.of(new ChatMessage("assistant", "oi"))));
        assertThrows(ValidationException.class, () -> chat.execute(id, List.of(new ChatMessage("user", "  "))));
        verify(gateway, never()).chat(any(), any(), anyList(), any());
    }

    @Test
    void chatMandaAVagaEAConversaParaAIa() {
        var jobMatch = JobMatch.create(profile.id(), null, pasted(null, "ACME"), new CvMatchResult(true, 80, List.of("ok"), null));
        when(jobMatchRepository.findById(jobMatch.id())).thenReturn(jobMatch);
        var messages = List.of(new ChatMessage("user", "qual o ponto fraco?"));
        when(gateway.chat(jobMatch, "cv", messages, profile)).thenReturn("resposta");

        assertEquals("resposta", chat.execute(jobMatch.id(), messages));
    }

    @Test
    void pdfDoChatUsaOTextoDaConversaSemMexerNaVaga() {
        var jobMatch = JobMatch.create(profile.id(), null, pasted(null, "ACME"), new CvMatchResult(true, 80, List.of("ok"), "cv salvo"));
        when(jobMatchRepository.findById(jobMatch.id())).thenReturn(jobMatch);
        when(pdfGateway.createPdf("cv do chat")).thenReturn(new byte[]{9});
        var curricula = new GenerateCurriculumUseCaseImpl(profileRepository, jobMatchRepository, gateway, pdfGateway);

        Attachment pdf = curricula.pdf(jobMatch.id(), "  cv do chat ");

        assertEquals("Currículo - " + jobMatch.jobTitle() + ".pdf", pdf.name());
        verify(pdfGateway).createPdf("cv do chat");
        verify(jobMatchRepository, never()).updateCurriculum(any(), any());
        assertThrows(ValidationException.class, () -> curricula.pdf(jobMatch.id(), " "));
    }

    @Test
    void refazerAnaliseUsaOPdfAtualETrocaNotaECurriculo() {
        var jobMatch = JobMatch.create(profile.id(), null, pasted(null, "ACME"), new CvMatchResult(false, 40, List.of("falta Go"), "cv antigo"));
        when(jobMatchRepository.findById(jobMatch.id())).thenReturn(jobMatch);
        var nova = new CvMatchResult(true, 82, List.of("tem Go agora"), null);
        when(gateway.analyze(anyList(), eq("cv"), eq(profile))).thenReturn(List.of(nova));
        when(gateway.adaptCurriculum(any(), eq("cv"), eq(profile))).thenReturn("cv novo");

        JobMatch refeita = new ReanalyzeJobMatchUseCaseImpl(profileRepository, jobMatchRepository, gateway, pdfGateway).execute(jobMatch.id());

        assertEquals(82, refeita.score());
        assertEquals(List.of("tem Go agora"), refeita.motivos());
        assertEquals("cv novo", refeita.curriculoAts());
        verify(jobMatchRepository).updateAnalysis(jobMatch.id(), nova);
        verify(jobMatchRepository).updateCurriculum(jobMatch.id(), "cv novo");
    }
}
