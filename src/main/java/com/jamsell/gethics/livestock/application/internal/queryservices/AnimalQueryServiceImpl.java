package com.jamsell.gethics.livestock.application.internal.queryservices;

import com.jamsell.gethics.livestock.domain.exceptions.AnimalNotFoundException;
import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.exceptions.AnimalQrNotFoundException;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByIdQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalByQrCodeQuery;
import com.jamsell.gethics.livestock.domain.model.queries.GetAnimalsQuery;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import com.jamsell.gethics.livestock.domain.repositories.AnimalRepository;
import com.jamsell.gethics.livestock.domain.services.AnimalQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimalQueryServiceImpl implements AnimalQueryService {

    private final AnimalQueryRepository queryRepository;
    private final AnimalRepository repository;

    public AnimalQueryServiceImpl(AnimalQueryRepository queryRepository, AnimalRepository repository) {
        this.queryRepository = queryRepository;
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Animal> handle(GetAnimalsQuery query) {
        return queryRepository.search(query.search(), query.status());
    }

    /** US-07: precarga el formulario de edicion. */
    @Override
    @Transactional(readOnly = true)
    public Animal handle(GetAnimalByIdQuery query) {
        return repository.findById(query.animalId())
                .orElseThrow(() -> new AnimalNotFoundException(query.animalId()));
    }

    /** US-29: busca por el codigo del QR; un codigo desconocido es "no se encontro el registro". */
    @Override
    @Transactional(readOnly = true)
    public Animal handle(GetAnimalByQrCodeQuery query) {
        return repository.findByQrCode(query.code()).orElseThrow(AnimalQrNotFoundException::new);
    }
}
