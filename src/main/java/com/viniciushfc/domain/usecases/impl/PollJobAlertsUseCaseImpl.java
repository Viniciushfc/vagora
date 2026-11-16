package com.viniciushfc.domain.usecases.impl;

import com.viniciushfc.domain.annotations.UseCase;
import com.viniciushfc.domain.entities.EmailReceived;
import com.viniciushfc.domain.entities.LinkedAccount;
import com.viniciushfc.domain.entities.Profile;
import com.viniciushfc.domain.enums.impl.ExternalProvider;
import com.viniciushfc.domain.exceptions.RateLimitedException;
import com.viniciushfc.domain.gateways.MailboxGateway;
import com.viniciushfc.domain.repositories.ProcessingStatusRepository;
import com.viniciushfc.domain.repositories.ProfileRepository;
import com.viniciushfc.domain.usecases.contracts.PollJobAlertsUseCase;
import com.viniciushfc.domain.usecases.contracts.ProcessEmailReceiverUseCase;
import com.viniciushfc.domain.utils.Utils;
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

    public static final Duration MIN_INTERVAL = Duration.ofMinutes(15);
    public static final Duration MAX_JITTER = Duration.ofMinutes(10);
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
    public void execute() {
        Profile profile = profileRepository.find();
        if (Utils.isNull(profile) || !profile.isMonitoringEnabled()) {
            return;
        }
        LinkedAccount emailAccount = profile.findLinkedAccount(ExternalProvider.EMAIL);
        if (Utils.isNull(emailAccount)) {
            return;
        }

        if (status(profile).isWaiting(LocalDateTime.now())) {
            save(profile, status(profile).checked());
            return;
        }

        List<EmailReceived> alerts;
        try {
            alerts = mailboxGateway.fetchUnreadJobAlerts(emailAccount, LocalDate.now().minus(MAX_ALERT_AGE));
        } catch (Exception e) {
            save(profile, status(profile).failed(e).checked());
            throw e;
        }

        if (alerts.isEmpty()) {
            save(profile, status(profile).queued(0).checked());
            return;
        }

        for (int i = 0; i < alerts.size(); i++) {
            EmailReceived alert = alerts.get(i);
            String subject = alert.email().subject();
            save(profile, status(profile).analysing(subject, alerts.size() - i - 1));
            try {
                LOG.log(Level.INFO, "Processando alerta: {0}", subject);
                boolean linkedinAccessed = processEmailReceiverUseCase.execute(profile, alert);
                mailboxGateway.markAsRead(emailAccount, alert.messageId());
                save(profile, status(profile).succeeded());
                if (linkedinAccessed) {
                    waitInterval(profile);
                    return;
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Falha ao processar alerta '" + subject + "', será tentado de novo depois do intervalo", e);
                save(profile, status(profile).failed(e));
                Duration wait = e instanceof RateLimitedException limited && limited.retryAfter().compareTo(MIN_INTERVAL) > 0
                        ? limited.retryAfter() : nextInterval();
                save(profile, status(profile).waitUntil(LocalDateTime.now().plus(wait)).checked());
                return;
            }
        }
        save(profile, status(profile).queued(0).checked());
    }

    private void waitInterval(Profile profile) {
        save(profile, status(profile).waitUntil(LocalDateTime.now().plus(nextInterval())).checked());
    }

    public static Duration nextInterval() {
        long jitterSeconds = ThreadLocalRandom.current().nextLong(MAX_JITTER.toSeconds() + 1);
        return MIN_INTERVAL.plusSeconds(jitterSeconds);
    }

    private ProcessingStatus status(Profile profile) {
        return statusRepository.find(profile.id());
    }

    private void save(Profile profile, ProcessingStatus status) {
        statusRepository.save(profile.id(), status);
    }
}
