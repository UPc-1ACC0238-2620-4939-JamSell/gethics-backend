package com.jamsell.gethics.sanitary.interfaces.acl;

import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.ScheduleSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetCompletedEventsByAnimalsQuery;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * TS07: fachada de Sanitary para otros bounded contexts (hoy, Veterinary). Permite agregar una atencion veterinaria
 * al historial clinico del animal, programar su proximo control y resumir ese historial sin exponer entidades de
 * Sanitary.
 */
@Service
public class SanitaryContextFacade {

    private final ClinicalHistoryCommandService commandService;
    private final ClinicalHistoryQueryService queryService;

    public SanitaryContextFacade(ClinicalHistoryCommandService commandService, ClinicalHistoryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    /**
     * Agrega una atencion veterinaria (TREATMENT, ya realizada) al historial del animal. Es idempotente por
     * {@code clientRequestId} (US-35): reintentar la misma atencion no duplica el evento.
     */
    public void registerVeterinaryCare(UUID animalId, UUID clientRequestId, LocalDateTime occurredAt,
                                       String description) {
        commandService.handle(new RegisterSanitaryEventCommand(animalId, SanitaryEventType.TREATMENT, occurredAt,
                description, clientRequestId));
    }

    /**
     * US-19, "proximos controles": programa un CHECKUP futuro en el calendario del animal. Idempotente por
     * {@code followUpRequestId}. La fecha no puede ser anterior a hoy (lo valida Sanitary).
     */
    public void scheduleVeterinaryFollowUp(UUID animalId, UUID followUpRequestId, LocalDate date, String description) {
        commandService.handle(new ScheduleSanitaryEventCommand(animalId, SanitaryEventType.CHECKUP, date, description,
                followUpRequestId));
    }

    /** Eventos COMPLETED del animal: total y el mas reciente. Vacio si el animal aun no tiene eventos aplicados. */
    public Optional<ClinicalSummaryView> findClinicalSummary(UUID animalId) {
        return Optional.ofNullable(findClinicalSummaries(Set.of(animalId)).get(animalId));
    }

    /** Resumen clinico de varios animales con UNA consulta. Los animales sin eventos aplicados no aparecen. */
    public Map<UUID, ClinicalSummaryView> findClinicalSummaries(Collection<UUID> animalIds) {
        var byAnimal = queryService.handle(new GetCompletedEventsByAnimalsQuery(Set.copyOf(animalIds))).stream()
                .collect(Collectors.groupingBy(event -> event.getClinicalHistory().getAnimalId()));
        var result = new HashMap<UUID, ClinicalSummaryView>();
        byAnimal.forEach((animalId, events) -> events.stream()
                .max(Comparator.comparing(SanitaryEvent::getOccurredAt))
                .ifPresent(last -> result.put(animalId, new ClinicalSummaryView(events.size(),
                        last.getOccurredAt().toLocalDate(), last.getType().name()))));
        return result;
    }
}
