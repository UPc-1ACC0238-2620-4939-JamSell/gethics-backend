package com.jamsell.gethics.iam.interfaces.rest;

import com.jamsell.gethics.iam.domain.model.commands.ForgotPasswordCommand;
import com.jamsell.gethics.iam.domain.model.commands.ResetPasswordCommand;
import com.jamsell.gethics.iam.domain.services.UserCommandService;
import com.jamsell.gethics.iam.interfaces.rest.resources.ForgotPasswordResource;
import com.jamsell.gethics.iam.interfaces.rest.resources.LoginResource;
import com.jamsell.gethics.iam.interfaces.rest.resources.RegisterUserResource;
import com.jamsell.gethics.iam.interfaces.rest.resources.ResetPasswordResource;
import com.jamsell.gethics.iam.interfaces.rest.transform.AuthenticatedUserResourceFromEntityAssembler;
import com.jamsell.gethics.iam.interfaces.rest.transform.LoginCommandFromResourceAssembler;
import com.jamsell.gethics.iam.interfaces.rest.transform.RegisterUserCommandFromResourceAssembler;
import com.jamsell.gethics.iam.interfaces.rest.transform.UserResourceFromEntityAssembler;
import com.jamsell.gethics.shared.interfaces.rest.resources.MessageResource;
import com.jamsell.gethics.shared.interfaces.rest.transform.ResponseEntityAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final UserCommandService userCommandService;

    public AuthController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterUserResource resource) {
        var command = RegisterUserCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                UserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.CREATED
        );
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@Valid @RequestBody LoginResource resource) {
        var command = LoginCommandFromResourceAssembler.toCommandFromResource(resource);
        var result = userCommandService.handle(command);
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                AuthenticatedUserResourceFromEntityAssembler::toResourceFromEntity,
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordResource resource) {
        var result = userCommandService.handle(new ForgotPasswordCommand(resource.email()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                sent -> new MessageResource("Te enviamos un enlace para restablecer tu contraseña"),
                HttpStatus.OK
        );
    }

    @PostMapping(value = "/reset-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordResource resource) {
        var result = userCommandService.handle(new ResetPasswordCommand(resource.token(), resource.newPassword()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                updated -> new MessageResource("Tu contraseña fue actualizada correctamente"),
                HttpStatus.OK
        );
    }
}
