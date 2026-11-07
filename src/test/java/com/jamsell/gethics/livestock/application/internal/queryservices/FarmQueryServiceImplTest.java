package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.repositories.FarmQueryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FarmQueryServiceImplTest {

    private final FarmQueryRepository repository = mock(FarmQueryRepository.class);
    private final FarmQueryServiceImpl service = new FarmQueryServiceImpl(repository);
    private final UUID ownerId = UUID.randomUUID();

    @Test
    void returnsTheFarmsOfThatOwner() {
        var farms = List.of(Farm.register(new RegisterFarmCommand(ownerId, "Fundo Norte", "Lima", null)));
        when(repository.findByOwnerId(ownerId)).thenReturn(farms);

        assertEquals(farms, service.handle(new GetFarmsByOwnerQuery(ownerId)));
    }

    @Test
    void returnsAnEmptyListWhenTheOwnerHasNoFarms() {
        when(repository.findByOwnerId(ownerId)).thenReturn(List.of());

        assertTrue(service.handle(new GetFarmsByOwnerQuery(ownerId)).isEmpty());
    }
}
