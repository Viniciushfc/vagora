package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.AlertSource;
import com.viniciushfc.domain.enums.ExternalProvider;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.PollJobAlertsUseCase;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.vo.ProcessingStatus;

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@UseCase
public class PollJobAlertsUseCaseImpl implements PollJobAlertsUseCase {

    public static final Period MAX_ALERT_AGE = Period.ofDays(7);

    private static final System.Logger LOG = System.getLogger(PollJobAlertsUseCaseImpl.class.getName());

    private final MailboxGateway mailboxGateway;
    private final ProcessEmailReceiverUseCase processEmailReceiverUseCase;
    private final ProcessingStatusRepository statusRepository;
    private final ProfileRepository profileRepository;

    public PollJobAlertsUseCaseImpl(MailboxGateway mailboxGateway, ProcessEmailReceiverUseCase processEmailReceiverUseCase,
                                    ProcessingStatusRepository statusRepository, ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
        this.mailboxGateway = mailboxGateway;
        this.processEmailReceiverUseCase = processEmailReceiverUseCase;
        this.statusRepository = statusRepository;
    }

    @Override
    public void execute(AlertSource source) {
        Profile profile = profileRepository.find();
        if (profile == null || !profile.isMonitoringEnabled()) {
            return;
        }
        LinkedAccount emailAccount = profile.findLinkedAccount(ExternalProvider.EMAIL);
        if (emailAccount == null || emailAccount.folder(source) == null) {
            return;
        }

        if (status(profile, source).isWaiting(LocalDateTime.now())) {
            save(profile, source, status(profile, source).checked());
            return;
        }

        List<EmailReceived> alerts;
        try {
            alerts = mailboxGateway.fetchUnreadJobAlerts(emailAccount, source, LocalDate.now().minus(MAX_ALERT_AGE));
        } catch (Exception e) {
            save(profile, source, status(profile, source).failed(e).checked());
            throw e;
        }

        if (alerts.isEmpty()) {
            save(profile, source, status(profile, source).queued(0).checked());
            return;
        }

        for (int i = 0; i < alerts.size(); i += source.alertsPerRun) {
            List<EmailReceived> run = alerts.subList(i, Math.min(i + source.alertsPerRun, alerts.size()));
            String subject = run.size() == 1 ? run.getFirst().email().subject() : run.size() + " emails do " + source.label;
            save(profile, source, status(profile, source).analysing(subject, alerts.size() - i - run.size()));
            try {
                LOG.log(Level.INFO, "Processando alerta: {0}", subject);
                boolean siteAccessed = processEmailReceiverUseCase.execute(profile, source, run);
                mailboxGateway.markAsRead(emailAccount, source, run.stream().map(EmailReceived::messageId).toList());
                save(profile, source, status(profile, source).succeeded());
                if (siteAccessed) {
                    waitInterval(profile, source);
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Falha ao processar alerta '" + subject + "', será tentado de novo depois do intervalo", e);
                save(profile, source, status(profile, source).failed(e));
                Duration wait = e instanceof RateLimitedException limited && limited.retryAfter().compareTo(Duration.ofMinutes(profile.pollMin())) > 0
                        ? limited.retryAfter() : nextInterval(profile.pollMin(), profile.pollMax());
                save(profile, source, status(profile, source).waitUntil(LocalDateTime.now().plus(wait)).checked());
                return;
            }
        }
        save(profile, source, status(profile, source).queued(0).checked());
    }

    private void waitInterval(Profile profile, AlertSource source) {
        save(profile, source, status(profile, source).waitUntil(LocalDateTime.now().plus(nextInterval(profile.pollMin(), profile.pollMax()))).checked());
    }

    public static Duration nextInterval(int minMinutes, int maxMinutes) {
        long jitterSeconds = ThreadLocalRandom.current().nextLong((maxMinutes - minMinutes) * 60L + 1);
        return Duration.ofMinutes(minMinutes).plusSeconds(jitterSeconds);
    }

    private ProcessingStatus status(Profile profile, AlertSource source) {
        return statusRepository.find(profile.id(), source);
    }

    private void save(Profile profile, AlertSource source, ProcessingStatus status) {
        statusRepository.save(profile.id(), source, status);
    }
}
