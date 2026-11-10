package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Limites exactos de longitud: el maximo se acepta y un caracter mas se rechaza. */
class AnimalLimitsTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    private static Animal register(String tag, String name, String breed, String photoUrl) {
        return Animal.register(new RegisterAnimalCommand(tag, name, breed, null, LocalDate.of(2024, 1, 1), null,
                photoUrl, null), TODAY);
    }

    @Test
    void theMaximumLengthsAreAccepted() {
        var animal = register("t".repeat(50), "n".repeat(100), "b".repeat(60), "u".repeat(500));

        assertEquals(50, animal.getTag().length());
        assertEquals(100, animal.getName().length());
        assertEquals(60, animal.getBreed().length());
        assertEquals(500, animal.getPhotoUrl().length());
    }

    @Test
    void oneCharacterOverTheMaximumIsRejected() {
        assertThrows(InvalidAnimalDataException.class, () -> register("t".repeat(51), null, "Holstein", null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", "n".repeat(101), "Holstein", null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", null, "b".repeat(61), null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", null, "Holstein", "u".repeat(501)));
    }
}
