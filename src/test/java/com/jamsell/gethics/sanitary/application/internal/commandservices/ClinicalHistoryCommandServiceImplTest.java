package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotScheduledException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventRequestConflictException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClinicalHistoryCommandServiceImplTest {

    // 08:00 en Lima: "hoy" segun el Clock es 2042-06-07 (independiente de la fecha real de ejecucion).
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2042-06-07T13:00:00Z"), ZoneId.of("America/Lima"));

    private final ClinicalHistoryRepository repository = mock(ClinicalHistoryRepository.class);
    // Sin base de datos: un TransactionManager mock deja que TransactionTemplate ejecute el bloque tal cual.
    private final ClinicalHistoryCommandServiceImpl service =
            new ClinicalHistoryCommandServiceImpl(repository, CLOCK, mock(PlatformTransactionManager.class));
    private final UUID animalId = UUID.randomUUID();

    private RegisterSanitaryEventCommand command(LocalDateTime occurredAt) {
        return new RegisterSanitaryEventCommand(animalId, SanitaryEventType.DISEASE, occurredAt, "Fiebre");
    }

    @Test
    void createsClinicalHistoryWhenAnimalHasNone() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenAnswer(i -> i.getArgument(0));

        var event = service.handle(command(LocalDateTime.now().minusDays(1))).event();

        verify(repository).save(any(ClinicalHistory.class));
        assertEquals(animalId, event.getClinicalHistory().getAnimalId());
        assertEquals(1, event.getClinicalHistory().getEvents().size());
    }

    @Test
    void reusesExistingClinicalHistoryAndReturnsTheNewEvent() {
        var existing = new ClinicalHistory(animalId);
        existing.registerEvent(SanitaryEventType.CHECKUP, LocalDateTime.now().minusDays(5), "previo");
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var event = service.handle(command(LocalDateTime.now())).event();

        verify(repository).save(existing);
        assertSame(existing, event.getClinicalHistory());
        assertEquals(2, existing.getEvents().size());
        assertEquals(SanitaryEventType.DISEASE, event.getType());
    }

    @Test
    void dateAfterTodayIsRejectedAndNothingIsSaved() {
        var existing = new ClinicalHistory(animalId);
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));

        assertThrows(FutureEventDateException.class,
                () -> service.handle(command(LocalDate.now().plusDays(1).atTime(9, 0))));

        verify(repository, never()).save(any());
        assertTrue(existing.getEvents().isEmpty());
    }

    @Test
    void schedulesUsingTheClockDateAndCreatesTheHistoryWhenMissing() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenAnswer(i -> i.getArgument(0));

        var event = service.handle(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, TODAY, "Ivermectina"))
                .event();

        assertEquals(SanitaryEventStatus.SCHEDULED, event.getStatus());
        assertEquals(TODAY, event.getScheduledDate());
        assertEquals(animalId, event.getClinicalHistory().getAnimalId());
    }

    @Test
    void schedulingBeforeTheClockDateIsRejectedAndNothingIsSaved() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());

        assertThrows(PastScheduledDateException.class, () -> service.handle(
                new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, TODAY.minusDays(1), null)));

        verify(repository, never()).save(any());
    }

    @Test
    void completesTheSameScheduledEventAndSavesTheHistory() {
        var existing = new ClinicalHistory(animalId);
        var scheduled = existing.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(3), "Aftosa", TODAY);
        ReflectionTestUtils.setField(scheduled, "id", UUID.randomUUID());
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));
        var occurredAt = TODAY.atTime(9, 0);

        var event = service.handle(new CompleteScheduledEventCommand(animalId, scheduled.getId(), occurredAt, null));

        verify(repository).save(existing);
        assertSame(scheduled, event);
        assertEquals(1, existing.getEvents().size());
        assertEquals(SanitaryEventStatus.COMPLETED, event.getStatus());
        assertEquals(occurredAt, event.getOccurredAt());
        assertEquals(TODAY.plusDays(3), event.getScheduledDate());
    }

    @Test
    void completingForAnAnimalWithoutHistoryIsNotFound() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());

        assertThrows(SanitaryEventNotFoundException.class, () -> service.handle(
                new CompleteScheduledEventCommand(animalId, UUID.randomUUID(), TODAY.atTime(9, 0), null)));

        verify(repository, never()).save(any());
    }

    // ---- US-33: cancelacion ----

    private ClinicalHistory historyWithScheduledEvent() {
        var history = new ClinicalHistory(animalId);
        var scheduled = history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.plusDays(3), "Aftosa", TODAY);
        ReflectionTestUtils.setField(scheduled, "id", UUID.randomUUID());
        return history;
    }

    @Test
    void cancelsTheScheduledEventAndSavesTheHistory() {
        var existing = historyWithScheduledEvent();
        var scheduled = existing.getEvents().getFirst();
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));

        var event = service.handle(new CancelScheduledEventCommand(animalId, scheduled.getId()));

        verify(repository).save(existing);
        assertSame(scheduled, event);
        assertEquals(1, existing.getEvents().size());
        assertEquals(SanitaryEventStatus.CANCELLED, event.getStatus());
    }

    @Test
    void cancellingForAnAnimalWithoutHistoryIsNotFound() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());

        assertThrows(SanitaryEventNotFoundException.class,
                () -> service.handle(new CancelScheduledEventCommand(animalId, UUID.randomUUID())));

        verify(repository, never()).save(any());
    }

    @Test
    void cancellingAnEventThatBelongsToAnotherAnimalIsNotFound() {
        // El evento existe, pero en el historial de OTRO animal: el del animal indicado no lo contiene.
        var otherAnimalsEvent = historyWithScheduledEvent().getEvents().getFirst();
        var existing = new ClinicalHistory(animalId);
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));

        assertThrows(SanitaryEventNotFoundException.class,
                () -> service.handle(new CancelScheduledEventCommand(animalId, otherAnimalsEvent.getId())));

        verify(repository, never()).save(any());
        assertEquals(SanitaryEventStatus.SCHEDULED, otherAnimalsEvent.getStatus());
    }

    @Test
    void cancellingAnEventThatIsNoLongerScheduledIsRejectedAndNothingIsSaved() {
        var existing = historyWithScheduledEvent();
        var scheduled = existing.getEvents().getFirst();
        existing.completeScheduledEvent(scheduled.getId(), TODAY.atTime(9, 0), null, TODAY);
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(existing));

        assertThrows(SanitaryEventNotScheduledException.class,
                () -> service.handle(new CancelScheduledEventCommand(animalId, scheduled.getId())));

        verify(repository, never()).save(any());
        assertEquals(SanitaryEventStatus.COMPLETED, scheduled.getStatus());
    }

    // ---- US-35: idempotencia por clientRequestId ----

    private final UUID requestId = UUID.randomUUID();
    // El registro (US-11) valida contra la fecha real, no contra el Clock de 2042.
    private final LocalDateTime occurredAt = LocalDate.now().minusDays(1).atTime(8, 0);

    private RegisterSanitaryEventCommand register(UUID forAnimal, String description) {
        return new RegisterSanitaryEventCommand(forAnimal, SanitaryEventType.TREATMENT, occurredAt, description, requestId);
    }

    private ScheduleSanitaryEventCommand schedule(LocalDate date) {
        return new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, date, "Aftosa", requestId);
    }

    /** Evento ya guardado con requestId, tal como lo devuelve findEventByClientRequestId. */
    private SanitaryEvent storedRegistration() {
        return new ClinicalHistory(animalId).registerEvent(SanitaryEventType.TREATMENT, occurredAt, "Ivermectina", requestId);
    }

    private void savesNormally() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void withoutClientRequestIdItCreatesWithoutLookingForDuplicates() {
        savesNormally();

        var creation = service.handle(command(occurredAt));

        assertTrue(creation.created());
        assertNull(creation.event().getClientRequestId());
        verify(repository, never()).findEventByClientRequestId(any());
        verify(repository).save(any(ClinicalHistory.class));
    }

    @Test
    void aNewClientRequestIdCreatesTheEventAndStoresTheId() {
        savesNormally();
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.empty());

        var creation = service.handle(register(animalId, "Ivermectina"));

        assertTrue(creation.created());
        assertEquals(requestId, creation.event().getClientRequestId());
        verify(repository).save(any(ClinicalHistory.class));
    }

    @Test
    void anIdenticalRetryReturnsTheExistingEventWithoutCreatingAnother() {
        var stored = storedRegistration();
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.of(stored));

        var creation = service.handle(register(animalId, "Ivermectina"));

        assertFalse(creation.created());
        assertSame(stored, creation.event());
        verify(repository, never()).save(any());
        verify(repository, never()).findByAnimalId(any());
    }

    @Test
    void theSameIdWithDifferentDataIsAConflict() {
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.of(storedRegistration()));

        var ex = assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(register(animalId, "otra")));

        assertEquals("El clientRequestId " + requestId + " ya se uso con datos distintos.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void theSameIdForAnotherAnimalIsAConflict() {
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.of(storedRegistration()));

        assertThrows(SanitaryEventRequestConflictException.class,
                () -> service.handle(register(UUID.randomUUID(), "Ivermectina")));
        verify(repository, never()).save(any());
    }

    @Test
    void anIdUsedToRegisterCannotBeReusedToSchedule() {
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.of(storedRegistration()));

        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(schedule(TODAY.plusDays(3))));
        verify(repository, never()).save(any());
    }

    @Test
    void schedulingIsIdempotentToo() {
        var date = TODAY.plusDays(3);
        var stored = new ClinicalHistory(animalId).scheduleEvent(SanitaryEventType.VACCINATION, date, "Aftosa", TODAY, requestId);
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.empty(), Optional.of(stored));
        savesNormally();

        var first = service.handle(schedule(date));
        var retry = service.handle(schedule(date));

        assertTrue(first.created());
        assertEquals(requestId, first.event().getClientRequestId());
        assertFalse(retry.created());
        assertSame(stored, retry.event());
        verify(repository, times(1)).save(any());
        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(schedule(date.plusDays(1))));
    }

    @Test
    void aConcurrentDuplicateDetectedByTheUniqueConstraintReturnsTheWinner() {
        var winner = storedRegistration();
        // La comprobacion previa no lo ve (la otra peticion aun no habia confirmado); la UNIQUE si.
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.empty(), Optional.of(winner));
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenThrow(new DataIntegrityViolationException("uk_client_request_id"));

        var creation = service.handle(register(animalId, "Ivermectina"));

        assertFalse(creation.created());
        assertSame(winner, creation.event());
        verify(repository, times(2)).findEventByClientRequestId(requestId);
    }

    @Test
    void aConcurrentDuplicateWithDifferentDataIsAConflict() {
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.empty(), Optional.of(storedRegistration()));
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenThrow(new DataIntegrityViolationException("uk_client_request_id"));

        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(register(animalId, "otra")));
    }

    @Test
    void anIntegrityErrorNotCausedByTheClientRequestIdIsRethrown() {
        var unrelated = new DataIntegrityViolationException("otra constraint");
        when(repository.findEventByClientRequestId(requestId)).thenReturn(Optional.empty());
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenThrow(unrelated);

        assertSame(unrelated, assertThrows(DataIntegrityViolationException.class,
                () -> service.handle(register(animalId, "Ivermectina"))));
    }

    @Test
    void withoutClientRequestIdAnIntegrityErrorIsRethrownWithoutLookup() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenThrow(new DataIntegrityViolationException("uk_animal_id"));

        assertThrows(DataIntegrityViolationException.class, () -> service.handle(command(occurredAt)));
        verify(repository, never()).findEventByClientRequestId(any());
    }
}
