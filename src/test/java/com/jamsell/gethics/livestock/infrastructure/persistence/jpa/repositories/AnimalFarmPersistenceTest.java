package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.application.internal.commandservices.AnimalFarmCommandServiceImpl;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.AssignAnimalToFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada
 * test hace rollback y usa ids y duenos aleatorios, asi que sus resultados no dependen de otros datos de la base.
 */
@LivestockPersistenceTest
class AnimalFarmPersistenceTest {

    @Autowired
    AnimalRepository animals;
    @Autowired
    AnimalQueryRepository animalQueries;
    @Autowired
    FarmRepository farms;
    @Autowired
    AnimalFarmAssignmentRepository assignments;
    @Autowired
    AnimalFarmCommandServiceImpl commandService;
    @Autowired
    EntityManager em;

    private final String token = "zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    private Animal saveAnimal(String suffix, AnimalStatus status) {
        var animal = Animal.register(new RegisterAnimalCommand(token + suffix, null, "Holstein", null,
                LocalDate.of(2024, 1, 1), null, null, null), LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "status", status);
        return animals.save(animal);
    }

    private Farm saveFarm(String name) {
        return farms.save(Farm.register(new RegisterFarmCommand(UUID.randomUUID(), name + token, "Lima", null)));
    }

    private List<String> tags(UUID farmId, AnimalStatus status) {
        return animalQueries.findByFarmId(farmId, status).stream().map(Animal::getTag).toList();
    }

    @Test
    void assigningPersistsTheFarmOfTheAnimal() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farm = saveFarm("Fundo");

        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farm.getId()));
        em.clear();

        assertEquals(farm.getId(), animals.findById(animal.getId()).orElseThrow().getFarmId());
    }

    @Test
    void movingKeepsEveryChangeInChronologicalOrder() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farmA = saveFarm("A");
        var farmB = saveFarm("B");

        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farmA.getId()));
        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farmB.getId()));
        em.clear();

        List<AnimalFarmAssignment> history = assignments.findByAnimalId(animal.getId());
        assertEquals(2, history.size());
        assertNull(history.get(0).getFromFarmId());
        assertEquals(farmA.getId(), history.get(0).getToFarmId());
        assertEquals(farmA.getId(), history.get(1).getFromFarmId());
        assertEquals(farmB.getId(), history.get(1).getToFarmId());
        assertEquals(farmB.getId(), animals.findById(animal.getId()).orElseThrow().getFarmId());
    }

    @Test
    void repeatingTheSameFarmAddsNoHistory() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farm = saveFarm("Fundo");

        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farm.getId()));
        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farm.getId()));
        em.clear();

        assertEquals(1, assignments.findByAnimalId(animal.getId()).size());
    }

    @Test
    void historiesOfDifferentAnimalsDoNotMix() {
        var first = saveAnimal("-1", AnimalStatus.ACTIVE);
        var second = saveAnimal("-2", AnimalStatus.ACTIVE);
        var farm = saveFarm("Fundo");

        commandService.handle(new AssignAnimalToFarmCommand(first.getId(), farm.getId()));
        em.clear();

        assertEquals(1, assignments.findByAnimalId(first.getId()).size());
        assertTrue(assignments.findByAnimalId(second.getId()).isEmpty());
    }

    @Test
    void theFarmListsItsAnimalsByTagAndStatusAndNothingFromOtherFarms() {
        var farm = saveFarm("Fundo");
        var other = saveFarm("Otro");
        var b = saveAnimal("-B", AnimalStatus.ACTIVE);
        var a = saveAnimal("-A", AnimalStatus.ACTIVE);
        var sold = saveAnimal("-S", AnimalStatus.SOLD);
        var elsewhere = saveAnimal("-X", AnimalStatus.ACTIVE);
        for (var animal : List.of(a, b, sold)) {
            commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farm.getId()));
        }
        commandService.handle(new AssignAnimalToFarmCommand(elsewhere.getId(), other.getId()));
        em.clear();

        assertEquals(List.of((token + "-A").toUpperCase(), (token + "-B").toUpperCase()), tags(farm.getId(), AnimalStatus.ACTIVE));
        assertEquals(List.of((token + "-S").toUpperCase()), tags(farm.getId(), AnimalStatus.SOLD));
        assertTrue(tags(farm.getId(), AnimalStatus.DECEASED).isEmpty());
    }

    @Test
    void aMovedAnimalLeavesTheListOfItsOldFarm() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farmA = saveFarm("A");
        var farmB = saveFarm("B");

        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farmA.getId()));
        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farmB.getId()));
        em.clear();

        assertTrue(tags(farmA.getId(), AnimalStatus.ACTIVE).isEmpty());
        assertEquals(1, tags(farmB.getId(), AnimalStatus.ACTIVE).size());
    }

    @Test
    void existsByIdTellsRealFromUnknownIds() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farm = saveFarm("Fundo");

        assertTrue(animals.existsById(animal.getId()));
        assertTrue(farms.existsById(farm.getId()));
        assertFalse(animals.existsById(UUID.randomUUID()));
        assertFalse(farms.existsById(UUID.randomUUID()));
    }

    @Test
    void theHistoryRecordsTheMomentOfTheChange() {
        var animal = saveAnimal("-1", AnimalStatus.ACTIVE);
        var farm = saveFarm("Fundo");
        var before = Instant.now().minusSeconds(5);

        commandService.handle(new AssignAnimalToFarmCommand(animal.getId(), farm.getId()));
        em.clear();

        assertTrue(assignments.findByAnimalId(animal.getId()).getFirst().getAssignedAt().isAfter(before));
    }
}
