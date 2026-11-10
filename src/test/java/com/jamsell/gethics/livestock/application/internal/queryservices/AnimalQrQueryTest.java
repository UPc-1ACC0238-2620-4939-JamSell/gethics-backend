package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalQrNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByQrCodeQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-29: el QR de un animal lo identifica; un codigo desconocido es "no se encontro el registro". */
class AnimalQrQueryTest {

    private final AnimalRepository repository = mock(AnimalRepository.class);
    private final AnimalQueryServiceImpl service = new AnimalQueryServiceImpl(mock(AnimalQueryRepository.class), repository);

    @Test
    void theCodeIsTrimmedAndUppercasedBeforeSearching() {
        assertEquals("GTH-AB12CD34EF56", new GetAnimalByQrCodeQuery("  gth-ab12cd34ef56 ").code());
    }

    @Test
    void escenario1AnExistingCodeReturnsTheAnimalWhateverItsStatus() {
        var animal = Animal.register(new RegisterAnimalCommand("A-1", null, "Holstein", null,
                LocalDate.of(2024, 1, 1), null, null, null), LocalDate.of(2026, 1, 1));
        animal.deactivate();
        var query = new GetAnimalByQrCodeQuery(animal.getQrCode().toLowerCase());
        when(repository.findByQrCode(animal.getQrCode())).thenReturn(Optional.of(animal));

        assertSame(animal, service.handle(query));
        verify(repository).findByQrCode(animal.getQrCode());
    }

    @Test
    void escenario2AnUnknownCodeIsNotFound() {
        when(repository.findByQrCode("GTH-000000000000")).thenReturn(Optional.empty());

        assertThrows(AnimalQrNotFoundException.class,
                () -> service.handle(new GetAnimalByQrCodeQuery("GTH-000000000000")));
    }

    @Test
    void aBlankCodeIsNeverSearched() {
        assertThrows(AnimalQrNotFoundException.class, () -> new GetAnimalByQrCodeQuery("   "));
        assertThrows(AnimalQrNotFoundException.class, () -> new GetAnimalByQrCodeQuery(null));
    }

    @Test
    void theMessageTellsTheUserTheRecordWasNotFound() {
        assertEquals("No se encontro el registro: el codigo QR no corresponde a ningun animal.",
                new AnimalQrNotFoundException().getMessage());
    }
}
