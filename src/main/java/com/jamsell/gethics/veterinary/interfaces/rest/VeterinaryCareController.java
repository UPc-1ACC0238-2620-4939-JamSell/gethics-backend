package com.jamsell.gethics.veterinary.interfaces.rest;

import com.jamsell.gethics.veterinary.domain.model.commands.SyncCareBatchCommand;
import com.jamsell.gethics.veterinary.domain.model.exceptions.CareConflictException;
import com.jamsell.gethics.veterinary.domain.services.CareRecordCommandService;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.CareRecordResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.RegisterCareResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncCareResource;
import com.jamsell.gethics.veterinary.interfaces.rest.resources.SyncCareResponseResource;
import com.jamsell.gethics.veterinary.interfaces.rest.transform.CareRecordAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Veterinary")
@RestController
@RequestMapping("/api/v1/vet")
public class VeterinaryCareController {

    private final CareRecordCommandService commandService;

    public VeterinaryCareController(CareRecordCommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/patients/{patientId}/care")
    public ResponseEntity<CareRecordResource> registerCare(@PathVariable UUID patientId,
                                                           @Valid @RequestBody RegisterCareResource resource) {
        var result = commandService.handle(CareRecordAssembler.toCommand(patientId, resource));
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(CareRecordAssembler.toResource(result.record()));
    }

    @PostMapping("/care/sync")
    public ResponseEntity<SyncCareResponseResource> sync(@Valid @RequestBody SyncCareResource resource) {
        var items = resource.items().stream().map(CareRecordAssembler::toCommand).toList();
        var results = commandService.handle(new SyncCareBatchCommand(items)).stream()
                .map(outcome -> CareRecordAssembler.toResource(outcome))
                .toList();
        return ResponseEntity.ok(new SyncCareResponseResource(results));
    }

    @ExceptionHandler(CareConflictException.class)
    public ResponseEntity<Map<String, String>> handleConflict(CareConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalid(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
