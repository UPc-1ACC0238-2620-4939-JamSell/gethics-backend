package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnimalQueryServiceImplTest {

    private final AnimalQueryRepository repository = mock(AnimalQueryRepository.class);
    private final AnimalQueryServiceImpl service = new AnimalQueryServiceImpl(repository);

    private static Animal animal(String tag) {
        return Animal.register(new RegisterAnimalCommand(tag, null, "Holstein", null, LocalDate.of(2024, 1, 1), null, null, null),
                LocalDate.of(2026, 1, 1));
    }

    @Test
    void returnsWhatTheRepositoryFindsForTheSearchAndStatus() {
        var found = List.of(animal("MX-1"), animal("MX-2"));
        when(repository.search("holstein", AnimalStatus.ACTIVE)).thenReturn(found);

        var result = service.handle(new GetAnimalsQuery("holstein", AnimalStatus.ACTIVE));

        assertEquals(found, result);
    }

    @Test
    void returnsAnEmptyListWhenNothingMatches() {
        when(repository.search("zzz", AnimalStatus.ACTIVE)).thenReturn(List.of());

        assertTrue(service.handle(new GetAnimalsQuery("zzz", AnimalStatus.ACTIVE)).isEmpty());
    }

    @Test
    void passesANullSearchThroughUntouched() {
        when(repository.search(null, AnimalStatus.SOLD)).thenReturn(List.of());

        service.handle(new GetAnimalsQuery(null, AnimalStatus.SOLD));

        verify(repository).search(null, AnimalStatus.SOLD);
    }
}
