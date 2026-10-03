package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.model.aggregates.ClinicalHistory;
import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryRepository;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicalHistoryCommandServiceImpl implements ClinicalHistoryCommandService {

    private final ClinicalHistoryRepository repository;

    public ClinicalHistoryCommandServiceImpl(ClinicalHistoryRepository repository) {
        this.repository = repository;
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
}
