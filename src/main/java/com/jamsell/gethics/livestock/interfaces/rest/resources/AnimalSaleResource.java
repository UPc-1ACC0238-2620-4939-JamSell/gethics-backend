package com.jamsell.gethics.livestock.interfaces.rest.resources;

import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** US-30: venta registrada: el animal (ya SOLD), el precio, la fecha y el id del ingreso creado en Finance. */
public record AnimalSaleResource(
        UUID animalId,
        AnimalStatus status,
        BigDecimal price,
        LocalDate saleDate,
        UUID incomeTransactionId) {
}
