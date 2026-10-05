package com.jamsell.gethics.veterinary.domain.services;

import com.jamsell.gethics.veterinary.domain.model.commands.RegisterCareCommand;
import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareRegistration;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.CareSyncOutcome;
import java.util.List;

public interface CareRecordCommandService {

    CareRegistration handle(RegisterCareCommand command);

    List<CareSyncOutcome> handle(SyncCareBatchCommand command);
}
