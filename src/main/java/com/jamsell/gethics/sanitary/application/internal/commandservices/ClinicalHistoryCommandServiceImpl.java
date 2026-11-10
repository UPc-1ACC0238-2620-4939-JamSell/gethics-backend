package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventRequestConflictException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelAllScheduledEventsCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventCreation;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
public class ClinicalHistoryCommandServiceImpl implements ClinicalHistoryCommandService {

    private final ClinicalHistoryRepository repository;
    // Mismo Clock (zona configurada) que el job de recordatorios: "hoy" coincide para programar y para avisar.
    private final Clock clock;
    // US-35: la creacion idempotente demarca su transaccion a mano (ver createIdempotently).
    private final TransactionTemplate transactions;

    public ClinicalHistoryCommandServiceImpl(ClinicalHistoryRepository repository, Clock clock,
                                             PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.clock = clock;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public SanitaryEventCreation handle(RegisterSanitaryEventCommand command) {
        return createIdempotently(command.clientRequestId(), existing -> existing.matches(command), () -> {
            var history = repository.findByAnimalId(command.animalId())
                    .orElseGet(() -> new ClinicalHistory(command.animalId()));
            history.registerEvent(command.type(), command.occurredAt(), command.description(), command.clientRequestId());
            // El evento recien registrado es el ultimo; se toma de la instancia persistida (con id asignado).
            return repository.save(history).getEvents().getLast();
        });
    }

    @Override
    public SanitaryEventCreation handle(ScheduleSanitaryEventCommand command) {
        return createIdempotently(command.clientRequestId(), existing -> existing.matches(command), () -> {
            var history = repository.findByAnimalId(command.animalId())
                    .orElseGet(() -> new ClinicalHistory(command.animalId()));
            history.scheduleEvent(command.type(), command.scheduledDate(), command.description(), LocalDate.now(clock),
                    command.clientRequestId());
            return repository.save(history).getEvents().getLast();
        });
    }

    /**
     * US-35, mismo patron que US-19 (CareRecordCommandServiceImpl): sin clientRequestId se crea siempre; con el, un
     * reintento con los mismos datos devuelve el evento existente y uno con otros datos es 409.
     * <p>
     * La creacion va en su propia transaccion y NO en un {@code @Transactional} del metodo: si la UNIQUE salta por una
     * peticion concurrente, PostgreSQL aborta esa transaccion, asi que la relectura del ganador debe hacerse fuera de ella.
     */
    private SanitaryEventCreation createIdempotently(UUID clientRequestId, Predicate<SanitaryEvent> sameRequest,
                                                     Supplier<SanitaryEvent> create) {
        if (clientRequestId != null) {
            var existing = repository.findEventByClientRequestId(clientRequestId);
            if (existing.isPresent()) {
                return resolveExisting(existing.get(), clientRequestId, sameRequest);
            }
        }
        try {
            return new SanitaryEventCreation(transactions.execute(status -> create.get()), true);
        } catch (DataIntegrityViolationException exception) {
            if (clientRequestId == null) {
                throw exception;
            }
            var concurrent = repository.findEventByClientRequestId(clientRequestId).orElseThrow(() -> exception);
            return resolveExisting(concurrent, clientRequestId, sameRequest);
        }
    }

    private static SanitaryEventCreation resolveExisting(SanitaryEvent existing, UUID clientRequestId,
                                                         Predicate<SanitaryEvent> sameRequest) {
        if (!sameRequest.test(existing)) {
            throw new SanitaryEventRequestConflictException(clientRequestId);
        }
        return new SanitaryEventCreation(existing, false);
    }

    @Override
    @Transactional
    public SanitaryEvent handle(CompleteScheduledEventCommand command) {
        var history = repository.findByAnimalId(command.animalId())
                .orElseThrow(SanitaryEventNotFoundException::new);
        var event = history.completeScheduledEvent(command.eventId(), command.occurredAt(), command.description(),
                LocalDate.now(clock));
        repository.save(history);
        return event;
    }

    /** US-33: un animal sin historial no tiene eventos que cancelar (404, como al completar). */
    @Override
    @Transactional
    public SanitaryEvent handle(CancelScheduledEventCommand command) {
        var history = repository.findByAnimalId(command.animalId())
                .orElseThrow(SanitaryEventNotFoundException::new);
        var event = history.cancelScheduledEvent(command.eventId());
        repository.save(history);
        return event;
    }

    /** Un animal sin historial no tiene nada que cancelar. Corre dentro de la transaccion de quien lo dispara. */
    @Override
    @Transactional
    public int handle(CancelAllScheduledEventsCommand command) {
        return repository.findByAnimalId(command.animalId()).map(history -> {
            var cancelled = history.cancelAllScheduledEvents();
            if (cancelled > 0) {
                repository.save(history);
            }
            return cancelled;
        }).orElse(0);
    }
}
