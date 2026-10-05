package com.jamsell.gethics.notifications.interfaces.rest;

import com.jamsell.gethics.notifications.application.internal.commandservices.NotificationPreferenceCommandServiceImpl;
import com.jamsell.gethics.notifications.application.internal.queryservices.NotificationPreferenceQueryServiceImpl;
import com.jamsell.gethics.notifications.domain.model.aggregates.NotificationPreference;
import com.jamsell.gethics.notifications.infrastructure.persistence.jpa.repositories.NotificationPreferenceRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationPreferenceController.class)
@Import({NotificationPreferenceCommandServiceImpl.class, NotificationPreferenceQueryServiceImpl.class})
@WithMockUser
class NotificationPreferenceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationPreferenceRepository repository;

    private final UUID userId = UUID.randomUUID();
    private final String url = "/api/v1/users/" + userId + "/notification-preference";

    @Test
    void getReturnsEnabledTrueWhenUserHasNoStoredPreference() throws Exception {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.pushEnabled").value(true));
    }

    @Test
    void putDisablesAndReturnsTheUpdatedPreference() throws Exception {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(NotificationPreference.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put(url).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pushEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pushEnabled").value(false));
    }

    @Test
    void putWithoutPushEnabledReturns400() throws Exception {
        mockMvc.perform(put(url).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
