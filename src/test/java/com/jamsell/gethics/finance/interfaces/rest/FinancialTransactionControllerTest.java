package com.jamsell.gethics.finance.interfaces.rest;

import com.jamsell.gethics.finance.application.internal.commandservices.FinancialManagementCommandServiceImpl;
import com.jamsell.gethics.finance.application.internal.queryservices.FinancialManagementQueryServiceImpl;
import com.jamsell.gethics.finance.domain.model.aggregates.FinancialManagement;
import com.jamsell.gethics.finance.domain.model.valueobjects.FinancialTransactionType;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialManagementRepository;
import com.jamsell.gethics.finance.infrastructure.persistence.jpa.repositories.FinancialTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa los query/command services reales (para ejercitar la validacion del rango) con los repositorios mockeados. */
@WebMvcTest(FinancialTransactionController.class)
@Import({FinancialManagementQueryServiceImpl.class, FinancialManagementCommandServiceImpl.class})
@WithMockUser
class FinancialTransactionControllerTest {

    private static final String URL = "/api/v1/finances";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    FinancialManagementRepository financialManagementRepository;

    @MockitoBean
    FinancialTransactionRepository financialTransactionRepository;

    private final UUID ownerId = UUID.randomUUID();

    private ResultActions getSummary(String query) throws Exception {
        return mockMvc.perform(get(URL + query));
    }

    @Test
    void returnsTotalsAndNetBalanceForWholeHistoryWhenNoPeriodGiven() throws Exception {
        var financialManagement = new FinancialManagement(ownerId);
        var income = financialManagement.register(FinancialTransactionType.INCOME, new BigDecimal("200.00"),
                "Cattle sale", LocalDate.of(2026, 9, 1), null);
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(financialManagement));
        when(financialTransactionRepository.findByFinancialManagementIdOrderByOccurredOnDesc(
                financialManagement.getId())).thenReturn(List.of(income));

        getSummary("?ownerId=" + ownerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.totalIncome").value(200.00))
                .andExpect(jsonPath("$.totalExpense").value(0))
                .andExpect(jsonPath("$.balance").value(200.00))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.transactions.length()").value(1));
    }

    @Test
    void periodWithoutMovementsReturnsZeroBalanceAndInformativeMessage() throws Exception {
        var financialManagement = new FinancialManagement(ownerId);
        when(financialManagementRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(financialManagement));
        when(financialTransactionRepository.findByFinancialManagementIdAndOccurredOnBetweenOrderByOccurredOnDesc(
                financialManagement.getId(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of());

        getSummary("?ownerId=" + ownerId + "&from=2026-01-01&to=2026-01-31")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.transactions").isEmpty())
                .andExpect(jsonPath("$.message").value("No existen movimientos registrados para el periodo "
                        + "seleccionado."));
    }

    @Test
    void onlyFromWithoutToReturns400() throws Exception {
        getSummary("?ownerId=" + ownerId + "&from=2026-01-01")
                .andExpect(status().isBadRequest());
    }

    @Test
    void fromAfterToReturns400() throws Exception {
        getSummary("?ownerId=" + ownerId + "&from=2026-09-30&to=2026-09-01")
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingOwnerIdReturns400() throws Exception {
        getSummary("").andExpect(status().isBadRequest());
    }
}
