package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.queryservices.SanitaryCalendarQueryServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
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
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Corre contra el PostgreSQL configurado en application-dev.yaml (en CI, el servicio postgres:16 del workflow).
 * Cada test hace rollback y solo verifica las filas que el propio test sembro (por id): la base puede contener
 * otros datos del mismo mes y no se borra ni trunca nada.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SanitaryCalendarQueryServiceImpl.class, SanitaryCalendarQueryRepositoryImpl.class, ClinicalHistoryRepositoryImpl.class})
class SanitaryCalendarPersistenceTest {

    private static final LocalDate MARCH = LocalDate.of(2041, 3, 1);

    @Autowired
    SanitaryCalendarQueryServiceImpl service;
    @Autowired
    ClinicalHistoryRepository histories;
    @Autowired
    EntityManager em;

    private ClinicalHistory history() {
        return new ClinicalHistory(UUID.randomUUID());
    }

    /** Persiste el historial y descarta el contexto para que la consulta lea de la base. */
    private ClinicalHistory persist(ClinicalHistory history) {
        var saved = histories.save(history);
        em.clear();
        return saved;
    }

    private List<SanitaryEvent> calendar(int year, int month) {
        return service.handle(GetSanitaryCalendarQuery.of(year, month));
    }

    /** Ids devueltos por el calendario, restringidos a los sembrados por el test y en el orden devuelto. */
    private List<UUID> returnedAmong(List<SanitaryEvent> returned, SanitaryEvent... seeded) {
        var seededIds = java.util.Arrays.stream(seeded).map(SanitaryEvent::getId).collect(Collectors.toSet());
        return returned.stream().map(SanitaryEvent::getId).filter(seededIds::contains).toList();
    }

    @Test
    void returnsSeededScheduledEventsOfTheMonthSortedByDateAcrossAnimals() {
        var cow = history();
        var bull = history();
        var late = cow.scheduleEvent(SanitaryEventType.VACCINATION, MARCH.plusDays(19), "Aftosa");
        var early = cow.scheduleEvent(SanitaryEventType.CHECKUP, MARCH.plusDays(2), null);
        var middle = bull.scheduleEvent(SanitaryEventType.TREATMENT, MARCH.plusDays(10), "Ivermectina");
        persist(cow);
        persist(bull);

        var returned = calendar(2041, 3);

        assertEquals(List.of(early.getId(), middle.getId(), late.getId()), returnedAmong(returned, late, early, middle));
        var byId = returned.stream().collect(Collectors.toMap(SanitaryEvent::getId, e -> e));
        assertEquals(cow.getAnimalId(), byId.get(early.getId()).getClinicalHistory().getAnimalId());
        assertEquals(bull.getAnimalId(), byId.get(middle.getId()).getClinicalHistory().getAnimalId());
        assertEquals(cow.getAnimalId(), byId.get(late.getId()).getClinicalHistory().getAnimalId());
        assertTrue(returned.stream().allMatch(e -> e.getStatus() == SanitaryEventStatus.SCHEDULED));
    }

    @Test
    void includesFirstAndLastDayAndExcludesAdjacentMonths() {
        var h = history();
        var february = h.scheduleEvent(SanitaryEventType.OTHER, MARCH.minusDays(1), "febrero");
        var firstDay = h.scheduleEvent(SanitaryEventType.OTHER, MARCH, "primer dia");
        var lastDay = h.scheduleEvent(SanitaryEventType.OTHER, MARCH.withDayOfMonth(31), "ultimo dia");
        var april = h.scheduleEvent(SanitaryEventType.OTHER, MARCH.plusMonths(1), "abril");
        persist(h);

        var returned = calendar(2041, 3);

        assertEquals(List.of(firstDay.getId(), lastDay.getId()),
                returnedAmong(returned, february, firstDay, lastDay, april));
    }

    @Test
    void excludesCompletedEvents() {
        var h = history();
        var completed = h.registerEvent(SanitaryEventType.VACCINATION, LocalDateTime.of(2020, 3, 10, 9, 0), "realizado");
        var scheduled = h.scheduleEvent(SanitaryEventType.CHECKUP, MARCH.plusDays(5), "programado");
        persist(h);

        var returned = calendar(2041, 3);

        assertEquals(List.of(scheduled.getId()), returnedAmong(returned, completed, scheduled));
    }

    @Test
    void excludesCancelledEvents() {
        var h = history();
        var cancelled = h.scheduleEvent(SanitaryEventType.CHECKUP, MARCH.plusDays(5), "cancelado");
        var active = h.scheduleEvent(SanitaryEventType.CHECKUP, MARCH.plusDays(6), "vigente");
        persist(h);
        // No existe comportamiento de cancelacion (fuera de alcance): se simula directamente en la BD.
        em.createNativeQuery("update sanitary_events set status = 'CANCELLED' where id = :id")
                .setParameter("id", cancelled.getId()).executeUpdate();
        em.clear();

        var returned = calendar(2041, 3);

        assertEquals(List.of(active.getId()), returnedAmong(returned, cancelled, active));
    }

    @Test
    void scheduledEventIsPersistedWithoutOccurredAt() {
        var h = history();
        var scheduled = h.scheduleEvent(SanitaryEventType.DISEASE, MARCH.plusDays(1), "x");
        persist(h);

        var found = calendar(2041, 3).stream().filter(e -> e.getId().equals(scheduled.getId())).findFirst().orElseThrow();

        assertNull(found.getOccurredAt());
        assertEquals(MARCH.plusDays(1), found.getScheduledDate());
    }

    @Test
    void technicalLimitsOfThePeriodRoundTripThroughPostgres() {
        var h = history();
        var atMin = h.scheduleEvent(SanitaryEventType.OTHER, LocalDate.of(-4712, 1, 1), "limite inferior");
        var atMax = h.scheduleEvent(SanitaryEventType.OTHER, LocalDate.of(5_874_896, 12, 31), "limite superior");
        persist(h);

        assertEquals(List.of(atMin.getId()), returnedAmong(calendar(-4712, 1), atMin, atMax));
        assertEquals(List.of(atMax.getId()), returnedAmong(calendar(5_874_896, 12), atMin, atMax));
    }
}
