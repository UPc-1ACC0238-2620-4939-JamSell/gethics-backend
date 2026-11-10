package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.commandservices.ClinicalHistoryCommandServiceImpl;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventRequestConflictException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
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

/**
 * US-35 contra PostgreSQL (application-dev.yaml; en CI, postgres:16): idempotencia por clientRequestId con el servicio y
 * la consulta reales, y la UNIQUE como ultima defensa. Cada test hace rollback.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClinicalHistoryCommandServiceImpl.class, ClinicalHistoryRepositoryImpl.class, SanitaryReminderConfiguration.class})
class SanitaryEventIdempotencyPersistenceTest {

    @Autowired
    ClinicalHistoryCommandServiceImpl service;
    @Autowired
    ClinicalHistoryRepository repository;
    @Autowired
    EntityManager em;

    private final UUID animalId = UUID.randomUUID();
    private final UUID requestId = UUID.randomUUID();

    private long eventsWith(UUID clientRequestId) {
        return em.createQuery("select count(e) from SanitaryEvent e where e.clientRequestId = :id", Long.class)
                .setParameter("id", clientRequestId).getSingleResult();
    }

    @Test
    void retryingARegisterReturnsTheSamePersistedEventAndDoesNotDuplicateIt() {
        var command = new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT,
                LocalDate.now().minusDays(1).atTime(8, 0), "Ivermectina", requestId);

        var first = service.handle(command);
        em.clear(); // el reintento llega en otra peticion: se lee de la base
        var retry = service.handle(command);

        assertTrue(first.created());
        assertFalse(retry.created());
        assertEquals(first.event().getId(), retry.event().getId());
        assertEquals(animalId, retry.event().getClinicalHistory().getAnimalId());
        assertEquals(1, eventsWith(requestId));
        assertEquals(1, repository.findByAnimalId(animalId).orElseThrow().getEvents().size());
    }

    @Test
    void retryingAScheduleReturnsTheSameEventAndOtherDataIsAConflict() {
        var date = LocalDate.now().plusDays(10);
        var command = new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, date, "Aftosa", requestId);

        var first = service.handle(command);
        em.clear();
        var retry = service.handle(command);

        assertEquals(first.event().getId(), retry.event().getId());
        assertFalse(retry.created());
        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(
                new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION, date.plusDays(1), "Aftosa", requestId)));
        assertEquals(1, eventsWith(requestId));
    }

    @Test
    void anIdAlreadyUsedByAnotherAnimalOrOperationIsAConflict() {
        var occurredAt = LocalDate.now().minusDays(1).atTime(8, 0);
        service.handle(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP, occurredAt, null, requestId));
        em.clear();

        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(
                new RegisterSanitaryEventCommand(UUID.randomUUID(), SanitaryEventType.CHECKUP, occurredAt, null, requestId)));
        assertThrows(SanitaryEventRequestConflictException.class, () -> service.handle(
                new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP, LocalDate.now().plusDays(1), null, requestId)));
        assertEquals(1, eventsWith(requestId));
    }

    @Test
    void theDatabaseRejectsTheSameClientRequestIdTwice() {
        var occurredAt = LocalDate.now().minusDays(1).atTime(8, 0);
        var cow = new ClinicalHistory(animalId);
        cow.registerEvent(SanitaryEventType.CHECKUP, occurredAt, null, requestId);
        repository.save(cow);
        var bull = new ClinicalHistory(UUID.randomUUID());
        bull.registerEvent(SanitaryEventType.CHECKUP, occurredAt, null, requestId);

        assertThrows(DataIntegrityViolationException.class, () -> repository.save(bull));
    }

    @Test
    void eventsWithoutClientRequestIdAreNotConstrained() {
        var occurredAt = LocalDate.now().minusDays(1).atTime(8, 0);
        var legacy = new RegisterSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP, occurredAt, null);

        var first = service.handle(legacy);
        var second = service.handle(legacy); // sin id no hay idempotencia: dos eventos, como antes de US-35

        assertTrue(first.created());
        assertTrue(second.created());
        assertNotEquals(first.event().getId(), second.event().getId());
        assertNull(second.event().getClientRequestId());
    }
}
