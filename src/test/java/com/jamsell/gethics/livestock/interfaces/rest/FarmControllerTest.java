package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateFarmNameException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FarmController.class)
@WithMockUser
class FarmControllerTest {

    private static final String URL = "/api/v1/farms";

    private final UUID ownerId = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FarmCommandService commandService;

    @MockitoBean
    FarmQueryService queryService;

    private ResultActions postJson(String body) throws Exception {
        return mockMvc.perform(post(URL).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String body(String name, String location, String size) {
        return "{\"ownerId\":\"" + ownerId + "\",\"name\":" + name + ",\"location\":" + location
                + ",\"sizeHectares\":" + size + "}";
    }

    private Farm farm(String name) {
        return Farm.register(new RegisterFarmCommand(ownerId, name, "Jauja, Junin", new BigDecimal("12.5")));
    }

    @Test
    void registersFarmAndReturns201() throws Exception {
        var command = new RegisterFarmCommand(ownerId, "Fundo Sur", "Jauja, Junin", new BigDecimal("12.5"));
        when(commandService.handle(any(RegisterFarmCommand.class))).thenReturn(Farm.register(command));

        postJson(body("\"Fundo Sur\"", "\"Jauja, Junin\"", "12.5"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.name").value("Fundo Sur"))
                .andExpect(jsonPath("$.location").value("Jauja, Junin"))
                .andExpect(jsonPath("$.sizeHectares").value(12.5))
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
    void nonPositiveSizeReturns400() throws Exception {
        postJson(body("\"Fundo\"", "\"Lima\"", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El tamano de la granja debe ser mayor a 0."));

        verifyNoInteractions(commandService);
    }

    @Test
    void duplicateNameReturns409() throws Exception {
        when(commandService.handle(any(RegisterFarmCommand.class))).thenThrow(new DuplicateFarmNameException("Fundo Sur"));

        postJson(body("\"Fundo Sur\"", "\"Lima\"", "null"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya tienes una granja llamada Fundo Sur."));
    }

    @Test
    void listsTheFarmsOfTheOwner() throws Exception {
        when(queryService.handle(new GetFarmsByOwnerQuery(ownerId))).thenReturn(List.of(farm("Fundo Norte"), farm("Fundo Sur")));

        mockMvc.perform(get(URL).param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farms.length()").value(2))
                .andExpect(jsonPath("$.farms[0].name").value("Fundo Norte"))
                .andExpect(jsonPath("$.farms[1].name").value("Fundo Sur"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void ownerWithoutFarmsGetsAMessage() throws Exception {
        when(queryService.handle(any(GetFarmsByOwnerQuery.class))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.farms").isEmpty())
                .andExpect(jsonPath("$.message").value("No hay granjas registradas."));
    }

    @Test
    void missingOwnerIdReturns400() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro obligatorio ausente: ownerId"));

        verifyNoInteractions(queryService);
    }

    @Test
    void invalidOwnerIdReturns400() throws Exception {
        mockMvc.perform(get(URL).param("ownerId", "no-es-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: ownerId"));

        verifyNoInteractions(queryService);
    }
}
