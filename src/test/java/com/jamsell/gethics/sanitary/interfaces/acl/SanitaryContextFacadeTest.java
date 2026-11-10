package com.jamsell.gethics.sanitary.interfaces.acl;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.queries.GetCompletedEventsByAnimalsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryQueryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SanitaryContextFacadeTest {

    private final ClinicalHistoryCommandService commands = mock(ClinicalHistoryCommandService.class);
    private final ClinicalHistoryQueryService queries = mock(ClinicalHistoryQueryService.class);
    private final SanitaryContextFacade facade = new SanitaryContextFacade(commands, queries);

    private final UUID animalId = UUID.randomUUID();
    private final ClinicalHistory history = new ClinicalHistory(animalId);

    @Test
    void registersTheVeterinaryCareAsAnIdempotentTreatmentEvent() {
        var requestId = UUID.randomUUID();
        var occurredAt = LocalDateTime.of(2026, 5, 1, 10, 30);

        facade.registerVeterinaryCare(animalId, requestId, occurredAt, "Mastitis tratada");

        verify(commands).handle(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt,
                "Mastitis tratada", requestId));
    }

    @Test
    void schedulesTheFollowUpAsAnIdempotentCheckup() {
        var requestId = UUID.randomUUID();

        facade.scheduleVeterinaryFollowUp(animalId, requestId, LocalDate.of(2027, 1, 15), "Control");

        verify(commands).handle(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP,
                LocalDate.of(2027, 1, 15), "Control", requestId));
    }

    @Test
    void summaryReportsTheCompletedEventsAndTheMostRecentOne() {
        var older = history.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 1, 10, 9, 0), null);
        var newest = history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2026, 3, 5, 9, 0), null);
        when(queries.handle(new GetCompletedEventsByAnimalsQuery(Set.of(animalId)))).thenReturn(List.of(older, newest));

        var summary = facade.findClinicalSummary(animalId).orElseThrow();

        assertEquals(new ClinicalSummaryView(2, LocalDate.of(2026, 3, 5), "TREATMENT"), summary);
    }

    @Test
    void summariesOfSeveralAnimalsComeFromASingleQueryAndAreGroupedPerAnimal() {
        var otherAnimal = UUID.randomUUID();
        var otherHistory = new ClinicalHistory(otherAnimal);
        var e1 = history.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 1, 10, 9, 0), null);
        var e2 = otherHistory.registerEvent(SanitaryEventType.CHECKUP, LocalDateTime.of(2026, 2, 1, 9, 0), null);
        var e3 = otherHistory.registerEvent(SanitaryEventType.DISEASE, LocalDateTime.of(2026, 4, 1, 9, 0), null);
        var both = Set.of(animalId, otherAnimal, UUID.randomUUID());
        when(queries.handle(new GetCompletedEventsByAnimalsQuery(both))).thenReturn(List.of(e1, e2, e3));

        var result = facade.findClinicalSummaries(both);

        assertEquals(2, result.size());
        assertEquals(new ClinicalSummaryView(1, LocalDate.of(2025, 1, 10), "VACCINATION"), result.get(animalId));
        assertEquals(new ClinicalSummaryView(2, LocalDate.of(2026, 4, 1), "DISEASE"), result.get(otherAnimal));
    }

    @Test
    void summaryIsEmptyWhenTheAnimalHasNoAppliedEvents() {
        when(queries.handle(new GetCompletedEventsByAnimalsQuery(Set.of(animalId)))).thenReturn(List.of());

        assertTrue(facade.findClinicalSummary(animalId).isEmpty());
    }
}
