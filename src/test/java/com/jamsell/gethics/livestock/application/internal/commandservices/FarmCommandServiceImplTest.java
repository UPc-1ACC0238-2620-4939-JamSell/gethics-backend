package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateFarmNameException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidFarmDataException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.FarmStatus;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FarmCommandServiceImplTest {

    private final FarmRepository repository = mock(FarmRepository.class);
    private final FarmCommandServiceImpl service = new FarmCommandServiceImpl(repository);
    private final UUID ownerId = UUID.randomUUID();

    private RegisterFarmCommand command(String name) {
        return new RegisterFarmCommand(ownerId, name, "Jauja, Junin", null);
    }

    @Test
    void savesActiveFarmOfTheOwner() {
        when(repository.existsByOwnerIdAndNormalizedName(ownerId, "fundo sur")).thenReturn(false);
        when(repository.save(any(Farm.class))).thenAnswer(i -> i.getArgument(0));

        var farm = service.handle(command("  Fundo Sur "));

        assertEquals("Fundo Sur", farm.getName());
        assertEquals(ownerId, farm.getOwnerId());
        assertEquals(FarmStatus.ACTIVE, farm.getStatus());
        verify(repository).save(farm);
    }

    @Test
    void rejectsADuplicateNameOfTheSameOwnerIgnoringCase() {
        when(repository.existsByOwnerIdAndNormalizedName(ownerId, "fundo sur")).thenReturn(true);

        var ex = assertThrows(DuplicateFarmNameException.class, () -> service.handle(command("FUNDO SUR")));

        assertEquals("Ya tienes una granja llamada FUNDO SUR.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void mapsUniqueConstraintViolationToDuplicateName() {
        when(repository.existsByOwnerIdAndNormalizedName(ownerId, "fundo sur")).thenReturn(false);
        when(repository.save(any(Farm.class))).thenThrow(new DataIntegrityViolationException("uk_farms_owner_name"));

        assertThrows(DuplicateFarmNameException.class, () -> service.handle(command("Fundo Sur")));
    }

    @Test
    void invalidDataNeverReachesTheRepository() {
        assertThrows(InvalidFarmDataException.class, () -> service.handle(command("  ")));

        verifyNoInteractions(repository);
    }
}
