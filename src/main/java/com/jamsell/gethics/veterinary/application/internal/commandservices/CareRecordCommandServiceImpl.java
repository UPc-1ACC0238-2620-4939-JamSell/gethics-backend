package com.jamsell.gethics.veterinary.application.internal.commandservices;

import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalHistoryWriter;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareRegistration;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncOutcome;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncStatus;
import com.jamsell.gethics.veterinary.domain.services.CareRecordCommandService;
import com.jamsell.gethics.veterinary.infrastructure.persistence.jpa.repositories.CareRecordRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class CareRecordCommandServiceImpl implements CareRecordCommandService {

    private final CareRecordRepository careRecordRepository;
    private final ClinicalHistoryWriter clinicalHistoryWriter;

    public CareRecordCommandServiceImpl(CareRecordRepository careRecordRepository,
                                        ClinicalHistoryWriter clinicalHistoryWriter) {
        this.careRecordRepository = careRecordRepository;
        this.clinicalHistoryWriter = clinicalHistoryWriter;
    }

    @Override
    public CareRegistration handle(RegisterCareCommand command) {
        var existing = careRecordRepository.findByClientRequestId(command.clientRequestId());
        if (existing.isPresent()) {
            return resolveExisting(existing.get(), command);
        }
        var record = new CareRecord(command);
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
        } catch (IllegalArgumentException exception) {
            return new CareSyncOutcome(item.clientRequestId(), CareSyncStatus.REJECTED,
                    exception.getMessage(), null);
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
        clinicalHistoryWriter.append(record);
        record.markHistorySynced();
        careRecordRepository.save(record);
    }
}
