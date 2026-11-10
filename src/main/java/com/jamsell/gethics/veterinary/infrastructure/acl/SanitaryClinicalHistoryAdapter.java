package com.jamsell.gethics.veterinary.infrastructure.acl;

import com.jamsell.gethics.sanitary.interfaces.acl.SanitaryContextFacade;
import com.jamsell.gethics.veterinary.application.internal.outboundservices.ClinicalHistoryWriter;
import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * TS07: agrega la atencion veterinaria al historial clinico del animal por la fachada de Sanitary. Usa el
 * {@code clientRequestId} de la atencion como id de idempotencia del evento: un reintento (por ejemplo cuando
 * {@code history_synced} sigue en false) no duplica el evento en el historial del ganadero. Si la atencion trae
 * "proximo control" en el futuro, tambien lo programa como CHECKUP en el calendario sanitario (US-19).
 */
@Component
public class SanitaryClinicalHistoryAdapter implements ClinicalHistoryWriter {

    // sanitary_events.description admite 1000 caracteres.
    static final int DESCRIPTION_MAX_LENGTH = 1000;

    private final SanitaryContextFacade sanitary;
    private final Clock clock;

    public SanitaryClinicalHistoryAdapter(SanitaryContextFacade sanitary, Clock clock) {
        this.sanitary = sanitary;
        this.clock = clock;
    }

    @Override
    public void append(CareRecord record) {
        // La atencion admite hasta 5 minutos "en el futuro" (relojes desfasados), pero Sanitary no admite fechas
        // futuras para eventos ya ocurridos: se ajusta a "ahora". Microsegundos = precision de la base de datos, para
        // que un reintento compare igual con lo ya guardado.
        var occurred = record.getOccurredAt().isAfter(clock.instant()) ? clock.instant() : record.getOccurredAt();
        var occurredAt = LocalDateTime.ofInstant(occurred, clock.getZone()).truncatedTo(ChronoUnit.MICROS);
        sanitary.registerVeterinaryCare(record.getPatientId(), record.getClientRequestId(), occurredAt,
                truncate(describe(record)));
        scheduleFollowUp(record);
    }

    private void scheduleFollowUp(CareRecord record) {
        var nextControl = record.getNextControlDate();
        // Una atencion sincronizada tarde puede traer un control que ya paso: no hay nada que programar.
        if (nextControl == null || nextControl.isBefore(LocalDate.now(clock))) {
            return;
        }
        sanitary.scheduleVeterinaryFollowUp(record.getPatientId(), followUpRequestId(record), nextControl,
                truncate("Control programado en la atencion veterinaria. Diagnostico: " + record.getDiagnosis() + "."));
    }

    /** Id estable derivado de la atencion: reintentar no programa el control dos veces. */
    static UUID followUpRequestId(CareRecord record) {
        return UUID.nameUUIDFromBytes(
                ("veterinary-care-follow-up:" + record.getClientRequestId()).getBytes(StandardCharsets.UTF_8));
    }

    static String describe(CareRecord record) {
        var text = new StringBuilder("Atencion veterinaria. Diagnostico: ").append(record.getDiagnosis())
                .append(". Tratamiento: ").append(record.getTreatment()).append('.');
        if (record.getNextControlDate() != null) {
            text.append(" Proximo control: ").append(record.getNextControlDate()).append('.');
        }
        return text.toString();
    }

    private static String truncate(String text) {
        return text.length() <= DESCRIPTION_MAX_LENGTH ? text : text.substring(0, DESCRIPTION_MAX_LENGTH - 3) + "...";
    }
}
