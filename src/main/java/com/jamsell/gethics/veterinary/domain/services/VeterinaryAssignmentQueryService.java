package com.jamsell.gethics.veterinary.domain.services;

import com.jamsell.gethics.veterinary.domain.model.queries.GetAssignedClientsQuery;
import com.jamsell.gethics.veterinary.domain.model.valueobjects.AssignedClient;
import java.util.List;

public interface VeterinaryAssignmentQueryService {

    List<AssignedClient> handle(GetAssignedClientsQuery query);
}
