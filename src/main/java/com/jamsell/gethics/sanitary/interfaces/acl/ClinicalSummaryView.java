package com.jamsell.gethics.sanitary.interfaces.acl;

import java.time.LocalDate;

/** Resumen del historial clinico de un animal para otros bounded contexts. */
public record ClinicalSummaryView(int totalEvents, LocalDate lastEventDate, String lastEventType) {
}
