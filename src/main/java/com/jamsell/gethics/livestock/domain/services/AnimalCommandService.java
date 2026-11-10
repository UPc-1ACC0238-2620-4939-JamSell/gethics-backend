package com.jamsell.gethics.livestock.domain.services;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.DeactivateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.SellAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSale;

public interface AnimalCommandService {
    Animal handle(RegisterAnimalCommand command);

    Animal handle(UpdateAnimalCommand command);

    Animal handle(DeactivateAnimalCommand command);

    /** US-30: vende el animal y registra el ingreso, en una sola transaccion. */
    AnimalSale handle(SellAnimalCommand command);
}
