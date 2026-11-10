package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.sanitary.interfaces.acl.SanitaryContextFacade;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalSummaryLookup;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.ClinicalSummary;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** TS07: resumen del historial clinico del paciente, leido por la fachada de Sanitary. */
@Component
public class SanitaryClinicalSummaryAdapter implements ClinicalSummaryLookup {

    private final SanitaryContextFacade sanitary;

    public SanitaryClinicalSummaryAdapter(SanitaryContextFacade sanitary) {
        this.sanitary = sanitary;
    }

    @Override
    public Optional<ClinicalSummary> findSummaryByPatientId(UUID patientId) {
        return sanitary.findClinicalSummary(patientId)
                .map(summary -> new ClinicalSummary(summary.totalEvents(), summary.lastEventDate(),
                        summary.lastEventType()));
    }

    /** Una sola consulta para todos los pacientes de la lista (evita una consulta por animal). */
    @Override
    public Map<UUID, ClinicalSummary> findSummariesByPatientIds(Collection<UUID> patientIds) {
        return sanitary.findClinicalSummaries(patientIds).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ClinicalSummary(
                        entry.getValue().totalEvents(), entry.getValue().lastEventDate(),
                        entry.getValue().lastEventType())));
    }
}
