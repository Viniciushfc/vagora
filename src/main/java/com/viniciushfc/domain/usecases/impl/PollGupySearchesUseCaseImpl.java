package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.GupySearch;
import com.viniciushfc.domain.entities.PendingJob;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.gateways.GupyGateway;
import com.viniciushfc.domain.repositories.GupySearchRepository;
import com.viniciushfc.domain.repositories.JobMatchRepository;
import com.viniciushfc.domain.repositories.PendingJobRepository;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.repositories.ScraperActivityRepository;
import com.viniciushfc.domain.usecases.contracts.PollGupySearchesUseCase;
import com.viniciushfc.domain.vo.GupyJob;
import com.viniciushfc.domain.vo.ProcessingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@UseCase
public class PollGupySearchesUseCaseImpl implements PollGupySearchesUseCase {

    static final Period MAX_JOB_AGE = Period.ofDays(7);
    static final int MAX_NEW_JOBS_PER_SEARCH = 30;

    private final ProfileRepository profileRepository;
    private final GupySearchRepository gupySearchRepository;
    private final GupyGateway gupyGateway;
    private final JobMatchRepository jobMatchRepository;
    private final PendingJobRepository pendingJobRepository;
    private final ProcessingStatusRepository statusRepository;
    private final ScraperActivityRepository activityRepository;

    public PollGupySearchesUseCaseImpl(ProfileRepository profileRepository, GupySearchRepository gupySearchRepository,
                                       GupyGateway gupyGateway, JobMatchRepository jobMatchRepository,
                                       PendingJobRepository pendingJobRepository, ProcessingStatusRepository statusRepository,
                                       ScraperActivityRepository activityRepository) {
        this.profileRepository = profileRepository;
        this.gupySearchRepository = gupySearchRepository;
        this.gupyGateway = gupyGateway;
        this.jobMatchRepository = jobMatchRepository;
        this.pendingJobRepository = pendingJobRepository;
        this.statusRepository = statusRepository;
        this.activityRepository = activityRepository;
    }

    @Override
    public void execute() {
        Profile profile = profileRepository.find();
        if (profile == null || !profile.isMonitoringEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<GupySearch> due = gupySearchRepository.findByProfileId(profile.id()).stream()
                .filter(search -> search.isDue(now))
                .toList();
        if (due.isEmpty()) {
            return;
        }

        try {
            for (GupySearch search : due) {
                enqueueNewJobs(profile, search, now.toLocalDate());
                GupySearch current = gupySearchRepository.findById(search.id());
                if (current != null) {
                    gupySearchRepository.save(current.ranAt(now));
                }
            }
            save(profile, statusRepository.find(profile.id(), AlertSource.GUPY).succeeded().checked());
        } catch (RuntimeException e) {
            save(profile, statusRepository.find(profile.id(), AlertSource.GUPY).failed(e).checked());
            activityRepository.update(AlertSource.GUPY, activity -> activity.failed(e.getMessage(), false));
            throw e;
        }
    }

    private void enqueueNewJobs(Profile profile, GupySearch search, LocalDate today) {
        LocalDate since = today.minus(MAX_JOB_AGE);
        List<GupyJob> results = gupyGateway.search(search, since);
        int wanted = 0;
        int added = 0;
        for (GupyJob found : results) {
            if (!wanted(search, found, since, today)) {
                continue;
            }
            wanted++;
            if (added < MAX_NEW_JOBS_PER_SEARCH && !known(profile, found.job().link())) {
                pendingJobRepository.save(PendingJob.create(profile.id(), null, AlertSource.GUPY, found.job()));
                added++;
            }
        }
        String message = search.jobName() + ": " + plural(wanted, "vaga nos filtros", "vagas nos filtros")
                + ", " + (added == 0 ? "nenhuma nova" : plural(added, "nova na fila", "novas na fila"));
        activityRepository.update(AlertSource.GUPY, activity -> activity.event(message, false));
    }

    private static String plural(int count, String one, String many) {
        return count + " " + (count == 1 ? one : many);
    }

    private static boolean wanted(GupySearch search, GupyJob found, LocalDate since, LocalDate today) {
        return search.contracts().contains(found.contract())
                && search.matchesLevel(found.job().title())
                && !found.publishedAt().toLocalDate().isBefore(since)
                && (found.deadline() == null || !found.deadline().isBefore(today));
    }

    private boolean known(Profile profile, String link) {
        return jobMatchRepository.existsByProfileIdAndJobLink(profile.id(), link)
                || pendingJobRepository.existsByProfileIdAndLink(profile.id(), link);
    }

    private void save(Profile profile, ProcessingStatus status) {
        statusRepository.save(profile.id(), AlertSource.GUPY, status);
    }
}
