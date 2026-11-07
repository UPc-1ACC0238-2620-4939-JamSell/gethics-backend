package com.jamsell.gethics.iot.domain.model.aggregates;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeviceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2042, 6, 7, 8, 0);

    // --- US-23 Escenario 1: vinculacion de dispositivo ---

    @Test
    void linksAndNormalizesTheCode() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), " mx-dev-1 "), NOW);

        assertEquals("MX-DEV-1", device.getCode());
        assertEquals(DeviceStatus.CONNECTED, device.getStatus());
        assertEquals(NOW, device.getLinkedAt());
        assertNull(device.getLastReadingAt());
        assertTrue(device.getReadings().isEmpty());
    }

    @Test
    void linkWithoutOwnerIdThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Device.link(new LinkDeviceCommand(null, "MX-DEV-1"), NOW));
    }

    @Test
    void linkWithBlankCodeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> Device.link(new LinkDeviceCommand(UUID.randomUUID(), "   "), NOW));
    }

    @Test
    void linkWithTooLongCodeThrows() {
        var tooLong = "A".repeat(51);

        assertThrows(IllegalArgumentException.class,
                () -> Device.link(new LinkDeviceCommand(UUID.randomUUID(), tooLong), NOW));
    }

    // --- Ingesta de lecturas (y reconexion) ---

    @Test
    void registerReadingAddsItAndUpdatesLastReadingAt() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var receivedAt = NOW.plusMinutes(5);

        var reading = device.registerReading("temperature", new BigDecimal("36.5"), null, receivedAt);

        assertEquals(1, device.getReadings().size());
        assertEquals("temperature", reading.getMetric());
        assertEquals(0, new BigDecimal("36.5").compareTo(reading.getValue()));
        assertEquals(receivedAt, reading.getRecordedAt());
        assertEquals(receivedAt, device.getLastReadingAt());
    }

    @Test
    void registerReadingUsesTheSensorReportedTimeWhenGiven() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var sensorTime = NOW.minusMinutes(2);

        var reading = device.registerReading("temperature", new BigDecimal("36.5"), sensorTime, NOW);

        assertEquals(sensorTime, reading.getRecordedAt());
    }

    @Test
    void registerReadingReconnectsADisconnectedDevice() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        device.markDisconnected();

        device.registerReading("temperature", new BigDecimal("36.5"), null, NOW.plusHours(1));

        assertEquals(DeviceStatus.CONNECTED, device.getStatus());
    }

    @Test
    void registerReadingWithBlankMetricThrows() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);

        assertThrows(IllegalArgumentException.class,
                () -> device.registerReading("  ", new BigDecimal("1"), null, NOW));
    }

    @Test
    void registerReadingWithTooLongMetricThrows() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var tooLong = "A".repeat(51);

        assertThrows(IllegalArgumentException.class,
                () -> device.registerReading(tooLong, new BigDecimal("1"), null, NOW));
    }

    @Test
    void registerReadingWithoutValueThrows() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);

        assertThrows(IllegalArgumentException.class,
                () -> device.registerReading("temperature", null, null, NOW));
    }

    // --- US-23 Escenario 2: perdida de conexion ---

    @Test
    void markDisconnectedChangesStatus() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);

        device.markDisconnected();

        assertEquals(DeviceStatus.DISCONNECTED, device.getStatus());
    }

    @Test
    void isStaleUsesLinkedAtWhenThereAreNoReadingsYet() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var threshold = Duration.ofMinutes(30);

        assertFalse(device.isStale(NOW.plusMinutes(29), threshold));
        assertTrue(device.isStale(NOW.plusMinutes(31), threshold));
    }

    @Test
    void isStaleUsesTheLastReadingWhenThereIsOne() {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "MX-DEV-1"), NOW);
        var lastReadingAt = NOW.plusHours(2);
        device.registerReading("temperature", new BigDecimal("1"), null, lastReadingAt);
        var threshold = Duration.ofMinutes(30);

        assertFalse(device.isStale(lastReadingAt.plusMinutes(29), threshold));
        assertTrue(device.isStale(lastReadingAt.plusMinutes(31), threshold));
    }

    @Test
    void normalizeCodeTrimsAndUppercases() {
        assertEquals("MX-DEV-1", Device.normalizeCode(" mx-dev-1 "));
        assertNull(Device.normalizeCode(null));
    }
}
