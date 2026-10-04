package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.exceptions.SanitaryEventNotFoundException;
import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.CompleteScheduledEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class ClinicalHistoryCommandServiceImpl implements ClinicalHistoryCommandService {

    private final ClinicalHistoryRepository repository;
    // Mismo Clock (zona configurada) que el job de recordatorios: "hoy" coincide para programar y para avisar.
    private final Clock clock;

    public ClinicalHistoryCommandServiceImpl(ClinicalHistoryRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SanitaryEvent handle(RegisterSanitaryEventCommand command) {
        var history = repository.findByAnimalId(command.animalId())
                .orElseGet(() -> new ClinicalHistory(command.animalId()));
        history.registerEvent(command.type(), command.occurredAt(), command.description());
        var saved = repository.save(history);
        // El evento recien registrado es el ultimo; se toma de la instancia persistida (con id asignado).
        return saved.getEvents().getLast();
    }

    @Override
    @Transactional
    public SanitaryEvent handle(ScheduleSanitaryEventCommand command) {
        var history = repository.findByAnimalId(command.animalId())
                .orElseGet(() -> new ClinicalHistory(command.animalId()));
        history.scheduleEvent(command.type(), command.scheduledDate(), command.description(), LocalDate.now(clock));
        return repository.save(history).getEvents().getLast();
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
}
