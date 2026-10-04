package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClinicalHistoryQueryServiceImplTest {

    // Fecha en que se programaron los fixtures: el dominio no permite programar en una fecha ya vencida.
    private static final LocalDate SCHEDULED_ON = LocalDate.of(2026, 1, 1);

    private final ClinicalHistoryQueryRepository repository = mock(ClinicalHistoryQueryRepository.class);
    private final ClinicalHistoryQueryServiceImpl service = new ClinicalHistoryQueryServiceImpl(repository);
    private final UUID animalId = UUID.randomUUID();
    private final ClinicalHistory history = new ClinicalHistory(animalId);

    @Test
    void queriesTheRepositoryWithTheRequestedAnimal() {
        when(repository.findEventsByAnimalId(animalId)).thenReturn(List.of());

        service.handle(new GetClinicalHistoryQuery(animalId));

        verify(repository).findEventsByAnimalId(animalId);
    }

    @Test
    void returnsEmptyListWhenThereAreNoEvents() {
        when(repository.findEventsByAnimalId(animalId)).thenReturn(List.of());

        assertTrue(service.handle(new GetClinicalHistoryQuery(animalId)).isEmpty());
    }

    @Test
    void sortsChronologicallyAndLeavesTheRepositoryListUntouched() {
        var future = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2027, 1, 1), null, SCHEDULED_ON);
        var recent = history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2026, 3, 1, 9, 0), null);
        var old = history.registerEvent(SanitaryEventType.DISEASE, LocalDateTime.of(2025, 1, 1, 9, 0), null);
        var immutable = List.of(future, recent, old);
        when(repository.findEventsByAnimalId(animalId)).thenReturn(immutable);

        var result = service.handle(new GetClinicalHistoryQuery(animalId));

        assertEquals(List.of(old, recent, future), result);
        assertEquals(List.of(future, recent, old), immutable);
    }
}
