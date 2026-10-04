package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.application.internal.queryservices.SanitaryCalendarQueryServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa el query service real (para ejercitar la validacion del rango tecnico) con el puerto de repositorio mockeado. */
@WebMvcTest(SanitaryCalendarController.class)
@Import(SanitaryCalendarQueryServiceImpl.class)
@WithMockUser
class SanitaryCalendarControllerTest {

    // Fecha en que se programaron los fixtures: el dominio no permite programar en una fecha ya vencida.
    private static final LocalDate SCHEDULED_ON = LocalDate.of(2026, 10, 1);

    private static final String URL = "/api/v1/sanitary-calendar";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SanitaryCalendarQueryRepository repository;

    private ResultActions getCalendar(String query) throws Exception {
        return mockMvc.perform(get(URL + query));
    }

    @Test
    void returnsScheduledEventsOfTheMonthWithoutMessage() throws Exception {
        var animalId = UUID.randomUUID();
        var history = new ClinicalHistory(animalId);
        var vaccination = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2026, 10, 5), "Aftosa", SCHEDULED_ON);
        var checkup = history.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2026, 10, 20), null, SCHEDULED_ON);
        when(repository.findScheduledBetween(any(), any())).thenReturn(List.of(vaccination, checkup));

        getCalendar("?year=2026&month=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.events.length()").value(2))
                .andExpect(jsonPath("$.events[0].animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.events[0].type").value("VACCINATION"))
                .andExpect(jsonPath("$.events[0].scheduledDate").value("2026-10-05"))
                .andExpect(jsonPath("$.events[0].description").value("Aftosa"))
                .andExpect(jsonPath("$.events[0].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.events[1].scheduledDate").value("2026-10-20"));

        verify(repository).findScheduledBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1));
    }

    @Test
    void monthWithoutEventsReturns200WithEmptyListAndMessage() throws Exception {
        when(repository.findScheduledBetween(any(), any())).thenReturn(List.of());

        getCalendar("?year=2026&month=11")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(11))
                .andExpect(jsonPath("$.events").isEmpty())
                .andExpect(jsonPath("$.message").value("No hay actividades pendientes."));
    }

    @Test
    void monthZeroReturns400() throws Exception {
        getCalendar("?year=2026&month=0")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Periodo de calendario inválido."));
        verifyNoInteractions(repository);
    }

    @Test
    void monthThirteenReturns400() throws Exception {
        getCalendar("?year=2026&month=13").andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void yearBeyondPersistenceLimitReturns400BeforeQuerying() throws Exception {
        for (var period : List.of("year=9999999&month=1", "year=999999999&month=12", "year=2147483647&month=1",
                "year=5874897&month=1", "year=-4713&month=12", "year=-999999999&month=1")) {
            getCalendar("?" + period)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Periodo de calendario inválido."));
        }
        verifyNoInteractions(repository);
    }

    @Test
    void supportedLimitsAndOrdinaryYearsReturn200() throws Exception {
        when(repository.findScheduledBetween(any(), any())).thenReturn(List.of());

        getCalendar("?year=2026&month=1").andExpect(status().isOk());
        getCalendar("?year=5874896&month=12").andExpect(status().isOk());
        getCalendar("?year=-4712&month=1").andExpect(status().isOk());
    }

    @Test
    void nonNumericParametersReturn400() throws Exception {
        getCalendar("?year=abc&month=10").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: year"));
        getCalendar("?year=2026&month=octubre").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: month"));
        verifyNoInteractions(repository);
    }

    @Test
    void missingParametersReturn400() throws Exception {
        getCalendar("?month=10").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro obligatorio ausente: year"));
        getCalendar("?year=2026").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro obligatorio ausente: month"));
        getCalendar("").andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }
}
