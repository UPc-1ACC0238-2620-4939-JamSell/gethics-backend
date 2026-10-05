package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.exceptions.ClientNotAssignedException;
import com.jamsell.gethics.veterinary.domain.model.queries.GetClientPatientsQuery;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryPatientQueryService;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.ClientPatientsResource;
import com.jamsell.gethics.veterinary.interfaces.rest.transform.PatientResourceAssembler;
import java.util.Map;
import java.util.UUID;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Veterinary")
@RestController
@RequestMapping("/api/v1/vet/clients")
public class VeterinaryPatientController {

    private static final String NO_ANIMALS_MESSAGE = "No animals registered";

    private final VeterinaryPatientQueryService queryService;

    public VeterinaryPatientController(VeterinaryPatientQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/{clientId}/animals")
    public ResponseEntity<ClientPatientsResource> getPatients(@PathVariable UUID clientId,
                                                              @RequestParam UUID veterinarianId) {
        var patients = queryService.handle(new GetClientPatientsQuery(veterinarianId, clientId)).stream()
                .map(view -> PatientResourceAssembler.toResource(view))
                .toList();
        var message = patients.isEmpty() ? NO_ANIMALS_MESSAGE : null;
        return ResponseEntity.ok(new ClientPatientsResource(patients, message));
    }

    @ExceptionHandler(ClientNotAssignedException.class)
    public ResponseEntity<Map<String, String>> handleNotAssigned(ClientNotAssignedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", exception.getMessage()));
    }
}
