package com.jamsell.gethics.sanitary.domain.repositories;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;

import java.util.List;
import java.util.UUID;

/** Puerto de lectura del historial clinico: no carga el aggregate, solo los eventos del animal. */
public interface ClinicalHistoryQueryRepository {

    /**
     * Todos los eventos del animal, de cualquier estado y sin orden garantizado. Lista vacia si el animal no tiene
     * ClinicalHistory o su historial no tiene eventos (no se distinguen: livestock aun no permite validar el animal).
     */
    List<SanitaryEvent> findEventsByAnimalId(UUID animalId);

    /** Eventos COMPLETED de esos animales (una sola consulta); cada evento trae su historial para conocer el animal. */
    List<SanitaryEvent> findCompletedEventsByAnimalIds(java.util.Collection<UUID> animalIds);
}
