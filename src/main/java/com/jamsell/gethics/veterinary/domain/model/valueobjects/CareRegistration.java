package com.jamsell.gethics.veterinary.domain.model.valueobjects;

import com.jamsell.gethics.veterinary.domain.model.aggregates.CareRecord;

public record CareRegistration(CareRecord record, boolean created) {
}
