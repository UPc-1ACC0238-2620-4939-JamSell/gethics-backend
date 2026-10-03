package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.valueobjects.SanitaryEventStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;

/**
 * Politica de ordenamiento de la consulta del historial clinico (ascendente: del mas antiguo al mas reciente).
 * La "fecha efectiva" que combina {@code occurredAt} y {@code scheduledDate} NO es un concepto del dominio: es una
 * politica de esta consulta, por eso vive en Application y {@link SanitaryEvent} solo expone sus datos reales.
 * <ul>
 *   <li>COMPLETED: {@code occurredAt}.</li>
 *   <li>SCHEDULED y CANCELLED: {@code scheduledDate} a las 00:00.</li>
 *   <li>Fallback tecnico: si esa fecha es nula (dato heredado inconsistente), {@code createdAt} en la zona del servidor,
 *       para no lanzar NullPointerException. No se corrigen ni validan datos aqui.</li>
 *   <li>Desempate: {@code createdAt} y luego {@code id}. Los nulos van primero, de modo que el orden siempre es total.</li>
 * </ul>
 */
final class ClinicalHistoryChronologicalComparator implements Comparator<SanitaryEvent> {

    static final ClinicalHistoryChronologicalComparator INSTANCE = new ClinicalHistoryChronologicalComparator();

    private static final Comparator<SanitaryEvent> ORDER = Comparator
            .comparing(ClinicalHistoryChronologicalComparator::effectiveDateTime, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(SanitaryEvent::getCreatedAt, Comparator.nullsFirst(Comparator.<Instant>naturalOrder()))
            .thenComparing(SanitaryEvent::getId, Comparator.nullsFirst(Comparator.naturalOrder()));

    private ClinicalHistoryChronologicalComparator() {
    }

    @Override
    public int compare(SanitaryEvent a, SanitaryEvent b) {
        return ORDER.compare(a, b);
    }

    static LocalDateTime effectiveDateTime(SanitaryEvent event) {
        var date = event.getStatus() == SanitaryEventStatus.COMPLETED
                ? event.getOccurredAt()
                : event.getScheduledDate() == null ? null : event.getScheduledDate().atStartOfDay();
        if (date != null) {
            return date;
        }
        return event.getCreatedAt() == null ? null : LocalDateTime.ofInstant(event.getCreatedAt(), ZoneId.systemDefault());
    }
}
