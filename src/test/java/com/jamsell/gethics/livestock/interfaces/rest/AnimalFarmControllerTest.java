package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalFarmHistoryQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalFarmController.class)
@WithMockUser
class AnimalFarmControllerTest {

    private final UUID animalId = UUID.randomUUID();
    private final UUID farmA = UUID.randomUUID();
    private final UUID farmB = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnimalFarmCommandService commandService;

    @MockitoBean
    AnimalFarmQueryService queryService;

    private static Animal animal(String tag) {
        return Animal.register(new RegisterAnimalCommand(tag, null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
    }

    private ResultActions putFarm(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/animals/" + animalId + "/farm").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void assignsTheFarmAndReturnsTheAnimalWithIt() throws Exception {
        var animal = animal("MX-1");
        animal.assignToFarm(farmA);
        when(commandService.handle(any(AssignAnimalToFarmCommand.class))).thenReturn(animal);

        putFarm("{\"farmId\":\"" + farmA + "\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tag").value("MX-1"))
                .andExpect(jsonPath("$.farmId").value(farmA.toString()));

        verify(commandService).handle(new AssignAnimalToFarmCommand(animalId, farmA));
    }

    @Test
    void missingFarmIdReturns400WithoutCallingTheService() throws Exception {
        putFarm("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La granja es obligatoria."));

        verifyNoInteractions(commandService);
    }

    @Test
    void nullFarmIdCannotLeaveTheAnimalWithoutAFarm() throws Exception {
        putFarm("{\"farmId\":null}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La granja es obligatoria."));

        verifyNoInteractions(commandService);
    }

    @Test
    void unknownAnimalReturns404() throws Exception {
        when(commandService.handle(any(AssignAnimalToFarmCommand.class))).thenThrow(new AnimalNotFoundException());

        putFarm("{\"farmId\":\"" + farmA + "\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El animal no existe."));
    }

    @Test
    void unknownFarmReturns404() throws Exception {
        when(commandService.handle(any(AssignAnimalToFarmCommand.class))).thenThrow(new FarmNotFoundException());

        putFarm("{\"farmId\":\"" + farmA + "\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La granja no existe."));
    }

    @Test
    void returnsTheFarmHistoryInTheOrderOfTheService() throws Exception {
        var first = Instant.parse("2026-03-01T10:00:00Z");
        var second = Instant.parse("2026-06-01T10:00:00Z");
        when(queryService.handle(new GetAnimalFarmHistoryQuery(animalId))).thenReturn(List.of(
                AnimalFarmAssignment.record(animalId, null, farmA, first),
                AnimalFarmAssignment.record(animalId, farmA, farmB, second)));

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/farm-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animalId").value(animalId.toString()))
                .andExpect(jsonPath("$.assignments.length()").value(2))
                .andExpect(jsonPath("$.assignments[0].fromFarmId").doesNotExist())
                .andExpect(jsonPath("$.assignments[0].toFarmId").value(farmA.toString()))
                .andExpect(jsonPath("$.assignments[1].fromFarmId").value(farmA.toString()))
                .andExpect(jsonPath("$.assignments[1].toFarmId").value(farmB.toString()))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void emptyHistoryReturnsAMessage() throws Exception {
        when(queryService.handle(any(GetAnimalFarmHistoryQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/farm-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignments").isEmpty())
                .andExpect(jsonPath("$.message").value("Sin cambios de granja."));
    }

    @Test
    void historyOfAnUnknownAnimalReturns404() throws Exception {
        when(queryService.handle(any(GetAnimalFarmHistoryQuery.class))).thenThrow(new AnimalNotFoundException());

        mockMvc.perform(get("/api/v1/animals/" + animalId + "/farm-history"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("El animal no existe."));
    }

    @Test
    void listsTheAnimalsOfTheFarmActiveByDefault() throws Exception {
        when(queryService.handle(any(GetAnimalsByFarmQuery.class))).thenReturn(List.of(animal("MX-1"), animal("MX-2")));

        mockMvc.perform(get("/api/v1/farms/" + farmA + "/animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animals.length()").value(2))
                .andExpect(jsonPath("$.animals[0].tag").value("MX-1"))
                .andExpect(jsonPath("$.message").doesNotExist());

        verify(queryService).handle(new GetAnimalsByFarmQuery(farmA, AnimalStatus.ACTIVE));
    }

    @Test
    void passesTheRequestedStatus() throws Exception {
        when(queryService.handle(any(GetAnimalsByFarmQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/farms/" + farmA + "/animals").param("status", "SOLD")).andExpect(status().isOk());

        verify(queryService).handle(new GetAnimalsByFarmQuery(farmA, AnimalStatus.SOLD));
    }

    @Test
    void farmWithoutAnimalsReturnsAMessage() throws Exception {
        when(queryService.handle(any(GetAnimalsByFarmQuery.class))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/farms/" + farmA + "/animals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animals").isEmpty())
                .andExpect(jsonPath("$.message").value("Esta granja no tiene animales."));
    }

    @Test
    void listingAnUnknownFarmReturns404() throws Exception {
        when(queryService.handle(any(GetAnimalsByFarmQuery.class))).thenThrow(new FarmNotFoundException());

        mockMvc.perform(get("/api/v1/farms/" + farmA + "/animals"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La granja no existe."));
    }

    @Test
    void invalidIdsOrStatusReturn400() throws Exception {
        mockMvc.perform(get("/api/v1/farms/no-es-uuid/animals"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: farmId"));
        mockMvc.perform(get("/api/v1/farms/" + farmA + "/animals").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: status"));

        verifyNoInteractions(queryService);
    }
}
