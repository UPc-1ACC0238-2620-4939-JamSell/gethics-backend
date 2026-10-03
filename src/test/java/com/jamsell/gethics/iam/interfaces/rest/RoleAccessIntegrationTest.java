package com.jamsell.gethics.iam.interfaces.rest;

import com.jamsell.gethics.iam.infrastructure.security.OnlyGanadero;
import com.jamsell.gethics.iam.infrastructure.security.OnlyVeterinario;
import com.jamsell.gethics.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(RoleAccessIntegrationTest.RoleProbeController.class)
class RoleAccessIntegrationTest extends IntegrationTestBase {

    @RestController
    public static class RoleProbeController {

        @OnlyVeterinario
        @GetMapping("/test/solo-veterinario")
        public String veterinarioOnly() {
            return "ok";
        }

        @OnlyGanadero
        @GetMapping("/test/solo-ganadero")
        public String ganaderoOnly() {
            return "ok";
        }
    }

    @Test
    void veterinarioRoute_withVeterinario_returns200() throws Exception {
        var token = registerAndLogin("VETERINARIO");

        mockMvc.perform(get("/test/solo-veterinario").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void veterinarioRoute_withGanadero_returns403() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(get("/test/solo-veterinario").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void ganaderoRoute_withGanadero_returns200() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(get("/test/solo-ganadero").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void ganaderoRoute_withVeterinario_returns403() throws Exception {
        var token = registerAndLogin("VETERINARIO");

        mockMvc.perform(get("/test/solo-ganadero").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void roleRoute_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/test/solo-ganadero"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
