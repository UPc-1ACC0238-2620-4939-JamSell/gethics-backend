package com.jamsell.gethics.sanitary.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.sanitary.application.internal.commandservices.ClinicalHistoryCommandServiceImpl;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.infrastructure.configuration.SanitaryReminderConfiguration;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/**
 * US-35: el mismo request (mismo clientRequestId) llega a la vez desde varios hilos contra PostgreSQL real. Sin
 * transaccion de test (cada hilo confirma la suya, como peticiones HTTP distintas), asi que el test borra lo que
 * sembro. Gane quien gane, el resto recibe el mismo evento: sin duplicados ni errores.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ClinicalHistoryCommandServiceImpl.class, ClinicalHistoryRepositoryImpl.class, SanitaryReminderConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SanitaryEventIdempotencyConcurrencyTest {

    private static final int REQUESTS = 8;

    @Autowired
    ClinicalHistoryCommandServiceImpl service;
    @Autowired
    ClinicalHistoryRepository repository;
    @Autowired
    EntityManager em;
    @Autowired
    PlatformTransactionManager transactionManager;

    private final UUID animalId = UUID.randomUUID();

    @AfterEach
    void deleteSeededRows() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            em.createNativeQuery("delete from sanitary_events where clinical_history_id in "
                            + "(select id from clinical_histories where animal_id = :animalId)")
                    .setParameter("animalId", animalId).executeUpdate();
            em.createNativeQuery("delete from clinical_histories where animal_id = :animalId")
                    .setParameter("animalId", animalId).executeUpdate();
        });
    }

    @Test
    void simultaneousIdenticalRequestsCreateOneEventAndAllReturnIt() throws Exception {
        // Historial ya existente: la carrera es solo por el clientRequestId del evento.
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> repository.save(new ClinicalHistory(animalId)));
        var command = new RegisterSanitaryEventCommand(animalId, SanitaryEventType.VACCINATION,
                LocalDate.now().minusDays(1).atTime(8, 0), "Aftosa", UUID.randomUUID());
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(REQUESTS);
        try {
            List<Future<SanitaryEventCreation>> calls = new ArrayList<>();
            for (int i = 0; i < REQUESTS; i++) {
                calls.add(pool.submit((Callable<SanitaryEventCreation>) () -> {
                    start.await();
                    return service.handle(command);
                }));
            }
            start.countDown();

            List<SanitaryEventCreation> results = new ArrayList<>();
            for (var call : calls) {
                results.add(call.get()); // una excepcion en cualquier hilo hace fallar el test
            }

            assertEquals(1, results.stream().filter(SanitaryEventCreation::created).count());
            assertEquals(1, results.stream().map(r -> r.event().getId()).distinct().count());
            var stored = new TransactionTemplate(transactionManager).execute(s -> em
                    .createQuery("select count(e) from SanitaryEvent e where e.clientRequestId = :id", Long.class)
                    .setParameter("id", command.clientRequestId()).getSingleResult());
            assertEquals(1L, stored);
        } finally {
            pool.shutdownNow();
        }
    }
}
