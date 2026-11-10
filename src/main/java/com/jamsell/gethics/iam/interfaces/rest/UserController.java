package com.jamsell.gethics.iam.interfaces.rest;

import com.jamsell.gethics.iam.domain.model.commands.UpdateProfileCommand;
import com.jamsell.gethics.iam.domain.model.commands.UpdateProfilePhotoCommand;
import com.jamsell.gethics.iam.domain.model.queries.GetProfilePhotoByUserIdQuery;
import com.jamsell.gethics.iam.domain.model.queries.GetUserByIdQuery;
import com.jamsell.gethics.iam.domain.model.valueobjects.ProfileImage;
import com.jamsell.gethics.iam.domain.services.UserCommandService;
import com.jamsell.gethics.iam.domain.services.UserQueryService;
import com.jamsell.gethics.iam.infrastructure.security.UserDetailsImpl;
import com.jamsell.gethics.iam.interfaces.rest.resources.ProfileUpdatedResource;
import com.jamsell.gethics.iam.interfaces.rest.resources.UpdateProfileResource;
import com.jamsell.gethics.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.jamsell.gethics.shared.application.result.ApplicationError;
import com.jamsell.gethics.shared.interfaces.rest.transform.ErrorResponseAssembler;
import com.jamsell.gethics.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Users", description = "Perfil del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @Operation(summary = "Consultar mi perfil", description = "US-04: datos del usuario autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil del usuario"),
        @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
        @ApiResponse(responseCode = "404", description = "El usuario no existe")
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return userQueryService.handle(new GetUserByIdQuery(principal.getId()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("User", "El usuario no existe")));
    }

    @Operation(summary = "Editar mi perfil", description = "US-04: actualiza nombre y telefono del usuario autenticado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos"),
        @ApiResponse(responseCode = "401", description = "Token ausente o invalido")
    })
    @PutMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @Valid @RequestBody UpdateProfileResource resource
    ) {
        var command = new UpdateProfileCommand(principal.getId(), resource.name(), resource.phone());
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                user -> new ProfileUpdatedResource(
                        "Perfil actualizado correctamente",
                        UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Actualizar mi foto de perfil", description = "US-04: sube la imagen (multipart, parte file; maximo 2 MB).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Foto actualizada"),
        @ApiResponse(responseCode = "400", description = "Imagen invalida o mayor al maximo permitido"),
        @ApiResponse(responseCode = "401", description = "Token ausente o invalido")
    })
    @PutMapping(value = "/photo", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updatePhoto(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        var command = new UpdateProfilePhotoCommand(principal.getId(), ProfileImage.from(file.getBytes()));
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                user -> new ProfileUpdatedResource(
                        "Foto de perfil actualizada correctamente",
                        UserResourceFromEntityAssembler.toResourceFromEntity(user)),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Obtener mi foto de perfil", description = "Devuelve los bytes de la imagen con su content-type.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagen de perfil"),
        @ApiResponse(responseCode = "401", description = "Token ausente o invalido"),
        @ApiResponse(responseCode = "404", description = "El usuario no tiene foto de perfil")
    })
    @GetMapping("/photo")
    public ResponseEntity<?> getPhoto(@AuthenticationPrincipal UserDetailsImpl principal) {
        return userQueryService.handle(new GetProfilePhotoByUserIdQuery(principal.getId()))
                .<ResponseEntity<?>>map(photo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(photo.getContentType()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .body(photo.getImageBytes()))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("Photo", "El usuario no tiene foto de perfil")));
    }
}
