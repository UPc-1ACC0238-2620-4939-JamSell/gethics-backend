package com.jamsell.gethics.veterinary.application.internal.outboundservices;

import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalSummaryLookup {

    Optional<ClinicalSummary> findSummaryByPatientId(UUID patientId);

    /**
     * Resumen de varios pacientes. Los pacientes sin eventos aplicados no aparecen en el mapa. Los adaptadores deben
     * sobrescribirlo con una sola consulta; esta version por defecto consulta de a uno.
     */
    default Map<UUID, ClinicalSummary> findSummariesByPatientIds(Collection<UUID> patientIds) {
        var result = new HashMap<UUID, ClinicalSummary>();
        for (var patientId : patientIds) {
            findSummaryByPatientId(patientId).ifPresent(summary -> result.put(patientId, summary));
        }
        return result;
    }
}
