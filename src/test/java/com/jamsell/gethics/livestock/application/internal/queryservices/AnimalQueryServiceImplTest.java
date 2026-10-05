package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnimalQueryServiceImplTest {

    private static final LocalDate TODAY = LocalDate.of(2042, 6, 7);

    private final AnimalRepository repository = mock(AnimalRepository.class);
    private final AnimalQueryServiceImpl service = new AnimalQueryServiceImpl(repository);

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
