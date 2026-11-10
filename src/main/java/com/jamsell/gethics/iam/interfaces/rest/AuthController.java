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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "Registro, inicio de sesion con JWT y recuperacion de contrasena (endpoints publicos)")
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    private final UserCommandService userCommandService;

    public AuthController(UserCommandService userCommandService) {
        this.userCommandService = userCommandService;
    }

    @Operation(summary = "Registrar un usuario",
            description = "US-01: crea una cuenta con rol GANADERO o VETERINARIO. La contrasena se guarda cifrada.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (correo, contrasena o rol)"),
        @ApiResponse(responseCode = "409", description = "El correo ya esta en uso")
    })
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

    @Operation(summary = "Iniciar sesion",
            description = "US-02: valida las credenciales y devuelve un token JWT para usar como Bearer en el resto de endpoints.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesion iniciada; incluye el token JWT"),
        @ApiResponse(responseCode = "400", description = "Datos invalidos (correo o contrasena vacios)"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
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

    @Operation(summary = "Solicitar recuperacion de contrasena",
            description = "US-03: envia al correo un enlace de restablecimiento con vigencia limitada.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Enlace de restablecimiento enviado"),
        @ApiResponse(responseCode = "400", description = "Correo con formato invalido"),
        @ApiResponse(responseCode = "404", description = "No existe una cuenta asociada a ese correo")
    })
    @PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordResource resource) {
        var result = userCommandService.handle(new ForgotPasswordCommand(resource.email()));
        return ResponseEntityAssembler.toResponseEntityFromResult(
                result,
                sent -> new MessageResource("Te enviamos un enlace para restablecer tu contraseña"),
                HttpStatus.OK
        );
    }

    @Operation(summary = "Restablecer la contrasena",
            description = "US-03: define una nueva contrasena con el token recibido por correo.",
            security = {})
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contrasena actualizada"),
        @ApiResponse(responseCode = "400", description = "Token invalido o expirado, o contrasena no valida")
    })
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
