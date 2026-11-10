package com.jamsell.gethics.livestock.application.internal.commandservices;

import com.jamsell.gethics.livestock.domain.exceptions.DuplicateFarmNameException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Farm;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.commands.UpdateFarmCommand;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmCommandServiceImpl implements FarmCommandService {

    private final FarmRepository repository;

    public FarmCommandServiceImpl(FarmRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Farm handle(RegisterFarmCommand command) {
        var farm = Farm.register(command);
        if (repository.existsByOwnerIdAndNormalizedName(farm.getOwnerId(), farm.getNormalizedName())) {
            throw new DuplicateFarmNameException(farm.getName());
        }
        try {
            return repository.save(farm);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultaneos del mismo dueno con el mismo nombre: gana la restriccion unica de la tabla.
            throw new DuplicateFarmNameException(farm.getName());
        }
    }

    /** US-32: edita la granja; el nombre nuevo no puede repetirse entre las granjas del mismo dueno. */
    @Override
    @Transactional
    public Farm handle(UpdateFarmCommand command) {
        var farm = repository.findById(command.farmId()).orElseThrow(FarmNotFoundException::new);
        // El nombre se valida ANTES de modificar la granja: si se modificara primero, la consulta de duplicados
        // dispararia un flush con el UPDATE y la restriccion unica saltaria como error de integridad.
        var normalizedName = Farm.normalizeName(command.name());
        if (repository.existsByOwnerIdAndNormalizedNameAndIdNot(farm.getOwnerId(), normalizedName, farm.getId())) {
            throw new DuplicateFarmNameException(command.name().trim());
        }
        farm.update(command.name(), command.location(), command.sizeHectares());
        try {
            return repository.save(farm);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateFarmNameException(farm.getName());
        }
    }
}
