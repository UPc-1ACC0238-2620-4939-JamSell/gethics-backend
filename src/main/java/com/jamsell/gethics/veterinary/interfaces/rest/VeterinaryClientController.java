package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.queries.GetAssignedClientsQuery;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryAssignmentCommandService;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryAssignmentQueryService;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignClientResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignedClientsResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.AssignmentResource;
import com.jamsell.gethics.veterinary.interfaces.rest.transform.VeterinaryAssignmentAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Veterinary")
@RestController
@RequestMapping("/api/v1/vet")
public class VeterinaryClientController {

    private static final String NO_CLIENTS_MESSAGE = "No linked clients";

    private final VeterinaryAssignmentCommandService commandService;
    private final VeterinaryAssignmentQueryService queryService;

    public VeterinaryClientController(VeterinaryAssignmentCommandService commandService,
                                      VeterinaryAssignmentQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResource> assignClient(@Valid @RequestBody AssignClientResource resource) {
        var assignment = commandService.handle(VeterinaryAssignmentAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VeterinaryAssignmentAssembler.toResource(assignment));
    }

    @GetMapping("/clients")
    public ResponseEntity<AssignedClientsResource> getAssignedClients(@RequestParam UUID veterinarianId) {
        var clients = queryService.handle(new GetAssignedClientsQuery(veterinarianId)).stream()
                .map(client -> VeterinaryAssignmentAssembler.toResource(client))
                .toList();
        var message = clients.isEmpty() ? NO_CLIENTS_MESSAGE : null;
        return ResponseEntity.ok(new AssignedClientsResource(clients, message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleConflict(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }
}

