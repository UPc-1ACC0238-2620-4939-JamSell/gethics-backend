package com.jamsell.gethics.analytics.interfaces.rest;

import com.jamsell.gethics.analytics.domain.model.aggregates.Alert;
import com.jamsell.gethics.analytics.domain.model.aggregates.Analytics;
import com.jamsell.gethics.analytics.domain.model.queries.GetAlertsByOwnerQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.RiskLevel;
import com.jamsell.gethics.analytics.domain.model.valueobjects.TrendType;
import com.jamsell.gethics.analytics.domain.services.AlertQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@WithMockUser
class AlertControllerTest {

    private static final String URL = "/api/v1/alerts";

    private final UUID ownerId = UUID.randomUUID();

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AlertQueryService queryService;

    private Alert alert(TrendType type, RiskLevel riskLevel, String message) {
        var trend = new Analytics(ownerId, RiskLevel.LOW).registerTrend(type, riskLevel, "descripcion",
                Instant.parse("2026-10-01T10:00:00Z"), RiskLevel.LOW);
        ReflectionTestUtils.setField(trend, "id", UUID.randomUUID());
        var alert = Alert.create(ownerId, trend, message);
        ReflectionTestUtils.setField(alert, "id", UUID.randomUUID());
        return alert;
    }

    @Test
    void listAlertsReturns200WithTheAlertsInTheOrderGivenByTheQueryService() throws Exception {
        var newest = alert(TrendType.SANITARY, RiskLevel.HIGH, "Aumento de casos sanitarios");
        var oldest = alert(TrendType.FINANCIAL, RiskLevel.CRITICAL, "Caida de ingresos");
        when(queryService.handle(new GetAlertsByOwnerQuery(ownerId))).thenReturn(List.of(newest, oldest));

        mockMvc.perform(get(URL).param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts.length()").value(2))
                .andExpect(jsonPath("$.alerts[0].id").value(newest.getId().toString()))
                .andExpect(jsonPath("$.alerts[0].trendId").value(newest.getTrendId().toString()))
                .andExpect(jsonPath("$.alerts[0].trendType").value("SANITARY"))
                .andExpect(jsonPath("$.alerts[0].riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.alerts[0].message").value("Aumento de casos sanitarios"))
                .andExpect(jsonPath("$.alerts[0].status").value("PENDING"))
                .andExpect(jsonPath("$.alerts[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.alerts[1].id").value(oldest.getId().toString()))
                .andExpect(jsonPath("$.alerts[1].trendType").value("FINANCIAL"))
                .andExpect(jsonPath("$.message").value(nullValue()));
    }

    @Test
    void listAlertsReturns200WithTheEmptyStateMessageWhenThereAreNone() throws Exception {
        when(queryService.handle(new GetAlertsByOwnerQuery(ownerId))).thenReturn(List.of());

        mockMvc.perform(get(URL).param("ownerId", ownerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts").isEmpty())
                .andExpect(jsonPath("$.message").value("No hay alertas registradas."));
    }

    @Test
    void missingOwnerIdReturns400() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro obligatorio ausente: ownerId"));

        verifyNoInteractions(queryService);
    }

    @Test
    void invalidOwnerIdReturns400() throws Exception {
        mockMvc.perform(get(URL).param("ownerId", "no-es-un-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parametro invalido: ownerId"));

        verifyNoInteractions(queryService);
    }
}
