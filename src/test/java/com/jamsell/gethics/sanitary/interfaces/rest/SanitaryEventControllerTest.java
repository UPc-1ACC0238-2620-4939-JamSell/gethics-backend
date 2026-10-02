package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.domain.exceptions.FutureEventDateException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
        when(commandService.handle(any())).thenThrow(new FutureEventDateException());
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
}
