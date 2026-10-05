package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.DuplicateAnimalTagException;
import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AnimalCommandServiceImplTest {

    // 08:00 en Lima: "hoy" segun el Clock es 2042-06-07 (independiente de la fecha real de ejecucion).
    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2042-06-07T13:00:00Z"), ZoneId.of("America/Lima"));

    private final AnimalRepository repository = mock(AnimalRepository.class);
    private final AnimalCommandServiceImpl service = new AnimalCommandServiceImpl(repository, CLOCK);

    private RegisterAnimalCommand command(String tag, LocalDate birthDate) {
        return new RegisterAnimalCommand(tag, null, "Holstein", null, birthDate, new BigDecimal("420"), null, null);
    }

    @Test
    void savesActiveAnimalWithNormalizedTagAndQrCode() {
        when(repository.existsByTag("MX-00123")).thenReturn(false);
        when(repository.save(any(Animal.class))).thenAnswer(i -> i.getArgument(0));

        var animal = service.handle(command(" mx-00123 ", TODAY.minusYears(1)));

        assertEquals("MX-00123", animal.getTag());
        assertEquals(AnimalStatus.ACTIVE, animal.getStatus());
        assertNotNull(animal.getQrCode());
        verify(repository).save(animal);
    }

    @Test
    void rejectsDuplicateTagIgnoringCaseWithoutSaving() {
        when(repository.existsByTag("MX-00123")).thenReturn(true);

        assertThrows(DuplicateAnimalTagException.class, () -> service.handle(command("mx-00123", TODAY)));

        verify(repository, never()).save(any());
    }

    @Test
    void mapsUniqueConstraintViolationToDuplicateTag() {
        when(repository.existsByTag("MX-00123")).thenReturn(false);
        when(repository.save(any(Animal.class))).thenThrow(new DataIntegrityViolationException("uk_animals_tag"));

        assertThrows(DuplicateAnimalTagException.class, () -> service.handle(command("MX-00123", TODAY)));
    }

    @Test
    void usesClockTodayToRejectFutureBirthDate() {
        assertThrows(FutureBirthDateException.class, () -> service.handle(command("MX-00123", TODAY.plusDays(1))));

        verifyNoInteractions(repository);
    }

    // --- US-07: edicion de animal ---

    @Test
    void updateHandlerLoadsAppliesChangesAndSaves() {
        var existing = Animal.register(command("A-1", TODAY.minusYears(1)), TODAY);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var updateCommand = new UpdateAnimalCommand(existing.getId(), "Luna", "Jersey", null, TODAY,
                new BigDecimal("410"), null, null);
        var updated = service.handle(updateCommand);

        assertEquals("Luna", updated.getName());
        assertEquals("Jersey", updated.getBreed());
        verify(repository).save(existing);
    }

    @Test
    void updateHandlerThrowsNotFoundWhenAnimalIsMissing() {
        var animalId = UUID.randomUUID();
        when(repository.findById(animalId)).thenReturn(Optional.empty());

        var updateCommand = new UpdateAnimalCommand(animalId, null, "Jersey", null, TODAY, null, null, null);

        assertThrows(AnimalNotFoundException.class, () -> service.handle(updateCommand));

        verify(repository, never()).save(any());
    }

    @Test
    void updateHandlerUsesClockTodayToRejectFutureBirthDateWithoutSaving() {
        var existing = Animal.register(command("A-1", TODAY.minusYears(1)), TODAY);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));

        var updateCommand = new UpdateAnimalCommand(existing.getId(), null, "Jersey", null, TODAY.plusDays(1), null,
                null, null);

        assertThrows(FutureBirthDateException.class, () -> service.handle(updateCommand));

        verify(repository, never()).save(any());
    }
}
