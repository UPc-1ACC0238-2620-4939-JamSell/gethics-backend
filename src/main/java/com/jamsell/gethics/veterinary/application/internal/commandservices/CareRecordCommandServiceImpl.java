package com.jamsell.gethics.veterinary.application.internal.commandservices;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalHistoryWriter;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.PatientAccessLookup;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareRejectedException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.ClinicalHistorySyncException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientAccessDeniedException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotActiveException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientNotFoundException;
import com.jamsell.gethics.veterinary.domain.model.exceptions.PatientWithoutClientException;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareRegistration;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncOutcome;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncStatus;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.VeterinaryAssignmentStatus;
import com.jamsell.gethics.veterinary.domain.services.CareRecordCommandService;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.CareRecordRepository;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.VeterinaryAssignmentRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CareRecordCommandServiceImpl implements CareRecordCommandService {

    private final CareRecordRepository careRecordRepository;
    private final ClinicalHistoryWriter clinicalHistoryWriter;
    private final VeterinaryAssignmentRepository assignmentRepository;
    private final PatientAccessLookup patientAccessLookup;

    public CareRecordCommandServiceImpl(CareRecordRepository careRecordRepository,
                                        ClinicalHistoryWriter clinicalHistoryWriter,
                                        VeterinaryAssignmentRepository assignmentRepository,
                                        PatientAccessLookup patientAccessLookup) {
        this.careRecordRepository = careRecordRepository;
        this.clinicalHistoryWriter = clinicalHistoryWriter;
        this.assignmentRepository = assignmentRepository;
        this.patientAccessLookup = patientAccessLookup;
    }

    /**
     * Un reintento con el mismo clientRequestId nunca vuelve a validar el paciente: devuelve el registro existente (y
     * termina de sincronizar su historial si quedo pendiente). Una atencion nueva exige un paciente real y ACTIVE
     * de un cliente asignado al veterinario.
     */
    @Override
    public CareRegistration handle(RegisterCareCommand command) {
        var existing = careRecordRepository.findByClientRequestId(command.clientRequestId());
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), command);
        }
        var record = new CareRecord(command);
        authorize(command);
        try {
            var saved = careRecordRepository.saveAndFlush(record);
            pushToHistory(saved);
            return new CareRegistration(saved, true);
        } catch (DataIntegrityViolationException exception) {
            var concurrent = careRecordRepository.findByClientRequestId(command.clientRequestId())
                    .orElseThrow(() -> exception);
            return resolveExisting(concurrent, command);
        }
    }

    @Override
    public List<CareSyncOutcome> handle(SyncCareBatchCommand command) {
        return command.items().stream().map(this::syncItem).toList();
    }

    private CareSyncOutcome syncItem(RegisterCareCommand item) {
        try {
            var result = handle(item);
            var status = result.created() ? CareSyncStatus.CREATED : CareSyncStatus.DUPLICATE;
            return new CareSyncOutcome(item.clientRequestId(), status, null, result.record().getId());
        } catch (CareConflictException exception) {
            return new CareSyncOutcome(item.clientRequestId(), CareSyncStatus.CONFLICT,
                    exception.getMessage(), null);
        } catch (CareRejectedException | IllegalArgumentException exception) {
            return new CareSyncOutcome(item.clientRequestId(), CareSyncStatus.REJECTED,
                    exception.getMessage(), null);
        } catch (ClinicalHistorySyncException exception) {
            // La atencion si quedo guardada: el ultimo paso se completa al reintentar el mismo clientRequestId.
            return new CareSyncOutcome(item.clientRequestId(), CareSyncStatus.FAILED,
                    exception.getMessage(), null);
        }
    }

    private void authorize(RegisterCareCommand command) {
        var patient = patientAccessLookup.findByPatientId(command.patientId())
                .orElseThrow(() -> new PatientNotFoundException(command.patientId()));
        if (!patient.active()) {
            throw new PatientNotActiveException(command.patientId());
        }
        if (patient.clientId() == null) {
            throw new PatientWithoutClientException(command.patientId());
        }
        var assigned = assignmentRepository.existsByVeterinarianIdAndClientIdAndStatus(
                command.veterinarianId(), patient.clientId(), VeterinaryAssignmentStatus.ACTIVE);
        if (!assigned) {
            throw new PatientAccessDeniedException(command.veterinarianId(), command.patientId());
        }
    }

    private CareRegistration resolveExisting(CareRecord existing, RegisterCareCommand command) {
        if (!existing.matches(command)) {
            throw new CareConflictException(command.clientRequestId());
        }
        if (!existing.isHistorySynced()) {
            pushToHistory(existing);
        }
        return new CareRegistration(existing, false);
    }

    private void pushToHistory(CareRecord record) {
        try {
            clinicalHistoryWriter.append(record);
        } catch (RuntimeException exception) {
            throw new ClinicalHistorySyncException(exception);
        }
        record.markHistorySynced();
        careRecordRepository.save(record);
    }
}
