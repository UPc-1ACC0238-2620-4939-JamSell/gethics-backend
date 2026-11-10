package com.jamsell.gethics.iot.domain.model.queries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetDeviceReadingsQueryTest {

    private final UUID deviceId = UUID.randomUUID();

    @Test
    void acceptsTheLimitsOfPageAndSize() {
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 0, 1));
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 5, GetDeviceReadingsQuery.MAX_SIZE));
    }

    @Test
    void rejectsANegativePage() {
        assertThrows(IllegalArgumentException.class, () -> new GetDeviceReadingsQuery(deviceId, -1, 20));
    }

    @Test
    void rejectsASizeOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new GetDeviceReadingsQuery(deviceId, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GetDeviceReadingsQuery(deviceId, 0, 101));
    }

    @Test
    void rejectsAMissingDeviceId() {
        assertThrows(IllegalArgumentException.class, () -> new GetDeviceReadingsQuery(null, 0, 20));
    }
}
