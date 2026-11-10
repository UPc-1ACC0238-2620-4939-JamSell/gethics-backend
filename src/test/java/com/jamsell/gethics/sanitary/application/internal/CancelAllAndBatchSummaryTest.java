package com.jamsell.gethics.sanitary.application.internal;

import com.jamsell.gethics.sanitary.application.internal.commandservices.ClinicalHistoryCommandServiceImpl;
import com.jamsell.gethics.sanitary.application.internal.queryservices.ClinicalHistoryQueryServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelAllScheduledEventsCommand;
import com.jamsell.gethics.sanitary.domain.model.queries.GetCompletedEventsByAnimalsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Cancelacion masiva al salir un animal del hato y consulta de eventos completados de varios animales. */
class CancelAllAndBatchSummaryTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    private final ClinicalHistoryRepository repository = mock(ClinicalHistoryRepository.class);
    private final ClinicalHistoryCommandServiceImpl commands = new ClinicalHistoryCommandServiceImpl(repository,
            Clock.systemUTC(), mock(PlatformTransactionManager.class));
    private final UUID animalId = UUID.randomUUID();

    @Test
    void cancelsEveryScheduledEventAndSavesTheHistory() {
        var history = new ClinicalHistory(animalId);
        var first = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2027, 1, 1), null, TODAY);
        var second = history.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2027, 2, 1), null, TODAY);
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(history));

        var cancelled = commands.handle(new CancelAllScheduledEventsCommand(animalId));

        assertEquals(2, cancelled);
        assertEquals(SanitaryEventStatus.CANCELLED, first.getStatus());
        assertEquals(SanitaryEventStatus.CANCELLED, second.getStatus());
        verify(repository).save(history);
    }

    @Test
    void anAnimalWithoutHistoryHasNothingToCancel() {
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.empty());

        assertEquals(0, commands.handle(new CancelAllScheduledEventsCommand(animalId)));

        verify(repository, never()).save(any());
    }

    @Test
    void aHistoryWithoutScheduledEventsIsNotSavedAgain() {
        var history = new ClinicalHistory(animalId);
        history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2025, 5, 1, 9, 0), null);
        when(repository.findByAnimalId(animalId)).thenReturn(Optional.of(history));

        assertEquals(0, commands.handle(new CancelAllScheduledEventsCommand(animalId)));

        verify(repository, never()).save(any());
    }

    @Test
    void theBatchQueryReturnsWhatTheRepositoryFindsForTheRequestedAnimals() {
        var queryRepository = mock(ClinicalHistoryQueryRepository.class);
        var service = new ClinicalHistoryQueryServiceImpl(queryRepository);
        var history = new ClinicalHistory(animalId);
        var event = history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2025, 5, 1, 9, 0), null);
        when(queryRepository.findCompletedEventsByAnimalIds(Set.of(animalId))).thenReturn(List.of(event));

        assertEquals(List.of(event), service.handle(new GetCompletedEventsByAnimalsQuery(Set.of(animalId))));
    }

    @Test
    void anEmptyBatchDoesNotHitTheRepository() {
        var queryRepository = mock(ClinicalHistoryQueryRepository.class);
        var service = new ClinicalHistoryQueryServiceImpl(queryRepository);

        assertTrue(service.handle(new GetCompletedEventsByAnimalsQuery(Set.of())).isEmpty());
        assertTrue(service.handle(new GetCompletedEventsByAnimalsQuery(null)).isEmpty());

        verifyNoInteractions(queryRepository);
    }
}
