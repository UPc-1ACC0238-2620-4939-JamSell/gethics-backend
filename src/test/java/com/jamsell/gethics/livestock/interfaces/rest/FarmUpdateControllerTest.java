package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateFarmNameException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateFarmCommand;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** US-32 por HTTP: PUT /api/v1/farms/{id}. */
@WebMvcTest(FarmController.class)
@WithMockUser
class FarmUpdateControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    FarmCommandService commandService;
    @MockitoBean
    FarmQueryService queryService;

    private final UUID farmId = UUID.randomUUID();

    private org.springframework.test.web.servlet.ResultActions edit(String body) throws Exception {
        return mockMvc.perform(put("/api/v1/farms/" + farmId).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void escenario1ReturnsTheUpdatedFarm() throws Exception {
        var farm = Farm.register(new RegisterFarmCommand(UUID.randomUUID(), "Fundo Norte", "Huancayo",
                new BigDecimal("20")));
        ReflectionTestUtils.setField(farm, "id", farmId);
        when(commandService.handle(new UpdateFarmCommand(farmId, "Fundo Norte", "Huancayo", new BigDecimal("20"))))
                .thenReturn(farm);

        edit("{\"name\":\"Fundo Norte\",\"location\":\"Huancayo\",\"sizeHectares\":20}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(farmId.toString()))
                .andExpect(jsonPath("$.name").value("Fundo Norte"))
                .andExpect(jsonPath("$.location").value("Huancayo"))
                .andExpect(jsonPath("$.sizeHectares").value(20));
    }

    @Test
    void escenario2ADuplicatedNameIs409WithAWarningMessage() throws Exception {
        when(commandService.handle(any(UpdateFarmCommand.class))).thenThrow(new DuplicateFarmNameException("Fundo Norte"));

        edit("{\"name\":\"Fundo Norte\",\"location\":\"Huancayo\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya tienes una granja llamada Fundo Norte."));
    }

    @Test
    void anUnknownFarmIs404() throws Exception {
        when(commandService.handle(any(UpdateFarmCommand.class))).thenThrow(new FarmNotFoundException());

        edit("{\"name\":\"X\",\"location\":\"Y\"}").andExpect(status().isNotFound());
    }

    @Test
    void blankNameOrLocationOrNonPositiveSizeIs400WithoutCallingTheService() throws Exception {
        edit("{\"name\":\" \",\"location\":\"Y\"}").andExpect(status().isBadRequest());
        edit("{\"name\":\"X\",\"location\":\"\"}").andExpect(status().isBadRequest());
        edit("{\"name\":\"X\",\"location\":\"Y\",\"sizeHectares\":0}").andExpect(status().isBadRequest());

        verifyNoInteractions(commandService);
    }
}
