package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.commandservices.ClinicalHistoryCommandServiceImpl;
import com.jamsell.gethics.sanitary.application.internal.commandservices.VaccinationReminderCommandServiceImpl;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationDeliveryException;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ProcessVaccinationRemindersCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.domain.services.SanitaryScheduleService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Flujo completo (servicio de aplicacion + repositorios reales sobre PostgreSQL) con un NotificationService de prueba y
 * un Clock fijo. Cada test hace rollback y solo mira los eventos que el propio test sembro.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VaccinationReminderFlowTest.TestBeans.class, VaccinationReminderCommandServiceImpl.class, ClinicalHistoryCommandServiceImpl.class,
        ReminderRepositoryImpl.class, ReminderCandidateQueryRepositoryImpl.class, ClinicalHistoryRepositoryImpl.class})
class VaccinationReminderFlowTest {

    // 08:00 en Lima: "hoy" = 2042-06-07, por lo que se avisa de las vacunas del 2042-06-10.
    private static final Instant NOW = Instant.parse("2042-06-07T13:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final LocalDate EVENT_DATE = LocalDate.of(2042, 6, 10);
    private static final ProcessVaccinationRemindersCommand COMMAND = new ProcessVaccinationRemindersCommand();

    static class RecordingNotifications implements NotificationService {
        final List<VaccinationReminderNotification> sent = new ArrayList<>();
        final Set<UUID> failingEvents = new HashSet<>();

        @Override
        public void sendVaccinationReminder(VaccinationReminderNotification n) {
            if (failingEvents.contains(n.sanitaryEventId())) {
                throw new NotificationDeliveryException("proveedor caido");
            }
            sent.add(n);
        }

        long sentFor(UUID eventId) {
            return sent.stream().filter(n -> n.sanitaryEventId().equals(eventId)).count();
        }
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        RecordingNotifications notifications() {
            return new RecordingNotifications();
        }

        @Bean
        Clock clock() {
            return Clock.fixed(NOW, ZoneId.of("America/Lima"));
        }

        @Bean
        SanitaryScheduleService sanitaryScheduleService() {
            return new SanitaryScheduleService();
        }
    }

    @Autowired
    VaccinationReminderCommandServiceImpl service;
    @Autowired
    RecordingNotifications notifications;
    @Autowired
    ClinicalHistoryRepository histories;
    @Autowired
    ClinicalHistoryCommandServiceImpl clinicalHistoryCommands;
    @Autowired
    EntityManager em;

    private SanitaryEvent seedScheduledVaccination() {
        var history = new ClinicalHistory(UUID.randomUUID());
        var event = history.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, "Aftosa", TODAY);
        histories.save(history);
        em.clear();
        return event;
    }

    private List<Reminder> remindersOf(SanitaryEvent event) {
        em.clear();
        return em.createQuery("select r from Reminder r where r.sanitaryEvent.id = :id", Reminder.class)
                .setParameter("id", event.getId()).getResultList();
    }

    @Test
    void vaccinationThreeDaysAheadIsNotifiedAndMarkedSent() {
        var event = seedScheduledVaccination();

        service.handle(COMMAND);

        assertEquals(1, notifications.sentFor(event.getId()));
        var reminders = remindersOf(event);
        assertEquals(1, reminders.size());
        assertEquals(ReminderStatus.SENT, reminders.getFirst().getStatus());
        assertEquals(LocalDate.of(2042, 6, 7).atStartOfDay(), reminders.getFirst().getScheduledFor());
        assertEquals(NOW, reminders.getFirst().getSentAt());
    }

    @Test
    void sameEventProcessedTwiceSendsASingleNotification() {
        var event = seedScheduledVaccination();

        service.handle(COMMAND);
        service.handle(COMMAND);

        assertEquals(1, notifications.sentFor(event.getId()));
        assertEquals(1, remindersOf(event).size());
    }

    /** Registra como aplicada la vacuna programada mediante la transicion real SCHEDULED -> COMPLETED. */
    private void applyVaccination(SanitaryEvent event) {
        clinicalHistoryCommands.handle(new CompleteScheduledEventCommand(
                event.getClinicalHistory().getAnimalId(), event.getId(), TODAY.atTime(7, 30), null));
        em.clear();
    }

    @Test
    void completedEventSendsNoNotification() {
        var event = seedScheduledVaccination();
        applyVaccination(event);

        service.handle(COMMAND);

        assertEquals(0, notifications.sentFor(event.getId()));
        assertEquals(0, remindersOf(event).size());
        var stored = em.find(SanitaryEvent.class, event.getId());
        assertEquals(SanitaryEventStatus.COMPLETED, stored.getStatus());
        assertEquals(EVENT_DATE, stored.getScheduledDate());
    }

    @Test
    void failedReminderIsNotRetriedOnceTheVaccinationWasApplied() {
        var event = seedScheduledVaccination();
        notifications.failingEvents.add(event.getId());
        service.handle(COMMAND);
        assertEquals(ReminderStatus.FAILED, remindersOf(event).getFirst().getStatus());
        notifications.failingEvents.clear();

        applyVaccination(event);
        service.handle(COMMAND);

        assertEquals(0, notifications.sentFor(event.getId()));
        var reminders = remindersOf(event);
        assertEquals(1, reminders.size());
        assertEquals(ReminderStatus.FAILED, reminders.getFirst().getStatus());
        assertEquals(1, reminders.getFirst().getAttempts());
    }

    @Test
    void deliveryFailureLeavesFailedReminderAndALaterRunRetriesIt() {
        var event = seedScheduledVaccination();
        notifications.failingEvents.add(event.getId());

        service.handle(COMMAND);

        assertEquals(0, notifications.sentFor(event.getId()));
        var failed = remindersOf(event).getFirst();
        assertEquals(ReminderStatus.FAILED, failed.getStatus());
        assertEquals(1, failed.getAttempts());

        notifications.failingEvents.clear();
        service.handle(COMMAND);

        assertEquals(1, notifications.sentFor(event.getId()));
        var retried = remindersOf(event);
        assertEquals(1, retried.size());
        assertEquals(ReminderStatus.SENT, retried.getFirst().getStatus());
        assertEquals(2, retried.getFirst().getAttempts());

        service.handle(COMMAND);
        assertEquals(1, notifications.sentFor(event.getId()));
    }
}
