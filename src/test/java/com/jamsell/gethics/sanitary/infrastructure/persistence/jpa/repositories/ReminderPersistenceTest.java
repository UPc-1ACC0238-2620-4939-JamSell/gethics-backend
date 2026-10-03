package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.Reminder;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.ReminderStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderCandidateQueryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ReminderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada
 * test hace rollback y solo verifica las filas que el propio test sembro (por id): no borra ni asume tablas vacias.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ReminderRepositoryImpl.class, ReminderCandidateQueryRepositoryImpl.class, ClinicalHistoryRepositoryImpl.class})
class ReminderPersistenceTest {

    private static final LocalDate EVENT_DATE = LocalDate.of(2042, 6, 10);
    private static final LocalDateTime REMINDER_TIME = LocalDate.of(2042, 6, 7).atStartOfDay();
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);

    @Autowired
    ReminderRepository reminders;
    @Autowired
    ReminderCandidateQueryRepository candidates;
    @Autowired
    ClinicalHistoryRepository histories;
    @Autowired
    EntityManager em;

    private ClinicalHistory history() {
        return new ClinicalHistory(UUID.randomUUID());
    }

    private void persist(ClinicalHistory history) {
        histories.save(history);
        em.clear();
    }

    private void setStatus(SanitaryEvent event, String status) {
        em.createNativeQuery("update sanitary_events set status = :status where id = :id")
                .setParameter("status", status).setParameter("id", event.getId()).executeUpdate();
        em.clear();
    }

    private List<UUID> candidateIdsAmong(SanitaryEvent... seeded) {
        var ids = java.util.Arrays.stream(seeded).map(SanitaryEvent::getId).toList();
        return candidates.findVaccinationsAwaitingReminder(EVENT_DATE, REMINDER_TIME).stream()
                .map(SanitaryEvent::getId).filter(ids::contains).toList();
    }

    @Test
    void vaccinationScheduledForTheEventDateIsACandidate() {
        var h = history();
        var due = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, "Aftosa");
        persist(h);

        var found = candidates.findVaccinationsAwaitingReminder(EVENT_DATE, REMINDER_TIME).stream()
                .filter(e -> e.getId().equals(due.getId())).findFirst().orElseThrow();

        assertEquals(h.getAnimalId(), found.getClinicalHistory().getAnimalId());
    }

    @Test
    void eventsOnOtherDatesAreNotCandidates() {
        var h = history();
        var twoDays = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE.minusDays(1), null);
        var fourDays = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE.plusDays(1), null);
        persist(h);

        assertEquals(List.of(), candidateIdsAmong(twoDays, fourDays));
    }

    @Test
    void otherEventTypesAreNotCandidates() {
        var h = history();
        var treatment = h.scheduleEvent(SanitaryEventType.TREATMENT, EVENT_DATE, null);
        var checkup = h.scheduleEvent(SanitaryEventType.CHECKUP, EVENT_DATE, null);
        var vaccination = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);

        assertEquals(List.of(vaccination.getId()), candidateIdsAmong(treatment, checkup, vaccination));
    }

    @Test
    void completedAndCancelledEventsAreNotCandidates() {
        var h = history();
        var completed = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, "aplicada");
        var cancelled = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, "cancelada");
        var active = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, "vigente");
        h.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2020, 6, 1, 9, 0), "otro registro COMPLETED");
        persist(h);
        // No existen transiciones a COMPLETED/CANCELLED (fuera de alcance): se simulan directamente en la BD.
        setStatus(completed, "COMPLETED");
        setStatus(cancelled, "CANCELLED");

        assertEquals(List.of(active.getId()), candidateIdsAmong(completed, cancelled, active));
    }

    @Test
    void eventWithAReminderForTheSameTimeIsNoLongerACandidate() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);
        reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME));

        assertEquals(List.of(), candidateIdsAmong(event));
    }

    @Test
    void reminderForAnotherAnticipationDoesNotExcludeTheEvent() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);
        reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME.plusDays(2)));

        assertEquals(List.of(event.getId()), candidateIdsAmong(event));
    }

    @Test
    void createIfAbsentReturnsEmptyForTheSameEventAndScheduledFor() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);

        var first = reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME));
        var second = reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME));

        assertTrue(first.isPresent());
        assertNotNull(first.get().getId());
        assertTrue(second.isEmpty());
        assertEquals(1L, em.createQuery("select count(r) from Reminder r where r.sanitaryEvent.id = :id", Long.class)
                .setParameter("id", event.getId()).getSingleResult());
    }

    @Test
    void sameEventAcceptsRemindersForDifferentScheduledFor() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);

        assertTrue(reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME)).isPresent());
        assertTrue(reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME.plusDays(2))).isPresent());

        assertEquals(2L, em.createQuery("select count(r) from Reminder r where r.sanitaryEvent.id = :id", Long.class)
                .setParameter("id", event.getId()).getSingleResult());
    }

    @Test
    void persistedReminderRoundTripsItsDeliveryState() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);
        var reminder = reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME)).orElseThrow();
        var sentAt = Instant.parse("2042-06-07T13:00:00Z");
        reminder.markSent(sentAt);
        reminders.save(reminder);
        em.clear();

        var reloaded = em.find(Reminder.class, reminder.getId());

        assertEquals(ReminderStatus.SENT, reloaded.getStatus());
        assertEquals(1, reloaded.getAttempts());
        assertEquals(sentAt, reloaded.getSentAt());
        assertEquals(REMINDER_TIME, reloaded.getScheduledFor());
        assertEquals(event.getId(), reloaded.getSanitaryEventId());
    }

    @Test
    void retryableAreFailedRemindersOfScheduledEventsNotInThePast() {
        var h = history();
        var upcoming = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(2), null);
        var dueToday = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY, null);
        var past = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.minusDays(1), null);
        var completed = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(2), null);
        var pending = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(2), null);
        var sent = h.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(2), null);
        persist(h);
        var failedUpcoming = failed(upcoming);
        var failedDueToday = failed(dueToday);
        var seeded = List.of(failedUpcoming, failedDueToday, failed(past), failed(completed));
        reminders.createIfAbsent(Reminder.create(pending, REMINDER_TIME));
        var sentReminder = reminders.createIfAbsent(Reminder.create(sent, REMINDER_TIME)).orElseThrow();
        sentReminder.markSent(Instant.now());
        reminders.save(sentReminder);
        setStatus(completed, "COMPLETED");

        var seededIds = new java.util.HashSet<>(seeded.stream().map(Reminder::getId).toList());
        seededIds.addAll(List.of(sentReminder.getId()));
        var retryable = reminders.findRetryable(TODAY).stream().filter(r -> seededIds.contains(r.getId())).toList();

        assertEquals(java.util.Set.of(failedUpcoming.getId(), failedDueToday.getId()),
                retryable.stream().map(Reminder::getId).collect(java.util.stream.Collectors.toSet()));
        // Evento e historial vienen cargados: se usan fuera de la transaccion de lectura.
        assertDoesNotThrow(() -> retryable.forEach(r -> r.getSanitaryEvent().getClinicalHistory().getAnimalId()));
    }

    private Reminder failed(SanitaryEvent event) {
        var reminder = reminders.createIfAbsent(Reminder.create(event, REMINDER_TIME)).orElseThrow();
        reminder.markFailed();
        return reminders.save(reminder);
    }

    @Test
    void remindersTableHasARealForeignKeyToSanitaryEventsAndTheCompositeUnique() {
        @SuppressWarnings("unchecked")
        List<String> referencedTables = em.createNativeQuery("""
                select ccu.table_name
                from information_schema.table_constraints tc
                join information_schema.constraint_column_usage ccu
                  on tc.constraint_name = ccu.constraint_name and tc.constraint_schema = ccu.constraint_schema
                where tc.table_name = 'reminders' and tc.constraint_type = 'FOREIGN KEY'""").getResultList();
        @SuppressWarnings("unchecked")
        List<String> uniqueColumns = em.createNativeQuery("""
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_name = kcu.constraint_name and tc.constraint_schema = kcu.constraint_schema
                where tc.table_name = 'reminders' and tc.constraint_type = 'UNIQUE'""").getResultList();

        assertTrue(referencedTables.contains("sanitary_events"));
        assertEquals(java.util.Set.of("sanitary_event_id", "scheduled_for"), new java.util.HashSet<>(uniqueColumns));
    }

    // Debe ser el ultimo statement: la violacion deja abortada la transaccion de PostgreSQL.
    @Test
    void secondReminderWithTheSameEventAndScheduledForViolatesTheUniqueConstraint() {
        var h = history();
        var event = h.scheduleEvent(SanitaryEventType.VACCINATION, EVENT_DATE, null);
        persist(h);
        reminders.save(Reminder.create(event, REMINDER_TIME));

        assertThrows(DataIntegrityViolationException.class,
                () -> reminders.save(Reminder.create(event, REMINDER_TIME)));
    }
}
