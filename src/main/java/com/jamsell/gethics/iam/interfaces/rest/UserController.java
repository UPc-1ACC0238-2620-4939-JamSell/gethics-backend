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

@RestController
@RequestMapping("/users/me")
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public UserController(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal UserDetailsImpl principal) {
        return userQueryService.handle(new GetUserByIdQuery(principal.getId()))
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(UserResourceFromEntityAssembler.toResourceFromEntity(user)))
                .orElseGet(() -> ErrorResponseAssembler.toErrorResponseFromApplicationError(
                        ApplicationError.notFound("User", "El usuario no existe")));
    }

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
