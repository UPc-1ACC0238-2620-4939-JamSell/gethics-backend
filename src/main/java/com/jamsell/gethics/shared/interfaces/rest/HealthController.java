package com.jamsell.gethics.shared.interfaces.rest;

import com.jamsell.gethics.shared.interfaces.rest.resources.HealthResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class HealthController {

    private final String applicationName;

    public HealthController(@Value("${spring.application.name}") String applicationName) {
        this.applicationName = applicationName;
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public HealthResource health() {
        return new HealthResource("UP", applicationName, Instant.now().toString());
    }
}
