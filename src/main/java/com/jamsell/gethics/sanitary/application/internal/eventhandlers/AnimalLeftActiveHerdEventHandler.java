package com.jamsell.gethics.sanitary.application.internal.eventhandlers;

import com.jamsell.gethics.livestock.domain.model.events.AnimalLeftActiveHerdEvent;
import com.jamsell.gethics.sanitary.domain.model.commands.CancelAllScheduledEventsCommand;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Cuando un animal sale del hato activo (baja US-08 o venta US-30) sus eventos programados se cancelan: asi salen del
 * calendario sanitario y dejan de generar recordatorios. El historial clinico (eventos completados) se conserva.
 * Corre dentro de la transaccion de Livestock: si falla, la baja o venta tampoco se guarda.
 */
@Component
public class AnimalLeftActiveHerdEventHandler {

    private final ClinicalHistoryCommandService commandService;

    public AnimalLeftActiveHerdEventHandler(ClinicalHistoryCommandService commandService) {
        this.commandService = commandService;
    }

    @EventListener
    public void on(AnimalLeftActiveHerdEvent event) {
        commandService.handle(new CancelAllScheduledEventsCommand(event.animalId()));
    }
}
