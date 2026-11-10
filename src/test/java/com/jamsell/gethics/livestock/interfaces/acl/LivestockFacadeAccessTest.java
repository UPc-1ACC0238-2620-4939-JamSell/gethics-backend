package com.jamsell.gethics.livestock.interfaces.acl;

import com.jamsell.gethics.livestock.application.internal.queryservices.FarmQueryServiceImpl;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmByIdQuery;
import com.jamsell.gethics.livestock.domain.repositories.FarmQueryRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** TS07: estado y dueno de un animal tal como los ve el modulo veterinario. */
class LivestockFacadeAccessTest {

    private final FarmQueryService farms = mock(FarmQueryService.class);
    private final AnimalQueryService animals = mock(AnimalQueryService.class);
    private final LivestockContextFacade facade =
            new LivestockContextFacade(farms, mock(AnimalFarmQueryService.class), animals);

    private Animal animal(UUID farmId) {
        var animal = Animal.register(new RegisterAnimalCommand("A-1", null, "Holstein", null,
                LocalDate.of(2024, 1, 1), null, null, farmId), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "id", UUID.randomUUID());
        when(animals.handle(new GetAnimalByIdQuery(animal.getId()))).thenReturn(animal);
        return animal;
    }

    @Test
    void anActiveAnimalInAFarmReportsTheFarmOwner() {
        var owner = UUID.randomUUID();
        var farmId = UUID.randomUUID();
        var farm = Farm.register(new RegisterFarmCommand(owner, "Fundo", "Jauja", null));
        when(farms.handle(new GetFarmByIdQuery(farmId))).thenReturn(Optional.of(farm));
        var animal = animal(farmId);

        assertEquals(Optional.of(new AnimalAccessInfo(animal.getId(), true, owner)),
                facade.findAnimalAccess(animal.getId()));
    }

    @Test
    void anAnimalWithoutFarmHasNoOwner() {
        var animal = animal(null);

        assertEquals(Optional.of(new AnimalAccessInfo(animal.getId(), true, null)),
                facade.findAnimalAccess(animal.getId()));
    }

    @Test
    void aFarmThatCannotBeFoundLeavesTheOwnerUnknown() {
        var farmId = UUID.randomUUID();
        when(farms.handle(new GetFarmByIdQuery(farmId))).thenReturn(Optional.empty());
        var animal = animal(farmId);

        assertEquals(Optional.of(new AnimalAccessInfo(animal.getId(), true, null)),
                facade.findAnimalAccess(animal.getId()));
    }

    @Test
    void aDeactivatedOrSoldAnimalIsReportedAsNotActive() {
        var deactivated = animal(null);
        deactivated.deactivate();
        var sold = animal(null);
        sold.sell(java.math.BigDecimal.TEN, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1));

        assertEquals(false, facade.findAnimalAccess(deactivated.getId()).orElseThrow().active());
        assertEquals(false, facade.findAnimalAccess(sold.getId()).orElseThrow().active());
    }

    @Test
    void anUnknownAnimalHasNoAccessInfo() {
        var id = UUID.randomUUID();
        when(animals.handle(new GetAnimalByIdQuery(id))).thenThrow(new AnimalNotFoundException(id));

        assertTrue(facade.findAnimalAccess(id).isEmpty());
    }

    @Test
    void theFarmQueryServiceFindsAFarmById() {
        var repository = mock(FarmQueryRepository.class);
        var farmId = UUID.randomUUID();
        var farm = Farm.register(new RegisterFarmCommand(UUID.randomUUID(), "Fundo", "Jauja", null));
        when(repository.findById(farmId)).thenReturn(Optional.of(farm));

        assertEquals(Optional.of(farm), new FarmQueryServiceImpl(repository).handle(new GetFarmByIdQuery(farmId)));
        assertTrue(new FarmQueryServiceImpl(repository).handle(new GetFarmByIdQuery(UUID.randomUUID())).isEmpty());
    }
}
