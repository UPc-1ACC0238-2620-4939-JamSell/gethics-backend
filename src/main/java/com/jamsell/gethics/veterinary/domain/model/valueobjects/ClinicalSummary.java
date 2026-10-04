package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import java.time.LocalDate;

public record ClinicalSummary(int totalEvents, LocalDate lastEventDate, String lastEventType) {
}
