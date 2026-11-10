package com.jamsell.gethics.livestock.interfaces.acl;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LivestockContextFacadeTest {

    private final FarmQueryService farms = mock(FarmQueryService.class);
    private final AnimalFarmQueryService animalsByFarm = mock(AnimalFarmQueryService.class);
    private final AnimalQueryService animals = mock(AnimalQueryService.class);
    private final LivestockContextFacade facade = new LivestockContextFacade(farms, animalsByFarm, animals);

    private final UUID ownerId = UUID.randomUUID();

    private Farm farm(String name, String location) {
        var farm = Farm.register(new RegisterFarmCommand(ownerId, name, location, null));
        ReflectionTestUtils.setField(farm, "id", UUID.randomUUID());
        return farm;
    }

    private Animal animal(String tag, String name) {
        var animal = Animal.register(new RegisterAnimalCommand(tag, name, "Holstein", null,
                LocalDate.of(2024, 1, 1), null, null, null), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "id", UUID.randomUUID());
        return animal;
    }

    @Test
    void returnsTheActiveAnimalsOfAllTheOwnersFarms() {
        var north = farm("Norte", "Cajamarca");
        var south = farm("Sur", "Arequipa");
        var cow = animal("A-1", "Lola");
        var bull = animal("B-2", null);
        when(farms.handle(new GetFarmsByOwnerQuery(ownerId))).thenReturn(List.of(north, south));
        when(animalsByFarm.handle(GetAnimalsByFarmQuery.of(north.getId(), AnimalStatus.ACTIVE))).thenReturn(List.of(cow));
        when(animalsByFarm.handle(GetAnimalsByFarmQuery.of(south.getId(), AnimalStatus.ACTIVE))).thenReturn(List.of(bull));

        var result = facade.findActiveAnimalsByOwnerId(ownerId);

        assertEquals(List.of(
                new AnimalSummary(cow.getId(), "Lola", "A-1", "Holstein", "ACTIVE"),
                new AnimalSummary(bull.getId(), null, "B-2", "Holstein", "ACTIVE")), result);
    }

    @Test
    void onlyAsksForActiveAnimalsSoDeactivatedOnesAreNeverPatients() {
        var north = farm("Norte", "Cajamarca");
        when(farms.handle(new GetFarmsByOwnerQuery(ownerId))).thenReturn(List.of(north));
        when(animalsByFarm.handle(GetAnimalsByFarmQuery.of(north.getId(), AnimalStatus.ACTIVE))).thenReturn(List.of());

        assertTrue(facade.findActiveAnimalsByOwnerId(ownerId).isEmpty());

        verify(animalsByFarm).handle(new GetAnimalsByFarmQuery(north.getId(), AnimalStatus.ACTIVE));
    }

    @Test
    void anOwnerWithoutFarmsHasNoAnimals() {
        when(farms.handle(new GetFarmsByOwnerQuery(ownerId))).thenReturn(List.of());

        assertTrue(facade.findActiveAnimalsByOwnerId(ownerId).isEmpty());
    }

    @Test
    void locationIsTheOneOfTheFirstFarm() {
        when(farms.handle(new GetFarmsByOwnerQuery(ownerId)))
                .thenReturn(List.of(farm("Norte", "Cajamarca"), farm("Sur", "Arequipa")));

        assertEquals("Cajamarca", facade.findLocationByOwnerId(ownerId).orElseThrow());
    }

    @Test
    void locationIsEmptyWithoutFarms() {
        when(farms.handle(new GetFarmsByOwnerQuery(ownerId))).thenReturn(List.of());

        assertTrue(facade.findLocationByOwnerId(ownerId).isEmpty());
    }
}
