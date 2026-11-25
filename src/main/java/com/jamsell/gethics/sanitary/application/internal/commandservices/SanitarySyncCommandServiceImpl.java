package com.jamsell.gethics.sanitary.application.internal.commandservices;

import com.jamsell.gethics.sanitary.domain.model.commands.RegisterSanitaryEventCommand;
import com.jamsell.gethics.sanitary.domain.model.commands.SyncOfflineSanitaryEventItem;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventType;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryCommandService;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SyncSanitaryEventsResultResource;
import com.jamsell.gethics.sanitary.interfaces.rest.resources.SyncSanitaryEventsResultResource.ItemSyncResult;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SanitarySyncCommandServiceImpl {

    private final ClinicalHistoryCommandService clinicalHistoryCommandService;

    public SanitarySyncCommandServiceImpl(ClinicalHistoryCommandService clinicalHistoryCommandService) {
        this.clinicalHistoryCommandService = clinicalHistoryCommandService;
    }

    public SyncSanitaryEventsResultResource syncBatch(List<SyncOfflineSanitaryEventItem> items) {
        List<ItemSyncResult> results = new ArrayList<>();
        int synced = 0;
        int failed = 0;

        for (SyncOfflineSanitaryEventItem item : items) {
            try {
                UUID requestId = UUID.fromString(item.clientEventId());
                SanitaryEventType eventType = SanitaryEventType.valueOf(item.eventType().trim().toUpperCase());

                var command = new RegisterSanitaryEventCommand(
                        item.animalId(),
                        eventType,
                        item.eventDate(),
                        item.description(),
                        requestId
                );
                var creation = clinicalHistoryCommandService.handle(command);
                synced++;

                String status = creation.created() ? "SYNCED" : "ALREADY_SYNCED";
                String message = creation.created()
                        ? "Sincronizado correctamente"
                        : "Evento ya sincronizado previamente";

                results.add(new ItemSyncResult(
                        item.clientEventId(),
                        null,
                        status,
                        message
                ));
            } catch (Exception ex) {
                failed++;
                results.add(new ItemSyncResult(
                        item.clientEventId(),
                        null,
                        "FAILED",
                        ex.getMessage()
                ));
            }
        }

        return new SyncSanitaryEventsResultResource(items.size(), synced, failed, results);
    }
}
