package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.exceptions.FarmNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.entities.AnimalFarmAssignment;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalFarmHistoryQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsByFarmQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalFarmAssignmentRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.repositories.FarmRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalFarmQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimalFarmQueryServiceImpl implements AnimalFarmQueryService {

    private final AnimalRepository animals;
    private final FarmRepository farms;
    private final AnimalQueryRepository animalQueries;
    private final AnimalFarmAssignmentRepository assignments;

    public AnimalFarmQueryServiceImpl(AnimalRepository animals, FarmRepository farms,
                                      AnimalQueryRepository animalQueries, AnimalFarmAssignmentRepository assignments) {
        this.animals = animals;
        this.farms = farms;
        this.animalQueries = animalQueries;
        this.assignments = assignments;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnimalFarmAssignment> handle(GetAnimalFarmHistoryQuery query) {
        if (!animals.existsById(query.animalId())) {
            throw new AnimalNotFoundException();
        }
        return assignments.findByAnimalId(query.animalId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Animal> handle(GetAnimalsByFarmQuery query) {
        if (!farms.existsById(query.farmId())) {
            throw new FarmNotFoundException();
        }
        return animalQueries.findByFarmId(query.farmId(), query.status());
    }
}
