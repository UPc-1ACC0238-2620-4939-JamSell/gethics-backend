package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.commands.UnassignClientCommand;
import com.jamsell.gethics.veterinary.domain.services.VeterinaryAssignmentCommandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID; // <-- ESTA LÍNEA ES LA QUE FALTA

@RestController
@RequestMapping("/api/v1/vet/assignments")
@Tag(name = "Veterinary Assignments", description = "Endpoints for Veterinary Client Assignments")
public class VeterinaryAssignmentsController {

    private final VeterinaryAssignmentCommandService assignmentCommandService;

    public VeterinaryAssignmentsController(VeterinaryAssignmentCommandService assignmentCommandService) {
        this.assignmentCommandService = assignmentCommandService;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Unassign client from veterinarian")
    public ResponseEntity<?> unassignClient(@PathVariable UUID id) {
        var command = new UnassignClientCommand(id);
        assignmentCommandService.handle(command);
        return ResponseEntity.noContent().build();
    }

}
