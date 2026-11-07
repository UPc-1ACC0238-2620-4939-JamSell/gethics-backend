package com.jamsell.gethics.notifications.interfaces.rest;

import com.jamsell.gethics.notifications.domain.model.commands.SendPushNotificationCommand;
import com.jamsell.gethics.notifications.domain.services.NotificationDispatchCommandService;
import com.jamsell.gethics.notifications.interfaces.rest.resources.PushNotificationResultResource;
import com.jamsell.gethics.notifications.interfaces.rest.resources.SendPushNotificationResource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class PushNotificationController {

    private final NotificationDispatchCommandService dispatchService;

    public PushNotificationController(NotificationDispatchCommandService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @PostMapping
    public ResponseEntity<PushNotificationResultResource> send(@Valid @RequestBody SendPushNotificationResource resource) {
        var command = new SendPushNotificationCommand(
                resource.userId(), resource.category(), resource.title(), resource.body());
        var result = dispatchService.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PushNotificationResultResource(result.wasSent(), result.outcome().name()));
    }
}
