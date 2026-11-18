package com.viniciushfc.infra.adapter.out.database.panache.repositories;

import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.JobMatch;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.repositories.EmailReceivedRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.vo.Attachment;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.domain.vo.Email;
import com.viniciushfc.domain.exceptions.NotFoundException;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheEmailReceivedRepository;
import com.viniciushfc.infra.adapter.out.database.panache.repositories.contracts.PanacheProfileRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PanacheRepositoriesTest {

    @Inject
    ProfileRepository profileRepository;

    @Inject
    EmailReceivedRepository emailReceivedRepository;

    @Inject
    PanacheEmailReceivedRepository panacheEmailReceivedRepository;

    @Inject
    PanacheProfileRepository panacheProfileRepository;

    @Inject
    JobMatchRepository jobMatchRepository;

    @Inject
    PendingJobRepository pendingJobRepository;

    private final Attachment cv = new Attachment("cv.pdf", "application/pdf", new byte[]{1, 2, 3});

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@gmail.com";
    }

    @BeforeEach
    void semPerfil() {
        QuarkusTransaction.requiringNew().run(() -> panacheProfileRepository.listAll().forEach(panacheProfileRepository::delete));
    }

    @Test
    void profileSalvaBuscaAtualizaERemove() {
        assertNull(profileRepository.find());
        var profile = Profile.create("Fulano", "chave", null, null, cv);
        profile.linkedAccounts().add(LinkedAccount.create(unique("linked"), "x", ExternalProvider.EMAIL, "LinkedIn"));

        profileRepository.save(profile);

        var found = profileRepository.find();
        assertEquals(profile.id(), found.id());
        assertEquals("Fulano", found.name());
        assertArrayEquals(cv.content(), found.curriculum().content());
        assertEquals(1, found.linkedAccounts().size());
        assertEquals("LinkedIn", found.linkedAccounts().getFirst().folder());
        assertEquals(ExternalProvider.EMAIL, found.linkedAccounts().getFirst().externalProvider());
        assertEquals("chave", found.geminiApiKey());
        assertEquals(profile.id(), profileRepository.require().id());

        var updated = profileRepository.update(profile.id(), new Profile(null, "Novo", null, null, null, null, null, null, null, null));
        assertEquals("Novo", updated.name());
        assertEquals("chave", updated.geminiApiKey());
        assertEquals("Novo", profileRepository.find().name());

        var conta = LinkedAccount.create("novo@linkedin", "y", ExternalProvider.LINKEDIN, null);
        var comNovaConta = profileRepository.updateLinkedAccounts(profile.id(), List.of(conta));
        assertEquals(List.of(conta), comNovaConta.linkedAccounts());
        assertEquals("Novo", comNovaConta.name(), "só as contas mudam");
        assertNull(profileRepository.updateLinkedAccounts(UUID.randomUUID(), List.of()));

        profileRepository.delete(profile.id());
        assertNull(profileRepository.find());
        assertThrows(NotFoundException.class, profileRepository::require);
    }

    @Test
    void profileUpdateDeInexistenteRetornaNulo() {
        assertNull(profileRepository.update(UUID.randomUUID(), new Profile(null, "x", null, null, null, null, null, null, null, null)));
    }

    @Test
    void emailReceivedSalvaEMarcaComoProcessado() {
        var email = EmailReceived.create(UUID.randomUUID().toString(),
                new Email("from@x.com", "to@x.com", "assunto", "corpo", List.of(cv)));

        emailReceivedRepository.save(email);
        assertFalse(isSuccessProcessed(email.id()));
        assertNull(emailReceivedRepository.findByMessageId(UUID.randomUUID().toString()));

        var found = emailReceivedRepository.findByMessageId(email.messageId());
        assertEquals(email.id(), found.id());
        assertNull(found.email(), "o conteúdo do email não é guardado");

        emailReceivedRepository.markSuccessProcessed(email.id());
        assertTrue(isSuccessProcessed(email.id()));
    }

    private boolean isSuccessProcessed(UUID id) {
        return QuarkusTransaction.requiringNew()
                .call(() -> panacheEmailReceivedRepository.findById(id).isSuccessProcessed());
    }

    @Test
    void emailReceivedGuardaVagasLidasAteSerProcessado() {
        var email = EmailReceived.create(UUID.randomUUID().toString(),
                new Email("from@x.com", "to@x.com", "assunto", "corpo", List.of()));
        var vaga1 = new JobDetail("https://www.linkedin.com/jobs/view/1/", "Dev", "desc", new CompanyDetails("ACME", "sobre"));
        var vaga2 = new JobDetail("https://www.linkedin.com/jobs/view/2/", "QA", "desc", new CompanyDetails("ACME", null));
        emailReceivedRepository.save(email);
        assertEquals(List.of(), emailReceivedRepository.findScrapedJobs(email.id()));

        QuarkusTransaction.requiringNew().run(() -> panacheEmailReceivedRepository.findById(email.id()).setScrapedJobs(
                "[{\"link\":\"https://www.linkedin.com/jobs/view/1/\",\"title\":\"Dev\",\"description\":\"desc\","
                        + "\"companyDetails\":{\"name\":\"ACME\",\"description\":\"sobre\"}},"
                        + "{\"link\":\"https://www.linkedin.com/jobs/view/2/\",\"title\":\"QA\",\"description\":\"desc\","
                        + "\"companyDetails\":{\"name\":\"ACME\",\"description\":null}}]"));
        assertEquals(List.of(vaga1, vaga2), emailReceivedRepository.findScrapedJobs(email.id()));

        emailReceivedRepository.markSuccessProcessed(email.id());
        assertEquals(List.of(), emailReceivedRepository.findScrapedJobs(email.id()), "alerta processado descarta as vagas");
    }

    private PendingJob pending(UUID profileId, String id, int minutesAgo) {
        var job = new JobDetail("https://www.linkedin.com/jobs/view/" + id + "/", "Dev " + id, "desc", new CompanyDetails("ACME", "sobre"));
        var created = PendingJob.create(profileId, UUID.randomUUID(), AlertSource.LINKEDIN, job);
        return new PendingJob(created.id(), profileId, created.emailReceivedId(), created.source(), job, false, 0,
                LocalDateTime.now().minusMinutes(minutesAgo));
    }

    @Test
    void pendingJobFilaPriorizaEDeixaDeLadoQuemFalhouDemais() {
        var userId = UUID.randomUUID();
        var antiga = pending(userId, "1", 30);
        var nova = pending(userId, "2", 10);
        var ruim = pending(userId, "3", 60);
        List.of(antiga, nova, ruim, pending(UUID.randomUUID(), "1", 5)).forEach(pendingJobRepository::save);

        assertTrue(pendingJobRepository.existsByProfileIdAndLink(userId, "https://www.linkedin.com/jobs/view/1/"));
        assertFalse(pendingJobRepository.existsByProfileIdAndLink(userId, "https://www.linkedin.com/jobs/view/9/"));
        assertEquals(List.of(ruim.id(), antiga.id(), nova.id()), ids(pendingJobRepository.nextBatch(userId, 3, 10)), "mais antigas primeiro");

        for (int i = 0; i < 3; i++) {
            pendingJobRepository.registerFailure(List.of(ruim.id()));
        }
        pendingJobRepository.prioritize(nova.id());
        assertEquals(List.of(nova.id(), antiga.id()), ids(pendingJobRepository.nextBatch(userId, 3, 10)),
                "priorizada primeiro; a que falhou 3 vezes sai da fila automática");
        var salva = pendingJobRepository.findById(ruim.id());
        assertEquals(3, salva.attempts());
        assertEquals("sobre", salva.job().companyDetails().description());
        assertEquals(3, pendingJobRepository.findByProfileId(userId).size(), "a que falhou continua visível");

        pendingJobRepository.prioritize(ruim.id());
        assertEquals(List.of(nova.id(), ruim.id(), antiga.id()), ids(pendingJobRepository.nextBatch(userId, 3, 10)),
                "priorizar dá mais uma chance, depois das priorizadas que falharam menos");

        pendingJobRepository.delete(List.of(antiga.id(), nova.id(), ruim.id()));
        assertTrue(pendingJobRepository.findByProfileId(userId).isEmpty());
    }

    private static List<UUID> ids(List<PendingJob> jobs) {
        return jobs.stream().map(PendingJob::id).toList();
    }

    @Test
    void emailReceivedMarcarInexistenteNaoFalha() {
        assertDoesNotThrow(() -> emailReceivedRepository.markSuccessProcessed(UUID.randomUUID()));
    }

    @Test
    void jobMatchSalvaBuscaPorPerfilEVerificaExistencia() {
        var userId = UUID.randomUUID();
        var outroUserId = UUID.randomUUID();
        var match1 = new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/",
                "Dev", "ACME", true, 80, List.of("a", "b"), "cv ats", LocalDateTime.now(), null);
        var match2 = new JobMatch(UUID.randomUUID(), userId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/2/",
                "QA", "ACME", false, 30, List.of("c"), null, LocalDateTime.now(), null);
        var deOutro = new JobMatch(UUID.randomUUID(), outroUserId, UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/",
                "Dev", "ACME", true, 80, List.of("a"), "cv", LocalDateTime.now(), null);

        jobMatchRepository.saveAll(List.of(match1, match2, deOutro));

        var matches = jobMatchRepository.findByProfileId(userId);
        assertEquals(2, matches.size());
        var found = matches.stream().filter(m -> m.id().equals(match1.id())).findFirst().orElseThrow();
        assertEquals(List.of("a", "b"), found.motivos());
        assertEquals("cv ats", found.curriculoAts());

        assertTrue(jobMatchRepository.existsByProfileIdAndJobLink(userId, "https://www.linkedin.com/jobs/view/1/"));
        assertFalse(jobMatchRepository.existsByProfileIdAndJobLink(userId, "https://www.linkedin.com/jobs/view/3/"));
        assertFalse(jobMatchRepository.existsByProfileIdAndJobLink(UUID.randomUUID(), "https://www.linkedin.com/jobs/view/1/"));
    }
}
