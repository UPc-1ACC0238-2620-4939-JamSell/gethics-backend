package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotActiveException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalSaleException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** US-30 (venta) y la regla "solo un animal ACTIVE admite cambios" que cierra la baja logica (US-08). */
class AnimalSaleAndStateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 10);

    private Animal activeAnimal() {
        return Animal.register(new RegisterAnimalCommand("A-1", "Lola", "Holstein", null, LocalDate.of(2024, 1, 1),
                null, null, null), TODAY);
    }

    private UpdateAnimalCommand update() {
        return new UpdateAnimalCommand(null, "Nueva", "Jersey", null, LocalDate.of(2024, 1, 1), null, null, null);
    }

    @Test
    void sellingMarksTheAnimalSoldWithPriceAndDate() {
        var animal = activeAnimal();

        animal.sell(new BigDecimal("1500.50"), TODAY, TODAY);

        assertEquals(AnimalStatus.SOLD, animal.getStatus());
        assertEquals(new BigDecimal("1500.50"), animal.getSalePrice());
        assertEquals(TODAY, animal.getSoldOn());
    }

    @Test
    void aSaleDatedInThePastIsAllowed() {
        var animal = activeAnimal();

        animal.sell(BigDecimal.TEN, TODAY.minusDays(30), TODAY);

        assertEquals(TODAY.minusDays(30), animal.getSoldOn());
    }

    @Test
    void zeroOrNegativeOrMissingPriceIsRejectedAndTheAnimalStaysActive() {
        var animal = activeAnimal();

        assertThrows(InvalidAnimalSaleException.class, () -> animal.sell(BigDecimal.ZERO, TODAY, TODAY));
        assertThrows(InvalidAnimalSaleException.class, () -> animal.sell(new BigDecimal("-1"), TODAY, TODAY));
        assertThrows(InvalidAnimalSaleException.class, () -> animal.sell(null, TODAY, TODAY));

        assertEquals(AnimalStatus.ACTIVE, animal.getStatus());
        assertNull(animal.getSalePrice());
        assertNull(animal.getSoldOn());
    }

    @Test
    void aFutureOrMissingSaleDateIsRejected() {
        var animal = activeAnimal();

        assertThrows(InvalidAnimalSaleException.class, () -> animal.sell(BigDecimal.TEN, TODAY.plusDays(1), TODAY));
        assertThrows(InvalidAnimalSaleException.class, () -> animal.sell(BigDecimal.TEN, null, TODAY));
        assertEquals(AnimalStatus.ACTIVE, animal.getStatus());
    }

    @Test
    void anAnimalAlreadySoldCannotBeSoldAgain() {
        var animal = activeAnimal();
        animal.sell(BigDecimal.TEN, TODAY, TODAY);

        var ex = assertThrows(AnimalNotActiveException.class, () -> animal.sell(BigDecimal.TEN, TODAY, TODAY));

        assertEquals("El animal ya fue vendido.", ex.getMessage());
    }

    @Test
    void aDeactivatedAnimalCannotBeSold() {
        var animal = activeAnimal();
        animal.deactivate();

        assertThrows(AnimalNotActiveException.class, () -> animal.sell(BigDecimal.TEN, TODAY, TODAY));
    }

    @Test
    void aSoldAnimalCannotBeDeactivated() {
        var animal = activeAnimal();
        animal.sell(BigDecimal.TEN, TODAY, TODAY);

        var ex = assertThrows(AnimalNotActiveException.class, animal::deactivate);

        assertEquals("El animal ya fue vendido.", ex.getMessage());
        assertEquals(AnimalStatus.SOLD, animal.getStatus());
    }

    @Test
    void aDeactivatedAnimalCannotBeEdited() {
        var animal = activeAnimal();
        animal.deactivate();

        assertThrows(AnimalNotActiveException.class, () -> animal.updateDetails(update(), TODAY));
        assertEquals("Lola", animal.getName());
    }

    @Test
    void aDeactivatedOrSoldAnimalCannotChangeFarm() {
        var deactivated = activeAnimal();
        deactivated.deactivate();
        var sold = activeAnimal();
        sold.sell(BigDecimal.TEN, TODAY, TODAY);

        assertThrows(AnimalNotActiveException.class, () -> deactivated.assignToFarm(UUID.randomUUID()));
        assertThrows(AnimalNotActiveException.class, () -> sold.assignToFarm(UUID.randomUUID()));
    }

    @Test
    void theRejectionMessageNamesTheRealState() {
        assertEquals("El animal ya esta dado de baja.", new AnimalNotActiveException().getMessage());
        assertEquals("El animal ya esta dado de baja.",
                new AnimalNotActiveException(AnimalStatus.INACTIVE).getMessage());
        assertEquals("El animal ya fue vendido.", new AnimalNotActiveException(AnimalStatus.SOLD).getMessage());
        assertEquals("El animal esta registrado como fallecido.",
                new AnimalNotActiveException(AnimalStatus.DECEASED).getMessage());
    }
}
