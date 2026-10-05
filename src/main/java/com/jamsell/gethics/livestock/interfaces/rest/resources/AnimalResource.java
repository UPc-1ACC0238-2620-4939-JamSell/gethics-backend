package com.jamsell.gethics.livestock.interfaces.rest.resources;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalResource(
        UUID id,
        UUID farmId,
        String tag,
        String qrCode,
        String name,
        String breed,
        AnimalSex sex,
        LocalDate birthDate,
        BigDecimal initialWeightKg,
        String photoUrl,
        AnimalStatus status) {
}
