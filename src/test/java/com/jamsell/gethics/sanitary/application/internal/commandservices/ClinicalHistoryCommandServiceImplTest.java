package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

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
    private final ClinicalHistoryCommandServiceImpl service = new ClinicalHistoryCommandServiceImpl(repository, CLOCK);
    private final UUID animalId = UUID.randomUUID();

    private RegisterSanitaryEventCommand command(LocalDateTime occurredAt) {
        return new RegisterSanitaryEventCommand(animalId, SanitaryEventType.DISEASE, occurredAt, "Fiebre");
    }

    @Test
    void createsClinicalHistoryWhenAnimalHasNone() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());
        when(repository.save(any(ClinicalHistory.class))).thenAnswer(i -> i.getArgument(0));

        var event = service.handle(command(LocalDateTime.now().minusDays(1)));

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

        var event = service.handle(command(LocalDateTime.now()));

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

        var event = service.handle(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, TODAY, "Ivermectina"));

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
}
