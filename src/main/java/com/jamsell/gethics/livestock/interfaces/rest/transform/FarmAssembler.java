package com.jamsell.gethics.livestock.interfaces.rest.transform;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterFarmResource;

import java.util.List;

public final class FarmAssembler {

    static final String NO_FARMS = "No hay granjas registradas.";

    private FarmAssembler() {
    }

    public static RegisterFarmCommand toCommand(RegisterFarmResource resource) {
        return new RegisterFarmCommand(resource.ownerId(), resource.name(), resource.location(), resource.sizeHectares());
    }

    public static FarmResource toResource(Farm farm) {
        return new FarmResource(farm.getId(), farm.getOwnerId(), farm.getName(), farm.getLocation(),
                farm.getSizeHectares(), farm.getStatus());
    }

    public static FarmListResource toListResource(List<Farm> farms) {
        var items = farms.stream().map(FarmAssembler::toResource).toList();
        return new FarmListResource(items, items.isEmpty() ? NO_FARMS : null);
    }
}
