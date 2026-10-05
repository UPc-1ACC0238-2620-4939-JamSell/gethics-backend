package com.jamsell.gethics.livestock.domain.model.commands;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegisterAnimalCommand(
        String tag,
        String name,
        String breed,
        AnimalSex sex,
        LocalDate birthDate,
        BigDecimal initialWeightKg,
        String photoUrl,
        UUID farmId) {
}
