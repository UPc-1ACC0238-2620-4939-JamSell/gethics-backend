package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.repositories.FarmQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada
 * test hace rollback y usa duenos (ownerId) aleatorios, asi que sus resultados no dependen de otros datos de la base.
 */
@LivestockPersistenceTest
class FarmPersistenceTest {

    @Autowired
    FarmRepository farms;
    @Autowired
    FarmQueryRepository queryRepository;
    @Autowired
    EntityManager em;

    private final UUID ownerId = UUID.randomUUID();

    private Farm save(UUID owner, String name) {
        var saved = farms.save(Farm.register(new RegisterFarmCommand(owner, name, "Jauja, Junin", new BigDecimal("12.50"))));
        em.clear();
        return saved;
    }

    @Test
    void persistsAFarmWithAllItsFields() {
        var saved = save(ownerId, "Fundo Sur");

        var found = queryRepository.findByOwnerId(ownerId).getFirst();

        assertEquals(saved.getId(), found.getId());
        assertEquals("Fundo Sur", found.getName());
        assertEquals("fundo sur", found.getNormalizedName());
        assertEquals("Jauja, Junin", found.getLocation());
        assertEquals(0, new BigDecimal("12.50").compareTo(found.getSizeHectares()));
        assertEquals(ownerId, found.getOwnerId());
    }

    @Test
    void existsIsPerOwnerAndIgnoresCase() {
        save(ownerId, "Fundo Sur");

        assertTrue(farms.existsByOwnerIdAndNormalizedName(ownerId, Farm.normalizeName("  FUNDO sur ")));
        assertFalse(farms.existsByOwnerIdAndNormalizedName(UUID.randomUUID(), "fundo sur"));
        assertFalse(farms.existsByOwnerIdAndNormalizedName(ownerId, "fundo norte"));
    }

    @Test
    void theUniqueConstraintRejectsTheSameNormalizedNameForTheSameOwner() {
        save(ownerId, "Fundo Sur");

        assertThrows(DataIntegrityViolationException.class, () -> save(ownerId, "FUNDO SUR"));
    }

    @Test
    void differentOwnersMayUseTheSameName() {
        save(ownerId, "Fundo Sur");

        assertDoesNotThrow(() -> save(UUID.randomUUID(), "Fundo Sur"));
    }

    @Test
    void listsOnlyTheFarmsOfTheOwnerOrderedByName() {
        save(ownerId, "Zeta");
        save(ownerId, "alfa");
        save(ownerId, "Beta");
        save(UUID.randomUUID(), "Otro dueno");

        List<String> names = queryRepository.findByOwnerId(ownerId).stream().map(Farm::getName).toList();

        assertEquals(List.of("alfa", "Beta", "Zeta"), names);
    }

    @Test
    void ownerWithoutFarmsGetsAnEmptyList() {
        assertTrue(queryRepository.findByOwnerId(UUID.randomUUID()).isEmpty());
    }
}
