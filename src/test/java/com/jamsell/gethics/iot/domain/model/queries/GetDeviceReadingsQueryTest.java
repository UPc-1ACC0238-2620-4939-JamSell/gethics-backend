package com.jamsell.gethics.iot.domain.model.queries;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // --- US-48 ---

    @Test
    void acceptsNoRangeAnOpenRangeAndASingleDay() {
        var day = LocalDate.of(2042, 6, 7);
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 0, 20, null, null));
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 0, 20, day, null));
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 0, 20, null, day));
        assertDoesNotThrow(() -> new GetDeviceReadingsQuery(deviceId, 0, 20, day, day));
    }

    @Test
    void rejectsAFromAfterTo() {
        assertThrows(IllegalArgumentException.class, () -> new GetDeviceReadingsQuery(deviceId, 0, 20,
                LocalDate.of(2042, 6, 8), LocalDate.of(2042, 6, 7)));
    }

    @Test
    void theBoundsAreStartOfFromAndStartOfTheDayAfterTo() {
        var query = new GetDeviceReadingsQuery(deviceId, 0, 20, LocalDate.of(2042, 12, 31), LocalDate.of(2042, 12, 31));

        assertEquals(LocalDateTime.of(2042, 12, 31, 0, 0), query.fromInclusive());
        assertEquals(LocalDateTime.of(2043, 1, 1, 0, 0), query.toExclusive());
    }

    @Test
    void theThreeArgumentConstructorHasNoRange() {
        var query = new GetDeviceReadingsQuery(deviceId, 0, 20);

        assertNull(query.fromInclusive());
        assertNull(query.toExclusive());
    }
}
