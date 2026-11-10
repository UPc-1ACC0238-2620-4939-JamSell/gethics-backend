package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * US-37 contra el PostgreSQL de application-dev.yaml (en CI, el servicio postgres:16 del workflow). Cada test hace
 * rollback y usa codigos aleatorios, asi que no depende de otros datos de la base.
 *
 * <p>Spring cachea cada contexto de test con su propio pool de conexiones, y el resto de la suite ya usa casi todo el
 * limite de PostgreSQL (max_connections = 100). Este contexto solo necesita 2 conexiones, asi no lo supera.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DeviceRepositoryImpl.class)
@TestPropertySource(properties = {"spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=1"})
class DeviceReadingsPersistenceTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2042, 6, 7, 8, 0);

    @Autowired
    DeviceRepository repository;
    @Autowired
    EntityManager em;

    private Device deviceWithReadings(int count) {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "DEV-" + UUID.randomUUID()), BASE);
        // Se registran en desorden para comprobar que el orden lo da recordedAt y no el de insercion.
        for (int i = 0; i < count; i++) {
            int hour = (i * 7) % count;
            device.registerReading("temperature", BigDecimal.valueOf(30 + hour), BASE.plusHours(hour), BASE);
        }
        var saved = repository.save(device);
        em.clear();
        return saved;
    }

    @Test
    void returnsTheReadingsFromNewestToOldestPageByPage() {
        var device = deviceWithReadings(5);

        var first = repository.findReadingsByDeviceId(device.getId(), 0, 2);
        var last = repository.findReadingsByDeviceId(device.getId(), 2, 2);

        assertEquals(5, first.totalElements());
        assertEquals(3, first.totalPages());
        assertEquals(0, first.page());
        assertEquals(2, first.size());
        assertEquals(BASE.plusHours(4), first.content().get(0).getRecordedAt());
        assertEquals(BASE.plusHours(3), first.content().get(1).getRecordedAt());
        assertEquals(1, last.content().size());
        assertEquals(BASE, last.content().get(0).getRecordedAt());
    }

    @Test
    void doesNotMixReadingsOfOtherDevices() {
        var device = deviceWithReadings(2);
        deviceWithReadings(3);

        var page = repository.findReadingsByDeviceId(device.getId(), 0, 20);

        assertEquals(2, page.totalElements());
        assertTrue(page.content().stream().map(SensorReading::getDevice).allMatch(d -> d.getId().equals(device.getId())));
    }

    @Test
    void anExistingDeviceWithoutReadingsHasAnEmptyPage() {
        var device = deviceWithReadings(0);

        var page = repository.findReadingsByDeviceId(device.getId(), 0, 20);

        assertTrue(repository.existsById(device.getId()));
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
        assertTrue(page.content().isEmpty());
    }

    @Test
    void anUnknownDeviceDoesNotExist() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }
}
