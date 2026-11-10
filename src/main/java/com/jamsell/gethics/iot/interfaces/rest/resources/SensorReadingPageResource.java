package com.jamsell.gethics.iot.interfaces.rest.resources;

import java.util.List;

public record SensorReadingPageResource(
        List<SensorReadingResource> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
