package com.jamsell.gethics.livestock.infrastructure.persistence.jpa.repositories;

import com.jamsell.gethics.livestock.domain.model.aggregates.Animal;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

interface AnimalJpaRepository extends JpaRepository<Animal, UUID> {

    boolean existsByTag(String tag);

    // pattern llega siempre (con "%" si no hay busqueda) y ya viene en minusculas y con "!" como escape: asi no se
    // enlazan parametros null, que PostgreSQL no puede tipar.
    @Query("""
            select a from Animal a
            where a.status = :status
              and (lower(a.tag) like :pattern escape '!'
                   or lower(a.breed) like :pattern escape '!'
                   or lower(coalesce(a.name, '')) like :pattern escape '!')
            order by a.tag asc""")
    List<Animal> search(@Param("pattern") String pattern, @Param("status") AnimalStatus status);
}
