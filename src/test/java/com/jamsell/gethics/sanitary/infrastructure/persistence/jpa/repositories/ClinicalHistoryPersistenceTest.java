package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.commandservices.ClinicalHistoryCommandServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.infrastructure.configuration.SanitaryReminderConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada test hace rollback. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClinicalHistoryCommandServiceImpl.class, ClinicalHistoryRepositoryImpl.class, SanitaryReminderConfiguration.class})
class ClinicalHistoryPersistenceTest {

    @Autowired
    ClinicalHistoryCommandServiceImpl service;
    @Autowired
    ClinicalHistoryRepository repository;
    @Autowired
    EntityManager em;

    @Test
    void secondEventForSameAnimalReusesTheHistoryAndBothArePersisted() {
        var animalId = UUID.randomUUID();
        var occurredAt = LocalDate.now().minusDays(1).atTime(8, 15, 30);

        var first = service.handle(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, occurredAt, "Aftosa"));
        var second = service.handle(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt.plusHours(2), null));

        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
        assertEquals(SanitaryEventType.TREATMENT, second.getType());

        em.flush();
        em.clear();
        var reloaded = repository.findByAnimalId(animalId).orElseThrow();
        assertEquals(2, reloaded.getEvents().size());
        var vaccination = reloaded.getEvents().stream()
                .filter(e -> e.getType() == SanitaryEventType.VACCINATION).findFirst().orElseThrow();
        assertEquals("Aftosa", vaccination.getDescription());
        assertEquals(occurredAt, vaccination.getOccurredAt());
        assertEquals(SanitaryEventStatus.COMPLETED, vaccination.getStatus());
        assertEquals(reloaded.getId(), first.getClinicalHistory().getId());
    }

    @Test
    void animalIdIsUnique() {
        var animalId = UUID.randomUUID();
        repository.save(new ClinicalHistory(animalId));

        assertThrows(DataIntegrityViolationException.class,
                () -> repository.save(new ClinicalHistory(animalId)));
    }
}
