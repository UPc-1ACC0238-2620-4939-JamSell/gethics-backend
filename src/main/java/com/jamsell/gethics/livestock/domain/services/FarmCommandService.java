package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateFarmCommand;

public interface FarmCommandService {
    Farm handle(RegisterFarmCommand command);

    /** US-32: edita la granja. Falla si no existe o si otra granja del mismo dueno ya tiene ese nombre. */
    Farm handle(UpdateFarmCommand command);
}
