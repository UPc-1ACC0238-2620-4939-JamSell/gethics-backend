package com.jamsell.gethics.sanitary.application.internal.queryservices;

import com.jamsell.gethics.sanitary.domain.exceptions.InvalidCalendarPeriodException;
import com.jamsell.gethics.sanitary.domain.model.entities.SanitaryEvent;
import com.jamsell.gethics.sanitary.domain.model.queries.GetSanitaryCalendarQuery;
import com.jamsell.gethics.sanitary.domain.repositories.SanitaryCalendarQueryRepository;
import com.jamsell.gethics.sanitary.domain.services.SanitaryCalendarQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SanitaryCalendarQueryServiceImpl implements SanitaryCalendarQueryService {

    // Restriccion TECNICA de persistencia, no una regla de negocio: scheduled_date es una columna DATE de PostgreSQL,
    // que cubre 4713 a. C. (anio Java -4712) .. 5874897 d. C. YearMonth admite un rango mucho mayor. La consulta usa como
    // limite superior exclusivo el dia 1 del mes siguiente, que tambien debe caber en DATE; por eso el ultimo anio
    // completo admitido es 5874896. Se valida aqui para que una consulta no persistible no llegue a Infrastructure.
    private static final int MIN_SUPPORTED_YEAR = -4712;
    private static final int MAX_SUPPORTED_YEAR = 5874896;

    private final SanitaryCalendarQueryRepository repository;

    public SanitaryCalendarQueryServiceImpl(SanitaryCalendarQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SanitaryEvent> handle(GetSanitaryCalendarQuery query) {
        var period = query.period();
        if (period.getYear() < MIN_SUPPORTED_YEAR || period.getYear() > MAX_SUPPORTED_YEAR) {
            throw new InvalidCalendarPeriodException();
        }
        return repository.findScheduledBetween(period.atDay(1), period.plusMonths(1).atDay(1));
    }
}
