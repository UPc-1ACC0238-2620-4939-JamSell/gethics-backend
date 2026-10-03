package com.jamsell.gethics.iam.interfaces.rest;

import com.jamsell.gethics.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserProfileIntegrationTest extends IntegrationTestBase {

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02, 0x03
    };

    @Test
    void getProfile_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getProfile_returnsOwnData() throws Exception {
        var email = uniqueEmail();
        register(email, "VETERINARIO");
        var token = login(email, PASSWORD);

        mockMvc.perform(get("/users/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("VETERINARIO"))
                .andExpect(jsonPath("$.photoUrl").doesNotExist());
    }

    @Test
    void updateProfile_withValidData_returnsConfirmationAndPersists() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(put("/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Maria\",\"phone\":\"+51 987 654 321\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"))
                .andExpect(jsonPath("$.user.phone").value("+51987654321"));

        mockMvc.perform(get("/users/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.name").value("Ana Maria"))
                .andExpect(jsonPath("$.phone").value("+51987654321"));
    }

    @Test
    void updateProfile_withInvalidPhone_returns400() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(put("/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Maria\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updateProfile_withBlankName_returns400() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(put("/users/me").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updatePhoto_withPng_storesAndServesIt() throws Exception {
        var token = registerAndLogin("GANADERO");
        var file = new MockMultipartFile("file", "foto.png", "image/png", PNG);

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/photo").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.photoUrl").value("/users/me/photo"));

        mockMvc.perform(get("/users/me/photo").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void updatePhoto_withNonImageFile_returns400() throws Exception {
        var token = registerAndLogin("GANADERO");
        var file = new MockMultipartFile("file", "nota.png", "image/png", "no soy imagen".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/photo").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void updatePhoto_overTwoMegabytes_returns400() throws Exception {
        var token = registerAndLogin("GANADERO");
        var big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        var file = new MockMultipartFile("file", "grande.png", "image/png", big);

        mockMvc.perform(multipart(HttpMethod.PUT, "/users/me/photo").file(file).header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void getPhoto_withoutUploadedPhoto_returns404() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(get("/users/me/photo").header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PHOTO_NOT_FOUND"));
    }
}
