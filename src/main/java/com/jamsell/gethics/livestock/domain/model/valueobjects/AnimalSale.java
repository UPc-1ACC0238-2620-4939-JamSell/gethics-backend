package com.jamsell.gethics.livestock.domain.model.valueobjects;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;

import java.util.UUID;

/** Resultado de la venta: el animal vendido y el ingreso financiero que se registro. */
public record AnimalSale(Animal animal, UUID incomeTransactionId) {
}
