package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import com.jamsell.gethics.livestock.domain.services.AnimalCommandService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
}
