package com.jamsell.gethics.notifications.interfaces.rest;

import com.jamsell.gethics.notifications.domain.model.commands.SetNotificationPreferenceCommand;
import com.jamsell.gethics.notifications.domain.model.queries.GetNotificationPreferenceQuery;
import com.jamsell.gethics.notifications.domain.services.NotificationPreferenceCommandService;
import com.jamsell.gethics.notifications.domain.services.NotificationPreferenceQueryService;
import com.jamsell.gethics.notifications.interfaces.rest.resources.NotificationPreferenceResource;
import com.jamsell.gethics.notifications.interfaces.rest.resources.SetNotificationPreferenceResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notifications")
@RestController
@RequestMapping("/api/v1/users/{userId}/notification-preference")
public class NotificationPreferenceController {

    private final NotificationPreferenceCommandService commandService;
    private final NotificationPreferenceQueryService queryService;

    public NotificationPreferenceController(NotificationPreferenceCommandService commandService,
                                            NotificationPreferenceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(summary = "Consultar la preferencia de notificaciones push",
            description = "Indica si el usuario tiene habilitadas las notificaciones push.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencia del usuario"),
        @ApiResponse(responseCode = "400", description = "userId con formato invalido")
    })
    @GetMapping
    public ResponseEntity<NotificationPreferenceResource> get(@PathVariable UUID userId) {
        var pushEnabled = queryService.handle(new GetNotificationPreferenceQuery(userId));
        return ResponseEntity.ok(new NotificationPreferenceResource(userId, pushEnabled));
    }

    @Operation(summary = "Actualizar la preferencia de notificaciones push",
            description = "Activa o desactiva las notificaciones push del usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferencia actualizada"),
        @ApiResponse(responseCode = "400", description = "Cuerpo invalido (pushEnabled ausente)")
    })
    @PutMapping
    public ResponseEntity<NotificationPreferenceResource> update(@PathVariable UUID userId,
                                                                  @Valid @RequestBody SetNotificationPreferenceResource resource) {
        var preference = commandService.handle(new SetNotificationPreferenceCommand(userId, resource.pushEnabled()));
        return ResponseEntity.ok(new NotificationPreferenceResource(preference.getUserId(), preference.isPushEnabled()));
    }
}
