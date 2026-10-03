package com.jamsell.gethics.sanitary.interfaces.rest;

import com.jamsell.gethics.sanitary.application.internal.queryservices.ClinicalHistoryQueryServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa el query service real (para ejercitar el orden) con el puerto de repositorio mockeado. */
@WebMvcTest(ClinicalHistoryController.class)
@Import(ClinicalHistoryQueryServiceImpl.class)
@WithMockUser
class ClinicalHistoryControllerTest {

    private final UUID animalId = UUID.randomUUID();
    private final String url = "/api/v1/animals/" + animalId + "/clinical-history";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ClinicalHistoryQueryRepository repository;

    @Test
    void returnsAllEventsInChronologicalOrderMixingCompletedAndScheduled() throws Exception {
        var history = new ClinicalHistory(animalId);
        var future = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2027, 1, 20), "Brucelosis");
        var recent = history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2026, 3, 1, 10, 0), "Ivermectina");
        var old = history.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 12, 1, 8, 30), "Aftosa");
        when(repository.findEventsByAnimalId(animalId)).thenReturn(List.of(future, recent, old));

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.events.length()").value(3))
                .andExpect(jsonPath("$.events[0].description").value("Aftosa"))
                .andExpect(jsonPath("$.events[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.events[0].occurredAt").value("2025-12-01T08:30:00"))
                .andExpect(jsonPath("$.events[0].scheduledDate").value(nullValue()))
                .andExpect(jsonPath("$.events[1].description").value("Ivermectina"))
                .andExpect(jsonPath("$.events[2].description").value("Brucelosis"))
                .andExpect(jsonPath("$.events[2].status").value("SCHEDULED"))
                .andExpect(jsonPath("$.events[2].scheduledDate").value("2027-01-20"))
                .andExpect(jsonPath("$.events[2].occurredAt").value(nullValue()));

        verify(repository).findEventsByAnimalId(animalId);
    }

    @Test
    void exposesExactlyTheDocumentedFields() throws Exception {
        var eventId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        var event = new ClinicalHistory(animalId)
                .registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2026, 3, 1, 10, 0), "Aftosa");
        ReflectionTestUtils.setField(event, "id", eventId);
        when(repository.findEventsByAnimalId(animalId)).thenReturn(List.of(event));

        // Comparacion estricta: cualquier campo adicional (clinicalHistoryId, createdAt...) la hace fallar.
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"animalId":"%s",
                         "events":[{"id":"%s","type":"VACCINATION","status":"COMPLETED",
                                    "occurredAt":"2026-03-01T10:00:00","scheduledDate":null,"description":"Aftosa"}],
                         "message":null}""".formatted(animalId, eventId), true));
    }

    @Test
    void historyWithoutEventsReturns200WithEmptyListAndMessage() throws Exception {
        when(repository.findEventsByAnimalId(animalId)).thenReturn(List.of());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"animalId":"%s","events":[],"message":"Sin registros."}""".formatted(animalId), true));
    }

    @Test
    void nonExistentAnimalAlsoReturns200WithSinRegistros() throws Exception {
        // Livestock aun no permite validar el animal: un UUID valido pero inexistente es indistinguible de un animal sin eventos.
        var unknown = UUID.randomUUID();
        when(repository.findEventsByAnimalId(unknown)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/animals/" + unknown + "/clinical-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(unknown.toString()))
                .andExpect(jsonPath("$.events").isEmpty())
                .andExpect(jsonPath("$.message").value("Sin registros."));
    }

    @Test
    void nonUuidAnimalIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/animals/123/clinical-history"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: animalId"));
        verifyNoInteractions(repository);
    }
}
