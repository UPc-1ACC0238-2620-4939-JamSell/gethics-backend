package com.jamsell.gethics.subscription.interfaces.rest;

import com.jamsell.gethics.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubscriptionIntegrationTest extends IntegrationTestBase {

    @Test
    void getPlans_returnsThreePlansOrderedByPrice() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(get("/plans").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].code").value("GRATUITO"))
                .andExpect(jsonPath("$[1].code").value("BASICO"))
                .andExpect(jsonPath("$[2].code").value("PREMIUM"))
                .andExpect(jsonPath("$[2].features").isArray());
    }

    @Test
    void getPlans_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/plans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newUser_hasFreePlanByDefault() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(get("/subscriptions/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.plan.code").value("GRATUITO"));
    }

    @Test
    void subscribe_withApprovedPayment_activatesPlan() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(subscribe(token, "BASICO", "tok_approved"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan.code").value("BASICO"))
                .andExpect(jsonPath("$.endsAt").isNotEmpty());

        mockMvc.perform(get("/subscriptions/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.plan.code").value("BASICO"));
    }

    @Test
    void subscribe_withRejectedPayment_returns402AndKeepsFreePlan() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(subscribe(token, "PREMIUM", "tok_declined"))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_REJECTED"));

        mockMvc.perform(get("/subscriptions/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.plan.code").value("GRATUITO"));
    }

    @Test
    void subscribe_toSamePlanTwice_returns409() throws Exception {
        var token = registerAndLogin("GANADERO");
        mockMvc.perform(subscribe(token, "BASICO", "tok_approved")).andExpect(status().isCreated());

        mockMvc.perform(subscribe(token, "BASICO", "tok_approved"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_CONFLICT"));
    }

    @Test
    void subscribe_upgradingToAnotherPlan_replacesCurrentOne() throws Exception {
        var token = registerAndLogin("GANADERO");
        mockMvc.perform(subscribe(token, "BASICO", "tok_approved")).andExpect(status().isCreated());

        mockMvc.perform(subscribe(token, "PREMIUM", "tok_approved")).andExpect(status().isCreated());

        mockMvc.perform(get("/subscriptions/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.plan.code").value("PREMIUM"));
    }

    @Test
    void subscribe_toFreePlan_returns422() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(subscribe(token, "GRATUITO", "tok_approved"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void subscribe_toUnknownPlan_returns404() throws Exception {
        var token = registerAndLogin("GANADERO");

        mockMvc.perform(subscribe(token, "INEXISTENTE", "tok_approved"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
    }

    @Test
    void subscribe_withVeterinarioRole_returns403() throws Exception {
        var token = registerAndLogin("VETERINARIO");

        mockMvc.perform(subscribe(token, "BASICO", "tok_approved"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private static MockHttpServletRequestBuilder subscribe(
            String token,
            String planCode,
            String paymentToken
    ) {
        return post("/subscriptions")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"planCode\":\"%s\",\"paymentToken\":\"%s\"}".formatted(planCode, paymentToken));
    }
}
