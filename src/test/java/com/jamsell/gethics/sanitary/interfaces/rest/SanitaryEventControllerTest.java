package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.PastScheduledDateException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotScheduledException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SanitaryEventController.class)
@WithMockUser
class SanitaryEventControllerTest {

    private final UUID animalId = UUID.randomUUID();
    private final String url = "/api/v1/animals/" + animalId + "/sanitary-events";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClinicalHistoryCommandService commandService;

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void registersEventAndReturns201() throws Exception {
        var occurredAt = LocalDate.now().minusDays(2).atTime(10, 30, 15);
        var event = new ClinicalHistory(animalId).registerEvent(SanitaryEventType.VACCINATION, occurredAt, "Aftosa");
        when(commandService.handle(any(RegisterSanitaryEventCommand.class))).thenReturn(event);

        postJson("{\"type\":\"VACCINATION\",\"occurredAt\":\"" + occurredAt + "\",\"description\":\"Aftosa\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.type").value("VACCINATION"))
                .andExpect(jsonPath("$.occurredAt").value(occurredAt.toString()))
                .andExpect(jsonPath("$.description").value("Aftosa"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(commandService).handle(new RegisterSanitaryEventCommand(
                animalId, SanitaryEventType.VACCINATION, occurredAt, "Aftosa"));
    }

    @Test
    void dateAfterTodayReturns400WithValidationMessage() throws Exception {
        when(commandService.handle(any(RegisterSanitaryEventCommand.class))).thenThrow(new FutureEventDateException());
        var tomorrow = LocalDate.now().plusDays(1).atTime(8, 0, 1);

        postJson("{\"type\":\"TREATMENT\",\"occurredAt\":\"" + tomorrow + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha del evento no puede ser posterior a hoy."));
    }

    @Test
    void missingTypeReturns400() throws Exception {
        postJson("{\"occurredAt\":\"2025-01-01T10:00:00\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tipo de evento es obligatorio."));
        verifyNoInteractions(commandService);
    }

    @Test
    void missingOccurredAtReturns400() throws Exception {
        postJson("{\"type\":\"CHECKUP\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha del evento es obligatoria."));
        verifyNoInteractions(commandService);
    }

    @Test
    void unknownTypeReturns400() throws Exception {
        postJson("{\"type\":\"FLU\",\"occurredAt\":\"2025-01-01T10:00:00\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @Test
    void removedDewormingTypeReturns400() throws Exception {
        postJson("{\"type\":\"DEWORMING\",\"occurredAt\":\"2025-01-01T10:00:00\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        postJson("{\"type\":\"CHECKUP\",\"occurredAt\":\"ayer\"}").andExpect(status().isBadRequest());
        verifyNoInteractions(commandService);
    }

    @Test
    void nonUuidAnimalIdReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/animals/123/sanitary-events").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"CHECKUP\",\"occurredAt\":\"2025-01-01T10:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---- Programacion de eventos (US-13: crea el evento SCHEDULED que luego dispara el recordatorio) ----

    private ResultActions postJson(String path, String body) throws Exception {
        return mockMvc.perform(post(url + path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @ParameterizedTest
    @EnumSource(SanitaryEventType.class)
    void schedulesAnyEventTypeAndReturns201(SanitaryEventType type) throws Exception {
        var date = LocalDate.now().plusDays(3);
        var event = new ClinicalHistory(animalId).scheduleEvent(type, date, "Plan", LocalDate.now());
        when(commandService.handle(any(ScheduleSanitaryEventCommand.class))).thenReturn(event);

        postJson("/scheduled", "{\"type\":\"" + type + "\",\"scheduledDate\":\"" + date + "\",\"description\":\"Plan\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.type").value(type.name()))
                .andExpect(jsonPath("$.scheduledDate").value(date.toString()))
                .andExpect(jsonPath("$.occurredAt").doesNotExist())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        verify(commandService).handle(new ScheduleSanitaryEventCommand(animalId, type, date, "Plan"));
    }

    @Test
    void schedulingInThePastReturns400WithoutReachingTheService() throws Exception {
        postJson("/scheduled", "{\"type\":\"VACCINATION\",\"scheduledDate\":\"" + LocalDate.now().minusDays(1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha programada no puede ser anterior a hoy."));
        verifyNoInteractions(commandService);
    }

    @Test
    void pastScheduledDateDetectedByTheDomainReturns400() throws Exception {
        when(commandService.handle(any(ScheduleSanitaryEventCommand.class))).thenThrow(new PastScheduledDateException());

        postJson("/scheduled", "{\"type\":\"VACCINATION\",\"scheduledDate\":\"" + LocalDate.now() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha programada no puede ser anterior a hoy."));
    }

    @Test
    void schedulingWithoutDateReturns400() throws Exception {
        postJson("/scheduled", "{\"type\":\"VACCINATION\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha programada es obligatoria."));
        verifyNoInteractions(commandService);
    }

    // ---- Registro como aplicado de un evento programado (US-13, Escenario 2) ----

    @Test
    void completesTheScheduledEventAndReturns200() throws Exception {
        var scheduledDate = LocalDate.now().plusDays(3);
        var occurredAt = LocalDate.now().atTime(9, 0, 15);
        var event = new ClinicalHistory(animalId).scheduleEvent(SanitaryEventType.VACCINATION, scheduledDate, "Aftosa", LocalDate.now());
        var eventId = UUID.randomUUID();
        ReflectionTestUtils.setField(event, "id", eventId);
        event.complete(occurredAt, null, LocalDate.now());
        when(commandService.handle(any(CompleteScheduledEventCommand.class))).thenReturn(event);

        postJson("/" + eventId + "/complete", "{\"occurredAt\":\"" + occurredAt + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.occurredAt").value(occurredAt.toString()))
                .andExpect(jsonPath("$.scheduledDate").value(scheduledDate.toString()))
                .andExpect(jsonPath("$.description").value("Aftosa"));

        verify(commandService).handle(new CompleteScheduledEventCommand(animalId, eventId, occurredAt, null));
    }

    @Test
    void completingAnUnknownEventReturns404() throws Exception {
        when(commandService.handle(any(CompleteScheduledEventCommand.class))).thenThrow(new SanitaryEventNotFoundException());

        postJson("/" + UUID.randomUUID() + "/complete", "{\"occurredAt\":\"2025-01-01T10:00:00\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El evento sanitario no existe para este animal."));
    }

    @Test
    void completingAnEventThatIsNotScheduledReturns409() throws Exception {
        when(commandService.handle(any(CompleteScheduledEventCommand.class))).thenThrow(new SanitaryEventNotScheduledException());

        postJson("/" + UUID.randomUUID() + "/complete", "{\"occurredAt\":\"2025-01-01T10:00:00\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void completingWithAFutureDateReturns400() throws Exception {
        when(commandService.handle(any(CompleteScheduledEventCommand.class))).thenThrow(new FutureEventDateException());

        postJson("/" + UUID.randomUUID() + "/complete", "{\"occurredAt\":\"" + LocalDate.now().plusDays(1).atTime(8, 0, 1) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha del evento no puede ser posterior a hoy."));
    }

    @Test
    void completingWithoutOccurredAtReturns400() throws Exception {
        postJson("/" + UUID.randomUUID() + "/complete", "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de aplicacion es obligatoria."));
        verifyNoInteractions(commandService);
    }
}
