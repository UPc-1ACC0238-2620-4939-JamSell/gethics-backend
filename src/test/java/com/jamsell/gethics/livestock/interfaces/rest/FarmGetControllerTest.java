package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmByIdQuery;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /api/v1/farms/{id}: la ficha de una granja, para mostrar la granja asignada a un animal. */
@WebMvcTest(FarmController.class)
@WithMockUser
class FarmGetControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    FarmCommandService commandService;
    @MockitoBean
    FarmQueryService queryService;

    private final UUID farmId = UUID.randomUUID();

    @Test
    void returnsTheFarmWithItsData() throws Exception {
        var ownerId = UUID.randomUUID();
        var farm = Farm.register(new RegisterFarmCommand(ownerId, "Fundo Sur", "Jauja, Junin", new BigDecimal("12.5")));
        ReflectionTestUtils.setField(farm, "id", farmId);
        when(queryService.handle(new GetFarmByIdQuery(farmId))).thenReturn(Optional.of(farm));

        mockMvc.perform(get("/api/v1/farms/" + farmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(farmId.toString()))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.name").value("Fundo Sur"))
                .andExpect(jsonPath("$.location").value("Jauja, Junin"))
                .andExpect(jsonPath("$.sizeHectares").value(12.5))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void anUnknownFarmIs404WithAClearMessage() throws Exception {
        when(queryService.handle(new GetFarmByIdQuery(farmId))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/farms/" + farmId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La granja no existe."));
    }

    @Test
    void anInvalidIdIs400() throws Exception {
        mockMvc.perform(get("/api/v1/farms/no-es-un-uuid"))
                .andExpect(status().isBadRequest());
    }
}
