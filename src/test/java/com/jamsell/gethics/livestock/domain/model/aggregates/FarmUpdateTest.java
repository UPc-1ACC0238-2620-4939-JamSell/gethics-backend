package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidFarmDataException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.FarmStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** US-32: editar nombre, ubicacion y tamano de una granja. */
class FarmUpdateTest {

    private final UUID ownerId = UUID.randomUUID();

    private Farm farm() {
        return Farm.register(new RegisterFarmCommand(ownerId, "Fundo Sur", "Jauja", new BigDecimal("12.5")));
    }

    @Test
    void updatesNameLocationAndSizeKeepingOwnerAndStatus() {
        var farm = farm();

        farm.update("  Fundo Norte ", " Huancayo ", new BigDecimal("30"));

        assertEquals("Fundo Norte", farm.getName());
        assertEquals("fundo norte", farm.getNormalizedName());
        assertEquals("Huancayo", farm.getLocation());
        assertEquals(new BigDecimal("30"), farm.getSizeHectares());
        assertEquals(ownerId, farm.getOwnerId());
        assertEquals(FarmStatus.ACTIVE, farm.getStatus());
    }

    @Test
    void aMissingSizeClearsTheRegisteredSize() {
        var farm = farm();

        farm.update("Fundo Sur", "Jauja", null);

        assertNull(farm.getSizeHectares());
    }

    @Test
    void invalidDataIsRejectedAndNothingChanges() {
        var farm = farm();

        assertThrows(InvalidFarmDataException.class, () -> farm.update(" ", "Jauja", null));
        assertThrows(InvalidFarmDataException.class, () -> farm.update("X", " ", null));
        assertThrows(InvalidFarmDataException.class, () -> farm.update("X".repeat(101), "Jauja", null));
        assertThrows(InvalidFarmDataException.class, () -> farm.update("X", "L".repeat(201), null));
        assertThrows(InvalidFarmDataException.class, () -> farm.update("X", "Jauja", BigDecimal.ZERO));

        assertEquals("Fundo Sur", farm.getName());
        assertEquals("Jauja", farm.getLocation());
        assertEquals(new BigDecimal("12.5"), farm.getSizeHectares());
    }
}
