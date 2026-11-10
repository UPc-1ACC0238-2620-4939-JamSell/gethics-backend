package com.jamsell.gethics.veterinary.application.internal.queryservices;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClientPatientsLookup;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalSummaryLookup;
import com.jamsell.gethics.veterinary.domain.model.exceptions.ClientNotAssignedException;
import com.jamsell.gethics.veterinary.domain.model.queries.GetClientPatientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientInfo;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientView;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryPatientQueryService;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VeterinaryPatientQueryServiceImpl implements VeterinaryPatientQueryService {

    private final VeterinaryAssignmentRepository assignmentRepository;
    private final ClientPatientsLookup clientPatientsLookup;
    private final ClinicalSummaryLookup clinicalSummaryLookup;

    public VeterinaryPatientQueryServiceImpl(VeterinaryAssignmentRepository assignmentRepository,
                                             ClientPatientsLookup clientPatientsLookup,
                                             ClinicalSummaryLookup clinicalSummaryLookup) {
        this.assignmentRepository = assignmentRepository;
        this.clientPatientsLookup = clientPatientsLookup;
        this.clinicalSummaryLookup = clinicalSummaryLookup;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientView> handle(GetClientPatientsQuery query) {
        var assigned = assignmentRepository.existsByVeterinarianIdAndClientIdAndStatus(
                query.veterinarianId(), query.clientId(), VeterinaryAssignmentStatus.ACTIVE);
        if (!assigned) {
            throw new ClientNotAssignedException(query.veterinarianId(), query.clientId());
        }
        var patients = clientPatientsLookup.findPatientsByClientId(query.clientId());
        // Los resumenes de todos los pacientes se piden juntos (una consulta), no uno por uno.
        var summaries = clinicalSummaryLookup.findSummariesByPatientIds(
                patients.stream().map(PatientInfo::patientId).toList());
        return patients.stream()
                .map(patient -> new PatientView(patient, summaries.get(patient.patientId())))
                .toList();
    }
}
