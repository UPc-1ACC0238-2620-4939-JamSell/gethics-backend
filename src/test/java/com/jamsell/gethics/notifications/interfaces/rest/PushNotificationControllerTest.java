package com.jamsell.gethics.notifications.interfaces.rest;

import com.jamsell.gethics.notifications.application.internal.commandservices.NotificationDispatchCommandServiceImpl;
import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
import com.jamsell.gethics.shared.application.internal.outboundservices.PushNotificationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PushNotificationController.class)
@Import(NotificationDispatchCommandServiceImpl.class)
@WithMockUser
class PushNotificationControllerTest {

    private static final String URL = "/api/v1/notifications";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationPreferenceRepository preferenceRepository;

    @MockitoBean
    PushNotificationGateway gateway;

    private final UUID userId = UUID.randomUUID();

    @Test
    void sendsAndReturnsSentTrueWhenUserHasNoPreference() throws Exception {
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","category":"SANITARY","title":"Vacuna pendiente","body":"Aftosa vence manana"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.outcome").value("SENT"));
    }

    @Test
    void returnsSentFalseWhenUserDisabledPushNotifications() throws Exception {
        var preference = new NotificationPreference(userId);
        preference.setPushEnabled(false);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","category":"FINANCIAL","title":"Balance","body":"Cerraste el mes en verde"}
                                """.formatted(userId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.outcome").value("SKIPPED_DISABLED"));
    }

    @Test
    void missingTitleReturns400() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"%s","category":"SYSTEM","title":"","body":"Mensaje"}
                                """.formatted(userId)))
                .andExpect(status().isBadRequest());
    }
}
