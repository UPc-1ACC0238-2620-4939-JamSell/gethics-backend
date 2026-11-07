package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnimalController.class)
@WithMockUser
class AnimalControllerTest {

    private static final String URL = "/api/v1/animals";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnimalCommandService commandService;

    @MockitoBean
    AnimalQueryService queryService;

    private static Animal animal(String tag, String name, String breed) {
        return Animal.register(new RegisterAnimalCommand(tag, name, breed, null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
    }

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String minimalBody(String weight) {
        return "{\"tag\":\"A-1\",\"breed\":\"Jersey\",\"birthDate\":\"2024-01-01\",\"initialWeightKg\":" + weight + "}";
    }

    @Test
    void registersAnimalAndReturns201() throws Exception {
        var birthDate = LocalDate.now().minusYears(1);
        var command = new RegisterAnimalCommand("mx-00123", "Luna", "Holstein", AnimalSex.FEMALE, birthDate,
                new BigDecimal("420.5"), null, null);
        when(commandService.handle(any(RegisterAnimalCommand.class)))
                .thenReturn(Animal.register(command, LocalDate.now()));

        postJson("{\"tag\":\"mx-00123\",\"name\":\"Luna\",\"breed\":\"Holstein\",\"sex\":\"FEMALE\","
                + "\"birthDate\":\"" + birthDate + "\",\"initialWeightKg\":420.5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tag").value("MX-00123"))
                .andExpect(jsonPath("$.qrCode").value(org.hamcrest.Matchers.startsWith("GTH-")))
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.breed").value("Holstein"))
                .andExpect(jsonPath("$.sex").value("FEMALE"))
                .andExpect(jsonPath("$.birthDate").value(birthDate.toString()))
                .andExpect(jsonPath("$.initialWeightKg").value(420.5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(commandService).handle(command);
    }

    @Test
    void missingRequiredFieldsReturn400WithoutCallingService() throws Exception {
        postJson("{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(commandService);
    }

    @Test
    void nonPositiveWeightReturns400() throws Exception {
        postJson(minimalBody("0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El peso inicial debe ser mayor a 0."));

        verifyNoInteractions(commandService);
    }

    @Test
    void futureBirthDateReturns400WithDomainMessage() throws Exception {
        when(commandService.handle(any(RegisterAnimalCommand.class))).thenThrow(new FutureBirthDateException());

        postJson(minimalBody("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de nacimiento no puede ser posterior a hoy."));
    }

    @Test
    void duplicateTagReturns409() throws Exception {
        when(commandService.handle(any(RegisterAnimalCommand.class))).thenThrow(new DuplicateAnimalTagException("A-1"));

        postJson(minimalBody("null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un animal con el arete A-1."));
    }

    // ---------------- US-06 ----------------

    @Test
    void listsAnimalsWithoutMessage() throws Exception {
        when(queryService.handle(any(GetAnimalsQuery.class)))
                .thenReturn(List.of(animal("MX-1", "Luna", "Holstein"), animal("MX-2", null, "Jersey")));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animals.length()").value(2))
                .andExpect(jsonPath("$.animals[0].tag").value("MX-1"))
                .andExpect(jsonPath("$.animals[0].name").value("Luna"))
                .andExpect(jsonPath("$.animals[1].breed").value("Jersey"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void withoutParametersSearchesActiveAnimalsOnly() throws Exception {
        when(queryService.handle(any(GetAnimalsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(URL)).andExpect(status().isOk());

        verify(queryService).handle(new GetAnimalsQuery(null, AnimalStatus.ACTIVE));
    }

    @Test
    void passesSearchAndStatusToTheQuery() throws Exception {
        when(queryService.handle(any(GetAnimalsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("search", "  holstein ").param("status", "SOLD")).andExpect(status().isOk());

        verify(queryService).handle(new GetAnimalsQuery("holstein", AnimalStatus.SOLD));
    }

    @Test
    void searchWithoutMatchesReturnsSinResultados() throws Exception {
        when(queryService.handle(any(GetAnimalsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("search", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animals").isEmpty())
                .andExpect(jsonPath("$.message").value("Sin resultados."));
    }

    @Test
    void emptyInventoryReturnsNoAnimalsMessage() throws Exception {
        when(queryService.handle(any(GetAnimalsQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No hay animales registrados."));
    }

    @Test
    void unknownStatusReturns400() throws Exception {
        mockMvc.perform(get(URL).param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: status"));

        verifyNoInteractions(queryService);
    }

    @Test
    void tooLongSearchReturns400() throws Exception {
        mockMvc.perform(get(URL).param("search", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El criterio de busqueda no puede superar 100 caracteres."));

        verifyNoInteractions(queryService);
    }
}
