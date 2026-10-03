package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.queryservices.ClinicalHistoryQueryServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada
 * test hace rollback. Cada test usa animalId aleatorios, asi que sus resultados no dependen de otros datos de la base.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClinicalHistoryQueryRepositoryImpl.class, ClinicalHistoryQueryServiceImpl.class, ClinicalHistoryRepositoryImpl.class})
class ClinicalHistoryQueryPersistenceTest {

    @Autowired
    ClinicalHistoryQueryRepository queryRepository;
    @Autowired
    ClinicalHistoryQueryServiceImpl service;
    @Autowired
    ClinicalHistoryRepository histories;
    @Autowired
    EntityManager em;

    private void persist(ClinicalHistory history) {
        histories.save(history);
        em.clear();
    }

    private Set<UUID> ids(List<SanitaryEvent> events) {
        return events.stream().map(SanitaryEvent::getId).collect(Collectors.toSet());
    }

    @Test
    void returnsEveryEventOfTheAnimalWhateverItsStatus() {
        var history = new ClinicalHistory(UUID.randomUUID());
        var completed = history.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 5, 1, 9, 0), "aplicada");
        var scheduled = history.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2042, 1, 10), "programado");
        var cancelled = history.scheduleEvent(SanitaryEventType.TREATMENT, LocalDate.of(2042, 2, 10), "cancelado");
        persist(history);
        // No existe comportamiento de cancelacion (fuera de alcance): se simula directamente en la BD.
        em.createNativeQuery("update sanitary_events set status = 'CANCELLED' where id = :id")
                .setParameter("id", cancelled.getId()).executeUpdate();
        em.clear();

        var events = queryRepository.findEventsByAnimalId(history.getAnimalId());

        assertEquals(Set.of(completed.getId(), scheduled.getId(), cancelled.getId()), ids(events));
        assertEquals(Set.of(SanitaryEventStatus.COMPLETED, SanitaryEventStatus.SCHEDULED, SanitaryEventStatus.CANCELLED),
                events.stream().map(SanitaryEvent::getStatus).collect(Collectors.toSet()));
    }

    @Test
    void doesNotMixEventsOfDifferentAnimals() {
        var cow = new ClinicalHistory(UUID.randomUUID());
        var bull = new ClinicalHistory(UUID.randomUUID());
        var cowEvent = cow.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2025, 5, 1, 9, 0), "vaca");
        var bullEvent = bull.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2042, 1, 10), "toro");
        persist(cow);
        persist(bull);

        assertEquals(Set.of(cowEvent.getId()), ids(queryRepository.findEventsByAnimalId(cow.getAnimalId())));
        assertEquals(Set.of(bullEvent.getId()), ids(queryRepository.findEventsByAnimalId(bull.getAnimalId())));
    }

    @Test
    void existingHistoryWithoutEventsReturnsEmpty() {
        var history = new ClinicalHistory(UUID.randomUUID());
        persist(history);

        assertTrue(queryRepository.findEventsByAnimalId(history.getAnimalId()).isEmpty());
    }

    @Test
    void animalWithoutClinicalHistoryReturnsEmpty() {
        assertTrue(queryRepository.findEventsByAnimalId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void serviceReturnsMixedEventsInAscendingChronologicalOrder() {
        var history = new ClinicalHistory(UUID.randomUUID());
        var future = history.scheduleEvent(SanitaryEventType.VACCINATION, LocalDate.of(2042, 6, 1), "futuro");
        var recent = history.registerEvent(SanitaryEventType.TREATMENT, LocalDateTime.of(2026, 3, 1, 9, 0), "reciente");
        var overdue = history.scheduleEvent(SanitaryEventType.CHECKUP, LocalDate.of(2026, 1, 15), "vencido");
        var old = history.registerEvent(SanitaryEventType.DISEASE, LocalDateTime.of(2025, 1, 1, 9, 0), "antiguo");
        persist(history);

        var result = service.handle(new GetClinicalHistoryQuery(history.getAnimalId()));

        assertEquals(List.of(old.getId(), overdue.getId(), recent.getId(), future.getId()),
                result.stream().map(SanitaryEvent::getId).toList());
    }
}
