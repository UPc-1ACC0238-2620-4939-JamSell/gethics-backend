package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;

public interface AnimalCommandService {
    Animal handle(RegisterAnimalCommand command);

    Animal handle(UpdateAnimalCommand command);

    Animal handle(DeactivateAnimalCommand command);
}
