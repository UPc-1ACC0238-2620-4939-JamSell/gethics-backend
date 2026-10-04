package com.jamsell.gethics.sanitary;

import com.jamsell.gethics.sanitary.application.internal.outboundservices.NotificationService;
import com.jamsell.gethics.sanitary.application.internal.outboundservices.VaccinationReminderNotification;
import com.jamsell.gethics.sanitary.interfaces.scheduling.VaccinationReminderJob;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * US-13 de punta a punta con la aplicacion completa (REST + servicios + PostgreSQL + job): los dos escenarios de Trello.
 * Solo se reemplazan el Clock (fijo) y el puerto de notificacion: el push real esta bloqueado externamente, asi que se
 * verifica que el sistema lo despacha al puerto, no que llegue a un dispositivo. Cada test hace rollback.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VaccinationReminderEndToEndTest {

    // 08:00 en Lima: "hoy" = 2042-06-07, la vacuna a 3 dias es la del 2042-06-10.
    private static final Instant NOW = Instant.parse("2042-06-07T13:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final LocalDate IN_THREE_DAYS = TODAY.plusDays(3);

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneId.of("America/Lima"));
        }
    }

    @Autowired
    MockMvc mockMvc;
    @Autowired
    VaccinationReminderJob job;
    @MockitoBean
    NotificationService notifications;

    private final UUID animalId = UUID.randomUUID();

    private String eventsUrl() {
        return "/api/v1/animals/" + animalId + "/sanitary-events";
    }

    private UUID scheduleVaccination(LocalDate date) throws Exception {
        var body = mockMvc.perform(post(eventsUrl() + "/scheduled").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"VACCINATION\",\"scheduledDate\":\"" + date + "\",\"description\":\"Aftosa\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SCHEDULED"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private void verifyAlerts(UUID eventId, int times) {
        verify(notifications, times(times)).sendVaccinationReminder(
                argThat((VaccinationReminderNotification n) -> n.sanitaryEventId().equals(eventId)));
    }

    /** Escenario 1: dada una vacuna programada, cuando faltan 3 dias, el sistema envia la alerta (una sola vez). */
    @Test
    void scenario1_alertIsSentThreeDaysBeforeTheScheduledVaccination() throws Exception {
        var dueInThreeDays = scheduleVaccination(IN_THREE_DAYS);
        var dueInFourDays = scheduleVaccination(TODAY.plusDays(4));

        job.run();

        verify(notifications).sendVaccinationReminder(
                new VaccinationReminderNotification(dueInThreeDays, animalId, IN_THREE_DAYS, "Aftosa"));
        verifyAlerts(dueInFourDays, 0);

        job.run();

        verifyAlerts(dueInThreeDays, 1);
    }

    /** Escenario 2: dada una vacuna ya registrada como aplicada, cuando llega el recordatorio, no se envia alerta. */
    @Test
    void scenario2_noAlertIsSentOnceTheVaccinationWasRegisteredAsApplied() throws Exception {
        var eventId = scheduleVaccination(IN_THREE_DAYS);

        mockMvc.perform(post(eventsUrl() + "/" + eventId + "/complete").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"occurredAt\":\"" + TODAY.atTime(7, 30, 15) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.scheduledDate").value(IN_THREE_DAYS.toString()));

        job.run();

        verifyAlerts(eventId, 0);
        // Mismo evento, sin segunda fila: el historial (US-14) tiene una unica entrada, ya aplicada.
        mockMvc.perform(get("/api/v1/animals/" + animalId + "/clinical-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events.length()").value(1))
                .andExpect(jsonPath("$.events[0].id").value(eventId.toString()))
                .andExpect(jsonPath("$.events[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.events[0].occurredAt").value(TODAY.atTime(7, 30, 15).toString()))
                .andExpect(jsonPath("$.events[0].scheduledDate").value(IN_THREE_DAYS.toString()));
    }
}
