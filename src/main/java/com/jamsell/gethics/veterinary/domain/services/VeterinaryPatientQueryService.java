package com.jamsell.gethics.veterinary.domain.services;

import com.jamsell.gethics.veterinary.domain.model.queries.GetClientPatientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.PatientView;
import java.util.List;

public interface VeterinaryPatientQueryService {

    List<PatientView> handle(GetClientPatientsQuery query);
}
