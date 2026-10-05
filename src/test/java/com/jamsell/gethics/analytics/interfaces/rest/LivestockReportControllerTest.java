package com.jamsell.gethics.analytics.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LivestockReportController.class)
@WithMockUser
class LivestockReportControllerTest {

    private static final String URL = "/api/v1/reports";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LivestockReportQueryService queryService;

    @Test
    void getReportReturns200WithTheFinanceFiguresAndTheLimitedDataNotices() throws Exception {
        var ownerId = UUID.randomUUID();
        var report = new LivestockReport(ownerId, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                FinanceReportSection.of(new BigDecimal("500.00"), new BigDecimal("120.00")),
                ReportSection.limited("Los datos aun son limitados para un analisis completo."),
                ReportSection.limited("Los datos aun son limitados para un analisis completo."));
        when(queryService.handle(any(GetLivestockReportQuery.class))).thenReturn(report);

        mockMvc.perform(get(URL)
                        .param("ownerId", ownerId.toString())
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.finance.limitedData").value(false))
                .andExpect(jsonPath("$.finance.totalIncome").value(500.00))
                .andExpect(jsonPath("$.finance.totalExpense").value(120.00))
                .andExpect(jsonPath("$.finance.netResult").value(380.00))
                .andExpect(jsonPath("$.health.limitedData").value(true))
                .andExpect(jsonPath("$.health.message").value("Los datos aun son limitados para un analisis completo."))
                .andExpect(jsonPath("$.productivity.limitedData").value(true));
    }

    @Test
    void missingOwnerIdReturns400() throws Exception {
        mockMvc.perform(get(URL).param("from", "2026-03-01").param("to", "2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void fromAfterToReturns400WithDomainMessage() throws Exception {
        var ownerId = UUID.randomUUID();
        when(queryService.handle(any(GetLivestockReportQuery.class)))
                .thenThrow(new IllegalArgumentException("'from' no puede ser posterior a 'to'."));

        mockMvc.perform(get(URL)
                        .param("ownerId", ownerId.toString())
                        .param("from", "2026-03-31")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' no puede ser posterior a 'to'."));
    }
}
