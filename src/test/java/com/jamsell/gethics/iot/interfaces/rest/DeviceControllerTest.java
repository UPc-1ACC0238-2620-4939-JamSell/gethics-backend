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
import com.jamsell.gethics.iot.domain.model.queries.GetDeviceReadingsQuery;
import com.jamsell.gethics.iot.domain.model.queries.GetDevicesByOwnerQuery;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
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

    // --- US-37: lecturas de un dispositivo ---

    @Test
    void listReadingsReturns200WithThePageOfReadings() throws Exception {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var newest = device.registerReading("temperature", new BigDecimal("38.2"), NOW.plusHours(2), NOW.plusHours(2));
        var older = device.registerReading("temperature", new BigDecimal("36.5"), NOW.plusHours(1), NOW.plusHours(1));
        var deviceId = UUID.randomUUID();
        when(queryService.handle(any(GetDeviceReadingsQuery.class)))
                .thenReturn(new SensorReadingPage(List.of(newest, older), 0, 20, 2, 1));

        mockMvc.perform(get(URL + "/" + deviceId + "/readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].metric").value("temperature"))
                .andExpect(jsonPath("$.content[0].value").value(38.2))
                .andExpect(jsonPath("$.content[0].recordedAt").value("2042-06-07T10:00:00"))
                .andExpect(jsonPath("$.content[1].value").value(36.5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        // Sin page ni size se usan los valores por defecto (0 y 20).
        verify(queryService).handle(new GetDeviceReadingsQuery(deviceId, 0, 20));
    }

    @Test
    void listReadingsPassesThePageAndSizeRequested() throws Exception {
        var deviceId = UUID.randomUUID();
        when(queryService.handle(any(GetDeviceReadingsQuery.class)))
                .thenReturn(new SensorReadingPage(List.of(), 2, 5, 11, 3));

        mockMvc.perform(get(URL + "/" + deviceId + "/readings").param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(queryService).handle(new GetDeviceReadingsQuery(deviceId, 2, 5));
    }

    @Test
    void listReadingsOfADeviceWithoutReadingsReturns200WithAnEmptyPage() throws Exception {
        when(queryService.handle(any(GetDeviceReadingsQuery.class)))
                .thenReturn(new SensorReadingPage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(URL + "/" + UUID.randomUUID() + "/readings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listReadingsOfAnUnknownDeviceReturns404WithAMessage() throws Exception {
        var deviceId = UUID.randomUUID();
        when(queryService.handle(any(GetDeviceReadingsQuery.class))).thenThrow(new DeviceNotFoundException(deviceId));

        mockMvc.perform(get(URL + "/" + deviceId + "/readings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No se encontro el dispositivo " + deviceId + "."));
    }

    @Test
    void listReadingsWithASizeAboveTheMaximumReturns400WithoutCallingTheService() throws Exception {
        mockMvc.perform(get(URL + "/" + UUID.randomUUID() + "/readings").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parametro size debe estar entre 1 y 100."));

        verifyNoInteractions(queryService);
    }

    @Test
    void listReadingsWithANegativePageReturns400WithoutCallingTheService() throws Exception {
        mockMvc.perform(get(URL + "/" + UUID.randomUUID() + "/readings").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El parametro page no puede ser negativo."));

        verifyNoInteractions(queryService);
    }

    @Test
    void listReadingsWithAnIdThatIsNotAUuidReturns400() throws Exception {
        mockMvc.perform(get(URL + "/MX-DEV-1/readings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(queryService);
    }
}
