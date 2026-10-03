package com.jamsell.gethics.analytics.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Los enums deben coincidir exactamente con los valores definidos en el informe. */
class AnalyticsEnumsTest {

    @Test
    void riskLevelHasTheFourLevelsOfTheReport() {
        assertEquals(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"), names(RiskLevel.values()));
    }

    @Test
    void alertStatusHasPendingSentRead() {
        assertEquals(List.of("PENDING", "SENT", "READ"), names(AlertStatus.values()));
    }

    @Test
    void trendTypeHasSanitaryFinancialCombined() {
        assertEquals(List.of("SANITARY", "FINANCIAL", "COMBINED"), names(TrendType.values()));
    }

    private static List<String> names(Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).toList();
    }
}
