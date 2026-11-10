package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateFarmNameException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidFarmDataException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateFarmCommand;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** US-32: edicion de granja y sus dos escenarios (exitosa y nombre duplicado). */
class FarmUpdateServiceTest {

    private final FarmRepository repository = mock(FarmRepository.class);
    private final FarmCommandServiceImpl service = new FarmCommandServiceImpl(repository);
    private final UUID ownerId = UUID.randomUUID();
    private final Farm farm = Farm.register(new RegisterFarmCommand(ownerId, "Fundo Sur", "Jauja", null));

    FarmUpdateServiceTest() {
        ReflectionTestUtils.setField(farm, "id", UUID.randomUUID());
        when(repository.findById(farm.getId())).thenReturn(Optional.of(farm));
        when(repository.save(any(Farm.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void escenario1EditsTheFarmAndSavesIt() {
        when(repository.existsByOwnerIdAndNormalizedNameAndIdNot(ownerId, "fundo norte", farm.getId())).thenReturn(false);

        var updated = service.handle(new UpdateFarmCommand(farm.getId(), "Fundo Norte", "Huancayo", null));

        assertEquals("Fundo Norte", updated.getName());
        assertEquals("Huancayo", updated.getLocation());
        verify(repository).save(farm);
    }

    @Test
    void escenario2ANameAlreadyUsedByAnotherFarmOfTheSameOwnerIsAWarning() {
        when(repository.existsByOwnerIdAndNormalizedNameAndIdNot(ownerId, "fundo norte", farm.getId())).thenReturn(true);

        var ex = assertThrows(DuplicateFarmNameException.class,
                () -> service.handle(new UpdateFarmCommand(farm.getId(), "FUNDO NORTE", "Huancayo", null)));

        assertEquals("Ya tienes una granja llamada FUNDO NORTE.", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void keepingTheOwnNameIsNotADuplicateBecauseTheFarmItselfIsExcludedFromTheCheck() {
        when(repository.existsByOwnerIdAndNormalizedNameAndIdNot(ownerId, "fundo sur", farm.getId())).thenReturn(false);

        service.handle(new UpdateFarmCommand(farm.getId(), "fundo sur", "Otra ubicacion", null));

        verify(repository).existsByOwnerIdAndNormalizedNameAndIdNot(ownerId, "fundo sur", farm.getId());
    }

    @Test
    void aConcurrentRenameToTheSameNameIsMappedToADuplicate() {
        when(repository.save(any(Farm.class))).thenThrow(new DataIntegrityViolationException("uk_farms_owner_name"));

        assertThrows(DuplicateFarmNameException.class,
                () -> service.handle(new UpdateFarmCommand(farm.getId(), "Fundo Norte", "Huancayo", null)));
    }

    @Test
    void anUnknownFarmIsNotFound() {
        var id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(FarmNotFoundException.class, () -> service.handle(new UpdateFarmCommand(id, "X", "Y", null)));
    }

    @Test
    void invalidDataNeverReachesTheRepository() {
        assertThrows(InvalidFarmDataException.class,
                () -> service.handle(new UpdateFarmCommand(farm.getId(), " ", "Huancayo", null)));

        verify(repository, never()).save(any());
    }
}
