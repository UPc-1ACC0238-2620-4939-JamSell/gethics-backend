package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.queries.GetUpcomingSanitaryEventsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.UpcomingSanitaryEvents;
import com.jamsell.gethics.sanitary.domain.services.UpcomingSanitaryEventsQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UpcomingSanitaryEventsController.class)
@WithMockUser
class UpcomingSanitaryEventsControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final String URL = "/api/v1/sanitary-events/upcoming";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UpcomingSanitaryEventsQueryService queryService;

    @Test
    void returnsOverdueAndUpcomingEventsWithTheirProximity() throws Exception {
        var animalId = UUID.randomUUID();
        var history = new ClinicalHistory(animalId);
        var overdue = history.scheduleEvent(SanitaryEventType.VACCINATION, TODAY.minusDays(2), "Aftosa", TODAY.minusDays(10));
        var soon = history.scheduleEvent(SanitaryEventType.CHECKUP, TODAY.plusDays(3), null, TODAY);
        var overdueId = UUID.randomUUID();
        ReflectionTestUtils.setField(overdue, "id", overdueId);
        when(queryService.handle(any())).thenReturn(new UpcomingSanitaryEvents(TODAY, 7, List.of(overdue, soon)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today").value("2042-06-07"))
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].id").value(overdueId.toString()))
                .andExpect(jsonPath("$.events[0].animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.events[0].type").value("VACCINATION"))
                .andExpect(jsonPath("$.events[0].scheduledDate").value("2042-06-05"))
                .andExpect(jsonPath("$.events[0].daysUntil").value(-2))
                .andExpect(jsonPath("$.events[0].overdue").value(true))
                .andExpect(jsonPath("$.events[1].type").value("CHECKUP"))
                .andExpect(jsonPath("$.events[1].daysUntil").value(3))
                .andExpect(jsonPath("$.events[1].overdue").value(false));

        verify(queryService).handle(new GetUpcomingSanitaryEventsQuery(GetUpcomingSanitaryEventsQuery.DEFAULT_DAYS));
    }

    @Test
    void anEventDueTodayIsNotOverdue() throws Exception {
        var today = new ClinicalHistory(UUID.randomUUID()).scheduleEvent(SanitaryEventType.TREATMENT, TODAY, null, TODAY);
        when(queryService.handle(any())).thenReturn(new UpcomingSanitaryEvents(TODAY, 7, List.of(today)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].daysUntil").value(0))
                .andExpect(jsonPath("$.events[0].overdue").value(false));
    }

    @Test
    void withoutPendingEventsReturnsEmptyListAndMessage() throws Exception {
        when(queryService.handle(any())).thenReturn(new UpcomingSanitaryEvents(TODAY, 14, List.of()));

        mockMvc.perform(get(URL + "?days=14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(0))
                .andExpect(jsonPath("$.message").value("No hay alertas pendientes."));

        verify(queryService).handle(new GetUpcomingSanitaryEventsQuery(14));
    }

    @Test
    void windowOutOfRangeReturns400WithoutReachingTheService() throws Exception {
        mockMvc.perform(get(URL + "?days=31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La ventana de alerta debe estar entre 1 y 30 dias."));
        mockMvc.perform(get(URL + "?days=0")).andExpect(status().isBadRequest());

        verifyNoInteractions(queryService);
    }
}
