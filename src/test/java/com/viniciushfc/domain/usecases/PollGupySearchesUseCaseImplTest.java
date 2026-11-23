package com.viniciushfc.domain.usecases;

import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ContractType;
import com.viniciushfc.domain.enums.WorkplaceType;
import com.viniciushfc.domain.gateways.GupyGateway;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.impl.PollGupySearchesUseCaseImpl;
import com.viniciushfc.domain.vo.CompanyDetails;
import com.viniciushfc.domain.vo.GupyJob;
import com.viniciushfc.domain.vo.JobDetail;
import com.viniciushfc.infra.adapter.out.memory.InMemoryProcessingStatusRepository;
import com.viniciushfc.infra.adapter.out.memory.InMemoryScraperActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PollGupySearchesUseCaseImplTest {

    @Mock
    ProfileRepository profileRepository;
    @Mock
    GupySearchRepository gupySearchRepository;
    @Mock
    GupyGateway gupyGateway;
    @Mock
    JobMatchRepository jobMatchRepository;
    @Mock
    PendingJobRepository pendingJobRepository;

    private InMemoryProcessingStatusRepository statusRepository;
    private InMemoryScraperActivityRepository activityRepository;
    private PollGupySearchesUseCaseImpl useCase;
    private Profile profile;

    @BeforeEach
    void setUp() {
        statusRepository = new InMemoryProcessingStatusRepository();
        activityRepository = new InMemoryScraperActivityRepository();
        useCase = new PollGupySearchesUseCaseImpl(profileRepository, gupySearchRepository, gupyGateway, jobMatchRepository,
                pendingJobRepository, statusRepository, activityRepository);
        profile = new Profile(UUID.randomUUID(), "F", "k", null, new ArrayList<>(), null, null, null, null, null, null, null, null, null, null, null, null);
        when(profileRepository.find()).thenReturn(profile);
    }

    private GupySearch search(LocalDateTime lastRunAt) {
        return new GupySearch(UUID.randomUUID(), profile.id(), "Desenvolvedor Java", null, null,
                Set.of(WorkplaceType.REMOTE), Set.of(ContractType.CLT, ContractType.PJ), 4, true, lastRunAt);
    }

    private static GupyJob job(String link, ContractType contract, int daysAgo, LocalDate deadline) {
        return new GupyJob(new JobDetail(link, "Dev", "descrição", new CompanyDetails("Empresa", null)), contract,
                LocalDateTime.now().minusDays(daysAgo), deadline);
    }

    @Test
    void enfileiraSoVagasNovasDoTipoEscolhidoDentroDoPrazo() {
        GupySearch search = search(null);
        when(gupySearchRepository.findByProfileId(profile.id())).thenReturn(List.of(search));
        when(gupySearchRepository.findById(search.id())).thenReturn(search);
        when(gupyGateway.search(eq(search), any())).thenReturn(List.of(
                job("nova", ContractType.CLT, 1, LocalDate.now().plusDays(10)),
                job("estagio", ContractType.INTERNSHIP, 1, null),
                job("antiga", ContractType.PJ, 8, null),
                job("encerrada", ContractType.PJ, 1, LocalDate.now().minusDays(1)),
                job("ja-vista", ContractType.PJ, 1, null)));
        lenient().when(jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), "ja-vista")).thenReturn(true);

        useCase.execute();

        ArgumentCaptor<PendingJob> saved = ArgumentCaptor.forClass(PendingJob.class);
        verify(pendingJobRepository).save(saved.capture());
        assertEquals("nova", saved.getValue().link());
        assertEquals(AlertSource.GUPY, saved.getValue().source());
        verify(gupySearchRepository).save(argThat(s -> s.lastRunAt() != null));
        assertNotNull(statusRepository.find(profile.id(), AlertSource.GUPY).lastCheckAt());
        assertEquals("Desenvolvedor Java: 2 vagas nos filtros, 1 nova na fila",
                activityRepository.get(AlertSource.GUPY).events().getFirst().message());
    }

    @Test
    void buscaExcluidaDuranteABuscaNaoVolta() {
        GupySearch search = search(null);
        when(gupySearchRepository.findByProfileId(profile.id())).thenReturn(List.of(search));
        when(gupySearchRepository.findById(search.id())).thenReturn(null);

        useCase.execute();

        verify(gupySearchRepository, never()).save(any());
    }

    @Test
    void buscaSoDepoisDoIntervaloEscolhido() {
        when(gupySearchRepository.findByProfileId(profile.id())).thenReturn(List.of(search(LocalDateTime.now().minusHours(3))));

        useCase.execute();

        verifyNoInteractions(gupyGateway);
    }
}
