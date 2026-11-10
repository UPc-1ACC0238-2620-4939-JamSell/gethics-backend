package com.jamsell.gethics.livestock.application.internal.outboundservices;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Puerto hacia Finance: registra el ingreso de la venta de un animal y devuelve el id del movimiento. */
public interface SaleIncomeRegistrar {

    UUID registerIncome(UUID ownerId, BigDecimal amount, LocalDate saleDate, String description);
}
