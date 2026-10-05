package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada
 * test hace rollback. Cada test usa un token aleatorio en arete, nombre y raza, asi sus resultados no dependen de
 * otros datos de la base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({AnimalQueryRepositoryImpl.class, AnimalRepositoryImpl.class})
class AnimalQueryPersistenceTest {

    private final String token = "zq" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

    @Autowired
    AnimalQueryRepository queryRepository;
    @Autowired
    AnimalRepository animals;
    @Autowired
    EntityManager em;

    private Animal save(String tag, String name, String breed, AnimalStatus status) {
        var animal = Animal.register(new RegisterAnimalCommand(tag, name, breed, null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
        ReflectionTestUtils.setField(animal, "status", status);
        var saved = animals.save(animal);
        em.clear();
        return saved;
    }

    private List<String> tags(String search, AnimalStatus status) {
        return queryRepository.search(search, status).stream().map(Animal::getTag).toList();
    }

    @Test
    void findsByTagNameOrBreedIgnoringCaseAndOrdersByTag() {
        save(token + "-B", null, "Jersey", AnimalStatus.ACTIVE);
        save("OTRO-" + token, "Luna", "Angus", AnimalStatus.ACTIVE);
        save("X-" + token.substring(0, 6), "Estrella " + token, "Gyr", AnimalStatus.ACTIVE);
        save("Y-1" + token.substring(0, 4), null, "Raza " + token.toUpperCase(), AnimalStatus.ACTIVE);

        var found = tags(token.toUpperCase(), AnimalStatus.ACTIVE);

        assertEquals(4, found.size());
        assertEquals(found.stream().sorted().toList(), found);
    }

    @Test
    void doesNotReturnNonMatchingAnimals() {
        save(token + "-1", null, "Holstein", AnimalStatus.ACTIVE);
        save("OTRA-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), null, "Jersey", AnimalStatus.ACTIVE);

        assertEquals(List.of((token + "-1").toUpperCase()), tags(token, AnimalStatus.ACTIVE));
    }

    @Test
    void filtersByStatus() {
        save(token + "-A", null, "Holstein", AnimalStatus.ACTIVE);
        save(token + "-S", null, "Holstein", AnimalStatus.SOLD);

        assertEquals(List.of((token + "-A").toUpperCase()), tags(token, AnimalStatus.ACTIVE));
        assertEquals(List.of((token + "-S").toUpperCase()), tags(token, AnimalStatus.SOLD));
        assertTrue(tags(token, AnimalStatus.DECEASED).isEmpty());
    }

    @Test
    void withoutSearchListsEveryAnimalOfThatStatus() {
        var saved = save(token + "-1", null, "Holstein", AnimalStatus.ACTIVE);

        assertTrue(queryRepository.search(null, AnimalStatus.ACTIVE).stream().anyMatch(a -> a.getId().equals(saved.getId())));
    }

    @Test
    void percentAndUnderscoreAreTakenLiterally() {
        save(token + "-1", null, "Holstein", AnimalStatus.ACTIVE);

        assertTrue(tags("%", AnimalStatus.ACTIVE).stream().noneMatch(tag -> tag.equals((token + "-1").toUpperCase())));
        assertTrue(tags(token + "_1", AnimalStatus.ACTIVE).isEmpty());
        assertEquals(1, tags(token + "-1", AnimalStatus.ACTIVE).size());
    }
}
