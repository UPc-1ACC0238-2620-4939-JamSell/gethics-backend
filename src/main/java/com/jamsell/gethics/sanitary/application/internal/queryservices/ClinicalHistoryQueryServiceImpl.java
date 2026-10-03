package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetClinicalHistoryQuery;
import com.jamsell.gethics.sanitary.domain.repositories.ClinicalHistoryQueryRepository;
import com.jamsell.gethics.sanitary.domain.services.ClinicalHistoryQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClinicalHistoryQueryServiceImpl implements ClinicalHistoryQueryService {

    private final ClinicalHistoryQueryRepository repository;

    public ClinicalHistoryQueryServiceImpl(ClinicalHistoryQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SanitaryEvent> handle(GetClinicalHistoryQuery query) {
        // Se ordena en memoria sobre una copia: se carga el historial completo de un solo animal y la lista del
        // repositorio no se modifica. Sin paginacion (no la exige la US); paginar exigiria ordenar en la base de datos.
        return repository.findEventsByAnimalId(query.animalId()).stream()
                .sorted(ClinicalHistoryChronologicalComparator.INSTANCE)
                .toList();
    }
}
