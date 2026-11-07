package com.jamsell.gethics.livestock.interfaces.rest;

import com.jamsell.gethics.livestock.domain.model.queries.GetFarmsByOwnerQuery;
import com.jamsell.gethics.livestock.domain.services.FarmCommandService;
import com.jamsell.gethics.livestock.domain.services.FarmQueryService;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmListResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.FarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.resources.RegisterFarmResource;
import com.jamsell.gethics.livestock.interfaces.rest.transform.FarmAssembler;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Limitacion (US-09): IAM aun no esta integrado, asi que el dueno llega como {@code ownerId} en la solicitud y no se
 * valida que corresponda a quien la envia ni que el usuario exista. Cuando IAM este disponible, el dueno saldra del
 * usuario autenticado y este parametro desaparecera.
 */
@Tag(name = "Livestock")
@RestController
@RequestMapping("/api/v1/farms")
public class FarmController {

    private final FarmCommandService commandService;
    private final FarmQueryService queryService;

    public FarmController(FarmCommandService commandService, FarmQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<FarmResource> registerFarm(@Valid @RequestBody RegisterFarmResource resource) {
        var farm = commandService.handle(FarmAssembler.toCommand(resource));
        return ResponseEntity.status(HttpStatus.CREATED).body(FarmAssembler.toResource(farm));
    }

    /** Granjas del dueno por nombre ascendente. */
    @GetMapping
    public FarmListResource listFarms(@RequestParam UUID ownerId) {
        return FarmAssembler.toListResource(queryService.handle(new GetFarmsByOwnerQuery(ownerId)));
    }
}
