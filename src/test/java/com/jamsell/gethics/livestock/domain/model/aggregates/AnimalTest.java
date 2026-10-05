package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalWeightException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnimalTest {

    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);

    private Animal register(String tag, String breed, LocalDate birthDate, BigDecimal weight, String photoUrl) {
        return Animal.register(new RegisterAnimalCommand(tag, null, breed, null, birthDate, weight, photoUrl, null), TODAY);
    }

    @Test
    void registersAnimalWithAllFields() {
        var farmId = UUID.randomUUID();
        var command = new RegisterAnimalCommand("mx-00123", " Luna ", "Holstein", AnimalSex.FEMALE,
                TODAY.minusYears(2), new BigDecimal("420.50"), "https://img/1.jpg", farmId);

        var animal = Animal.register(command, TODAY);

        assertEquals("MX-00123", animal.getTag());
        assertEquals("Luna", animal.getName());
        assertEquals("Holstein", animal.getBreed());
        assertEquals(AnimalSex.FEMALE, animal.getSex());
        assertEquals(TODAY.minusYears(2), animal.getBirthDate());
        assertEquals(new BigDecimal("420.50"), animal.getInitialWeightKg());
        assertEquals("https://img/1.jpg", animal.getPhotoUrl());
        assertEquals(farmId, animal.getFarmId());
        assertNotNull(animal.getCreatedAt());
    }

    @Test
    void newAnimalIsActive() {
        assertEquals(AnimalStatus.ACTIVE, register("A-1", "Jersey", TODAY, null, null).getStatus());
    }

    @Test
    void generatesAUniqueQrCodeForEachAnimal() {
        var first = register("A-1", "Jersey", TODAY, null, null);
        var second = register("A-2", "Jersey", TODAY, null, null);

        assertTrue(first.getQrCode().matches("GTH-[0-9A-F]{12}"));
        assertNotEquals(first.getQrCode(), second.getQrCode());
    }

    @Test
    void optionalFieldsAreNullWhenMissingOrBlank() {
        var animal = register("A-1", "Jersey", TODAY, null, "  ");

        assertNull(animal.getInitialWeightKg());
        assertNull(animal.getPhotoUrl());
        assertNull(animal.getName());
        assertNull(animal.getSex());
        assertNull(animal.getFarmId());
    }

    @Test
    void tagIsTrimmedAndUppercased() {
        assertEquals("MX-00123", Animal.normalizeTag("  mx-00123 "));
    }

    @Test
    void birthDateOfTodayIsValid() {
        assertEquals(TODAY, register("A-1", "Jersey", TODAY, null, null).getBirthDate());
    }

    @Test
    void rejectsFutureBirthDate() {
        assertThrows(FutureBirthDateException.class, () -> register("A-1", "Jersey", TODAY.plusDays(1), null, null));
    }

    @Test
    void rejectsZeroOrNegativeWeight() {
        assertThrows(InvalidAnimalWeightException.class, () -> register("A-1", "Jersey", TODAY, BigDecimal.ZERO, null));
        assertThrows(InvalidAnimalWeightException.class, () -> register("A-1", "Jersey", TODAY, new BigDecimal("-3"), null));
    }

    @Test
    void rejectsBlankTagAndBreed() {
        assertThrows(InvalidAnimalDataException.class, () -> register("  ", "Jersey", TODAY, null, null));
        assertThrows(InvalidAnimalDataException.class, () -> register(null, "Jersey", TODAY, null, null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", " ", TODAY, null, null));
    }

    @Test
    void rejectsTooLongTagBreedNameAndPhotoUrl() {
        assertThrows(InvalidAnimalDataException.class, () -> register("x".repeat(51), "Jersey", TODAY, null, null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", "x".repeat(61), TODAY, null, null));
        assertThrows(InvalidAnimalDataException.class, () -> register("A-1", "Jersey", TODAY, null, "x".repeat(501)));
        var longName = new RegisterAnimalCommand("A-1", "x".repeat(101), "Jersey", null, TODAY, null, null, null);
        assertThrows(InvalidAnimalDataException.class, () -> Animal.register(longName, TODAY));
    }
}
