package com.jamsell.gethics.veterinary.interfaces.rest.transform;

import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncOutcome;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.CareRecordResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.RegisterCareResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncCareItemResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncResultResource;
import java.util.UUID;

public final class CareRecordAssembler {

    private CareRecordAssembler() {
    }

    public static RegisterCareCommand toCommand(UUID patientId, RegisterCareResource resource) {
        return new RegisterCareCommand(resource.clientRequestId(), resource.veterinarianId(), patientId,
                resource.diagnosis(), resource.treatment(), resource.nextControlDate(), resource.occurredAt());
    }

    public static RegisterCareCommand toCommand(SyncCareItemResource item) {
        return toCommand(item.patientId(), item.care());
    }

    public static CareRecordResource toResource(CareRecord record) {
        return new CareRecordResource(record.getId(), record.getClientRequestId(), record.getVeterinarianId(),
                record.getPatientId(), record.getDiagnosis(), record.getTreatment(),
                record.getNextControlDate(), record.getOccurredAt(), record.isHistorySynced());
    }

    public static SyncResultResource toResource(CareSyncOutcome outcome) {
        return new SyncResultResource(outcome.clientRequestId(), outcome.status(),
                outcome.message(), outcome.careRecordId());
    }
}
