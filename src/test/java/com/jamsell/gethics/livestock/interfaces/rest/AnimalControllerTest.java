package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putJson(String id, String body) throws Exception {
        return mockMvc.perform(put(URL + "/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String minimalUpdateBody() {
        return "{\"breed\":\"Jersey\",\"birthDate\":\"2024-01-01\"}";
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

    // --- US-07: edicion de animal ---

    @Test
    void getAnimalReturns200WithTheAnimal() throws Exception {
        // Animal.register() no asigna id (eso lo hace JPA al guardar), asi que la URL usa un id de ruta aparte;
        // el mock del query service devuelve el animal sin importar que id se le pida.
        var registerCommand = new RegisterAnimalCommand("mx-00123", "Luna", "Holstein", AnimalSex.FEMALE,
                LocalDate.now().minusYears(1), new BigDecimal("420.5"), null, null);
        var animal = Animal.register(registerCommand, LocalDate.now());
        when(queryService.handle(any())).thenReturn(animal);

        mockMvc.perform(get(URL + "/" + UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.breed").value("Holstein"));
    }

    @Test
    void getUnknownAnimalReturns404() throws Exception {
        var animalId = UUID.randomUUID();
        when(queryService.handle(any())).thenThrow(new AnimalNotFoundException(animalId));

        mockMvc.perform(get(URL + "/" + animalId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontro el animal " + animalId + "."));
    }

    @Test
    void updateAnimalReturns200WithUpdatedData() throws Exception {
        var animalId = UUID.randomUUID();
        var registerCommand = new RegisterAnimalCommand("A-1", "Luna", "Jersey", null,
                LocalDate.now().minusYears(1), null, null, null);
        var updated = Animal.register(registerCommand, LocalDate.now());
        when(commandService.handle(any(UpdateAnimalCommand.class))).thenReturn(updated);

        putJson(animalId.toString(), "{\"name\":\"Luna\",\"breed\":\"Jersey\",\"birthDate\":\"2024-01-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Luna"))
                .andExpect(jsonPath("$.breed").value("Jersey"));

        verify(commandService).handle(new UpdateAnimalCommand(animalId, "Luna", "Jersey", null,
                LocalDate.parse("2024-01-01"), null, null, null));
    }

    @Test
    void updateMissingBreedReturns400WithoutCallingService() throws Exception {
        putJson(UUID.randomUUID().toString(), "{\"birthDate\":\"2024-01-01\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(commandService);
    }

    @Test
    void updateUnknownAnimalReturns404() throws Exception {
        var animalId = UUID.randomUUID();
        when(commandService.handle(any(UpdateAnimalCommand.class))).thenThrow(new AnimalNotFoundException(animalId));

        putJson(animalId.toString(), minimalUpdateBody())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontro el animal " + animalId + "."));
    }

    @Test
    void updateFutureBirthDateReturns400WithDomainMessage() throws Exception {
        when(commandService.handle(any(UpdateAnimalCommand.class))).thenThrow(new FutureBirthDateException());

        putJson(UUID.randomUUID().toString(), minimalUpdateBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La fecha de nacimiento no puede ser posterior a hoy."));
    }
}
