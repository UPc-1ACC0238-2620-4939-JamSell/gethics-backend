package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** La version por defecto del resumen por lote consulta de a uno y omite a los pacientes sin eventos aplicados. */
class ClinicalSummaryLookupTest {

    @Test
    void theDefaultBatchLooksUpEachPatientAndSkipsThoseWithoutSummary() {
        var withSummary = UUID.randomUUID();
        var without = UUID.randomUUID();
        var summary = new ClinicalSummary(1, LocalDate.of(2026, 1, 1), "CHECKUP");
        ClinicalSummaryLookup lookup = patientId -> patientId.equals(withSummary) ? Optional.of(summary) : Optional.empty();

        var result = lookup.findSummariesByPatientIds(List.of(withSummary, without));

        assertEquals(Map.of(withSummary, summary), result);
    }
}
