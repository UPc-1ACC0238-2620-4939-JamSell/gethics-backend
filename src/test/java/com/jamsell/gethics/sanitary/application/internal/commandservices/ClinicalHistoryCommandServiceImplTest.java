package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClinicalHistoryCommandServiceImplTest {

    private final ClinicalHistoryRepository repository = mock(ClinicalHistoryRepository.class);
    private final ClinicalHistoryCommandServiceImpl service = new ClinicalHistoryCommandServiceImpl(repository);
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
}
