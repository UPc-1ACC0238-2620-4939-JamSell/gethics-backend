package com.jamsell.gethics.analytics.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamsell.gethics.analytics.domain.exceptions.ReportDataNotAvailableException;
import com.jamsell.gethics.analytics.domain.model.queries.GetLivestockReportQuery;
import com.jamsell.gethics.analytics.domain.model.valueobjects.FinanceReportSection;
import com.jamsell.gethics.analytics.domain.model.valueobjects.LivestockReport;
import com.jamsell.gethics.analytics.domain.model.valueobjects.ReportSection;
import com.jamsell.gethics.analytics.domain.services.LivestockReportExportService;
import com.jamsell.gethics.analytics.domain.services.LivestockReportQueryService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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

    @MockitoBean
    LivestockReportExportService exportService;

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

    // --- US-39: exportar el reporte en CSV ---

    private static final String EXPORT_URL = URL + "/export";

    @Test
    void exportReturns200WithTheCsvFileAndItsDownloadHeaders() throws Exception {
        var ownerId = UUID.randomUUID();
        var csv = "\uFEFFSeccion,Indicador,Valor\r\nFinanzas,Ingresos totales,500.00\r\n".getBytes(StandardCharsets.UTF_8);
        when(exportService.export(any(GetLivestockReportQuery.class))).thenReturn(csv);

        mockMvc.perform(get(EXPORT_URL)
                        .param("ownerId", ownerId.toString())
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"reporte-ganado-2026-03-01-2026-03-31.csv\""))
                .andExpect(content().bytes(csv));

        verify(exportService).export(new GetLivestockReportQuery(ownerId, LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31)));
    }

    @Test
    void exportWithoutDataInThePeriodReturns404WithTheMessageForTheApp() throws Exception {
        when(exportService.export(any(GetLivestockReportQuery.class))).thenThrow(new ReportDataNotAvailableException());

        mockMvc.perform(get(EXPORT_URL)
                        .param("ownerId", UUID.randomUUID().toString())
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("No hay información disponible para el periodo seleccionado"));
    }

    @Test
    void exportErrorsAreJsonEvenWhenTheClientAsksForCsv() throws Exception {
        when(exportService.export(any(GetLivestockReportQuery.class))).thenThrow(new ReportDataNotAvailableException());

        mockMvc.perform(get(EXPORT_URL)
                        .accept("text/csv")
                        .param("ownerId", UUID.randomUUID().toString())
                        .param("from", "2026-03-01")
                        .param("to", "2026-03-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No hay información disponible para el periodo seleccionado"));
    }

    @Test
    void exportWithFromAfterToReturns400WithAMessage() throws Exception {
        when(exportService.export(any(GetLivestockReportQuery.class)))
                .thenThrow(new IllegalArgumentException("'from' no puede ser posterior a 'to'."));

        mockMvc.perform(get(EXPORT_URL)
                        .param("ownerId", UUID.randomUUID().toString())
                        .param("from", "2026-04-01")
                        .param("to", "2026-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("'from' no puede ser posterior a 'to'."));
    }

    @Test
    void exportWithAnInvalidDateReturns400WithoutCallingTheService() throws Exception {
        mockMvc.perform(get(EXPORT_URL)
                        .param("ownerId", UUID.randomUUID().toString())
                        .param("from", "01/03/2026")
                        .param("to", "2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verifyNoInteractions(exportService);
    }
}
