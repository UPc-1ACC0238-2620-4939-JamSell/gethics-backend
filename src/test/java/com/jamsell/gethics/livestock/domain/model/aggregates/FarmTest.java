package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidFarmDataException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.FarmStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FarmTest {

    private final UUID ownerId = UUID.randomUUID();

    private Farm register(String name, String location, BigDecimal size) {
        return Farm.register(new RegisterFarmCommand(ownerId, name, location, size));
    }

    @Test
    void registersFarmWithAllFields() {
        var farm = register("  Fundo Sur ", " Jauja, Junin ", new BigDecimal("12.50"));

        assertEquals(ownerId, farm.getOwnerId());
        assertEquals("Fundo Sur", farm.getName());
        assertEquals("fundo sur", farm.getNormalizedName());
        assertEquals("Jauja, Junin", farm.getLocation());
        assertEquals(new BigDecimal("12.50"), farm.getSizeHectares());
        assertNotNull(farm.getCreatedAt());
    }

    @Test
    void newFarmIsActive() {
        assertEquals(FarmStatus.ACTIVE, register("Fundo", "Lima", null).getStatus());
    }

    @Test
    void sizeIsOptional() {
        assertNull(register("Fundo", "Lima", null).getSizeHectares());
    }

    @Test
    void namesDifferingOnlyInCaseOrOuterSpacesShareTheNormalizedName() {
        assertEquals(Farm.normalizeName("Fundo Sur"), Farm.normalizeName("  FUNDO SUR "));
    }

    @Test
    void rejectsZeroOrNegativeSize() {
        assertThrows(InvalidFarmDataException.class, () -> register("Fundo", "Lima", BigDecimal.ZERO));
        assertThrows(InvalidFarmDataException.class, () -> register("Fundo", "Lima", new BigDecimal("-1")));
    }

    @Test
    void rejectsBlankNameAndLocation() {
        assertThrows(InvalidFarmDataException.class, () -> register("  ", "Lima", null));
        assertThrows(InvalidFarmDataException.class, () -> register(null, "Lima", null));
        assertThrows(InvalidFarmDataException.class, () -> register("Fundo", " ", null));
        assertThrows(InvalidFarmDataException.class, () -> register("Fundo", null, null));
    }

    @Test
    void rejectsTooLongNameAndLocation() {
        assertThrows(InvalidFarmDataException.class, () -> register("x".repeat(101), "Lima", null));
        assertThrows(InvalidFarmDataException.class, () -> register("Fundo", "x".repeat(201), null));
        assertEquals(100, register("x".repeat(100), "Lima", null).getName().length());
    }

    @Test
    void rejectsMissingOwner() {
        assertThrows(NullPointerException.class,
                () -> Farm.register(new RegisterFarmCommand(null, "Fundo", "Lima", null)));
    }
}
