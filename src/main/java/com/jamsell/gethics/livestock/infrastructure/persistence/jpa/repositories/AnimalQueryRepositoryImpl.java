package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import com.jamsell.gethics.livestock.domain.repositories.AnimalQueryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Repository
public class AnimalQueryRepositoryImpl implements AnimalQueryRepository {

    private static final char ESCAPE = '!';

    private final AnimalJpaRepository jpaRepository;

    public AnimalQueryRepositoryImpl(AnimalJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Animal> search(String search, AnimalStatus status) {
        return jpaRepository.search(toPattern(search), status);
    }

    @Override
    public List<Animal> findByFarmId(UUID farmId, AnimalStatus status) {
        return jpaRepository.findByFarmIdAndStatusOrderByTagAsc(farmId, status);
    }

    /** "contiene", en minusculas y con los comodines LIKE del usuario (%, _) y el propio escape tomados literalmente. */
    static String toPattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        var escaped = search.trim().toLowerCase(Locale.ROOT)
                .replace(String.valueOf(ESCAPE), ESCAPE + "" + ESCAPE)
                .replace("%", ESCAPE + "%")
                .replace("_", ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
