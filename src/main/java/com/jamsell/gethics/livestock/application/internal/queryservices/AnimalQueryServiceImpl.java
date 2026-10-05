package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta minima que necesita US-07 para precargar el formulario de edicion (la busqueda/listado es US-06). */
@Service
public class AnimalQueryServiceImpl implements AnimalQueryService {

    private final AnimalRepository repository;

    public AnimalQueryServiceImpl(AnimalRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Animal handle(GetAnimalByIdQuery query) {
        return repository.findById(query.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(query.animalId()));
    }
}
