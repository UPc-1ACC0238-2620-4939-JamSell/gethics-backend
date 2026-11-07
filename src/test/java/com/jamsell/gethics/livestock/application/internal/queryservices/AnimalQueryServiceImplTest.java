package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalQueryServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);

    private final AnimalQueryRepository queryRepository = mock(AnimalQueryRepository.class);
    private final AnimalRepository repository = mock(AnimalRepository.class);
    private final AnimalQueryServiceImpl service = new AnimalQueryServiceImpl(queryRepository, repository);

    private static Animal animal(String tag) {
        return Animal.register(new RegisterAnimalCommand(tag, null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
    }

    // --- US-06: listado y busqueda ---

    @Test
    void returnsWhatTheRepositoryFindsForTheSearchAndStatus() {
        var found = List.of(animal("MX-1"), animal("MX-2"));
        when(queryRepository.search("holstein", AnimalStatus.ACTIVE)).thenReturn(found);

        var result = service.handle(new GetAnimalsQuery("holstein", AnimalStatus.ACTIVE));

        assertEquals(found, result);
    }

    @Test
    void returnsAnEmptyListWhenNothingMatches() {
        when(queryRepository.search("zzz", AnimalStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(service.handle(new GetAnimalsQuery("zzz", AnimalStatus.ACTIVE)).isEmpty());
    }

    @Test
    void passesANullSearchThroughUntouched() {
        when(queryRepository.search(null, AnimalStatus.SOLD)).thenReturn(List.of());

        service.handle(new GetAnimalsQuery(null, AnimalStatus.SOLD));

        verify(queryRepository).search(null, AnimalStatus.SOLD);
    }

    // --- US-07: animal por id ---

    @Test
    void returnsTheAnimalWhenItExists() {
        var command = new RegisterAnimalCommand("A-1", "Luna", "Holstein", null, TODAY, new BigDecimal("300"), null, null);
        var animal = Animal.register(command, TODAY);
        when(repository.findById(animal.getId())).thenReturn(Optional.of(animal));

        var result = service.handle(new GetAnimalByIdQuery(animal.getId()));

        assertEquals(animal.getId(), result.getId());
        assertEquals("Luna", result.getName());
    }

    @Test
    void throwsNotFoundWhenTheAnimalDoesNotExist() {
        var animalId = UUID.randomUUID();
        when(repository.findById(animalId)).thenReturn(Optional.empty());

        assertThrows(AnimalNotFoundException.class, () -> service.handle(new GetAnimalByIdQuery(animalId)));
    }
}
