package com.jamsell.gethics.iam.interfaces.rest;

import com.jamsell.gethics.iam.application.internal.outboundservices.mail.PasswordResetMailSender;
import com.jamsell.gethics.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PasswordResetIntegrationTest extends IntegrationTestBase {

    @MockitoBean
    private PasswordResetMailSender mailSender;

    @Test
    void forgotPassword_withKnownEmail_sendsLinkAndAllowsResetOnce() throws Exception {
        var email = uniqueEmail();
        register(email, "GANADERO");

        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isOk());

        var linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendPasswordResetLink(eq(email), anyString(), linkCaptor.capture(), anyLong());
        var link = linkCaptor.getValue();
        var token = link.substring(link.indexOf("token=") + "token=".length());

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(token, "nuevaClave123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        login(email, "nuevaClave123");
        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody(token, "otraClave12345")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void forgotPassword_withUnknownEmail_returns404AndSendsNothing() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\"}".formatted(uniqueEmail())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_FOUND"));

        verify(mailSender, never()).sendPasswordResetLink(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void forgotPassword_withInvalidEmailFormat_returns400() throws Exception {
        mockMvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void resetPassword_withUnknownToken_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("token-que-no-existe", "nuevaClave123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void resetPassword_withShortPassword_returns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody("cualquier-token", "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private static String resetBody(String token, String newPassword) {
        return "{\"token\":\"%s\",\"newPassword\":\"%s\"}".formatted(token, newPassword);
    }
}
