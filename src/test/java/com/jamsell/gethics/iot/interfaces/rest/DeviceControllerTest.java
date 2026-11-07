package com.jamsell.gethics.iot.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamsell.gethics.iot.domain.exceptions.DeviceNotFoundException;
import com.jamsell.gethics.iot.domain.exceptions.DuplicateDeviceCodeException;
import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.commands.RegisterSensorReadingCommand;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.services.DeviceCommandService;
import com.jamsell.gethics.iot.domain.services.DeviceQueryService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(DeviceController.class)
@WithMockUser
class DeviceControllerTest {

    private static final String URL = "/api/v1/devices";
    private static final LocalDateTime NOW = LocalDateTime.of(2042, 6, 7, 8, 0);

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    DeviceCommandService commandService;

    @MockitoBean
    DeviceQueryService queryService;

    private ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    // --- US-23 Escenario 1: vinculacion de dispositivo ---

    @Test
    void linkReturns201WithTheLinkedDevice() throws Exception {
        var ownerId = UUID.randomUUID();
        var device = Device.link(new LinkDeviceCommand(ownerId, "MX-DEV-1"), NOW);
        when(commandService.handle(any(LinkDeviceCommand.class))).thenReturn(device);

        postJson(URL + "/link", "{\"ownerId\":\"" + ownerId + "\",\"code\":\"mx-dev-1\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("MX-DEV-1"))
                .andExpect(jsonPath("$.status").value("CONNECTED"))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()));

        verify(commandService).handle(new LinkDeviceCommand(ownerId, "mx-dev-1"));
    }

    @Test
    void linkWithoutOwnerIdReturns400WithoutCallingTheService() throws Exception {
        postJson(URL + "/link", "{\"code\":\"MX-DEV-1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(commandService);
    }

    @Test
    void linkWithDuplicateCodeReturns409() throws Exception {
        when(commandService.handle(any(LinkDeviceCommand.class)))
                .thenThrow(new DuplicateDeviceCodeException("MX-DEV-1"));

        postJson(URL + "/link", "{\"ownerId\":\"" + UUID.randomUUID() + "\",\"code\":\"MX-DEV-1\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un dispositivo vinculado con el codigo MX-DEV-1."));
    }

    // --- Consulta de estado ---

    @Test
    void listDevicesReturns200WithTheOwnersDevices() throws Exception {
        var ownerId = UUID.randomUUID();
        var device = Device.link(new LinkDeviceCommand(ownerId, "MX-DEV-1"), NOW);
        when(queryService.handle(new GetDevicesByOwnerQuery(ownerId))).thenReturn(List.of(device));

        mockMvc.perform(get(URL).param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MX-DEV-1"))
                .andExpect(jsonPath("$[0].status").value("CONNECTED"));
    }

    // --- Ingesta de lecturas ---

    @Test
    void registerReadingReturns201() throws Exception {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var reading = device.registerReading("temperature", new BigDecimal("36.5"), null, NOW.plusMinutes(1));
        when(commandService.handle(any(RegisterSensorReadingCommand.class))).thenReturn(reading);

        postJson(URL + "/MX-DEV-1/readings", "{\"metric\":\"temperature\",\"value\":36.5}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metric").value("temperature"))
                .andExpect(jsonPath("$.value").value(36.5));

        verify(commandService)
                .handle(new RegisterSensorReadingCommand("MX-DEV-1", "temperature", new BigDecimal("36.5"), null));
    }

    @Test
    void registerReadingForAnUnknownDeviceReturns404() throws Exception {
        when(commandService.handle(any(RegisterSensorReadingCommand.class)))
                .thenThrow(new DeviceNotFoundException("MX-DEV-1"));

        postJson(URL + "/MX-DEV-1/readings", "{\"metric\":\"temperature\",\"value\":36.5}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message")
                        .value("No se encontro un dispositivo vinculado con el codigo MX-DEV-1."));
    }

    @Test
    void registerReadingWithoutMetricReturns400WithoutCallingTheService() throws Exception {
        postJson(URL + "/MX-DEV-1/readings", "{\"value\":36.5}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(commandService);
    }
}
