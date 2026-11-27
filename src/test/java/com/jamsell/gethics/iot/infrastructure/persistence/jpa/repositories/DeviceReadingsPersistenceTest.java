package com.jamsell.gethics.iot.infrastructure.persistence.jpa.repositories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jamsell.gethics.iot.domain.model.aggregates.Device;
import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import com.jamsell.gethics.iot.domain.model.valueobjects.SensorReadingPage;
import com.jamsell.gethics.iot.domain.repositories.DeviceRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

        var first = repository.findReadingsByDeviceId(device.getId(), null, null, 0, 2);
        var last = repository.findReadingsByDeviceId(device.getId(), null, null, 2, 2);

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

        var page = repository.findReadingsByDeviceId(device.getId(), null, null, 0, 20);

        assertEquals(2, page.totalElements());
        assertTrue(page.content().stream().map(SensorReading::getDevice).allMatch(d -> d.getId().equals(device.getId())));
    }

    @Test
    void anExistingDeviceWithoutReadingsHasAnEmptyPage() {
        var device = deviceWithReadings(0);

        var page = repository.findReadingsByDeviceId(device.getId(), null, null, 0, 20);

        assertTrue(repository.existsById(device.getId()));
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
        assertTrue(page.content().isEmpty());
    }

    @Test
    void anUnknownDeviceDoesNotExist() {
        assertFalse(repository.existsById(UUID.randomUUID()));
    }

    // --- US-48: filtro por rango de dias, resuelto en el WHERE de la consulta ---

    private static final LocalDateTime DAY1 = LocalDateTime.of(2042, 6, 1, 0, 0);
    private static final LocalDateTime DAY2 = DAY1.plusDays(1);
    private static final LocalDateTime DAY3 = DAY1.plusDays(2);
    private static final LocalDateTime DAY4 = DAY1.plusDays(3);

    /** Lecturas en los limites exactos de cada dia (00:00:00 y 23:59:59.999999) para probar inclusion/exclusion. */
    private Device deviceWithReadingsAt(LocalDateTime... recordedAt) {
        var device = Device.link(new LinkDeviceCommand(UUID.randomUUID(), "DEV-" + UUID.randomUUID()), BASE);
        for (var at : recordedAt) {
            device.registerReading("temperature", BigDecimal.TEN, at, BASE);
        }
        var saved = repository.save(device);
        em.clear();
        return saved;
    }

    private static final LocalDateTime[] BOUNDARY_READINGS = {
        DAY1.minusNanos(1_000),            // 31-may 23:59:59.999999 -> antes de day1
        DAY1,                              // 1-jun 00:00:00
        DAY2.minusNanos(1_000),            // 1-jun 23:59:59.999999
        DAY2,                              // 2-jun 00:00:00
        DAY3.plusHours(12),                // 3-jun 12:00
        DAY4.minusNanos(1_000),            // 3-jun 23:59:59.999999
        DAY4                               // 4-jun 00:00:00 -> despues de day3
    };

    private List<LocalDateTime> recordedAts(SensorReadingPage page) {
        return page.content().stream().map(SensorReading::getRecordedAt).toList();
    }

    @Test
    void withoutADateRangeReturnsAllTheReadings() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        var page = repository.findReadingsByDeviceId(device.getId(), null, null, 0, 20);

        assertEquals(7, page.totalElements());
    }

    @Test
    void fromAndToAreBothInclusiveAndExcludeEarlierAndLaterReadings() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        // dias 1 a 3 (to exclusivo = 00:00 del dia 4)
        var page = repository.findReadingsByDeviceId(device.getId(), DAY1, DAY4, 0, 20);

        assertEquals(List.of(DAY4.minusNanos(1_000), DAY3.plusHours(12), DAY2, DAY2.minusNanos(1_000), DAY1),
                recordedAts(page));
        assertEquals(5, page.totalElements());
    }

    @Test
    void aSingleDayReturnsOnlyThatDay() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        var page = repository.findReadingsByDeviceId(device.getId(), DAY1, DAY2, 0, 20);

        assertEquals(List.of(DAY2.minusNanos(1_000), DAY1), recordedAts(page));
    }

    @Test
    void onlyFromReturnsThatDayOnwards() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        var page = repository.findReadingsByDeviceId(device.getId(), DAY3, null, 0, 20);

        assertEquals(List.of(DAY4, DAY4.minusNanos(1_000), DAY3.plusHours(12)), recordedAts(page));
    }

    @Test
    void onlyToReturnsUpToAndIncludingThatDay() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        var page = repository.findReadingsByDeviceId(device.getId(), null, DAY2, 0, 20);

        assertEquals(List.of(DAY2.minusNanos(1_000), DAY1, DAY1.minusNanos(1_000)), recordedAts(page));
    }

    @Test
    void aRangeWithoutReadingsReturnsAnEmptyPage() {
        var device = deviceWithReadingsAt(BOUNDARY_READINGS);

        var page = repository.findReadingsByDeviceId(device.getId(), DAY4.plusDays(10), DAY4.plusDays(11), 0, 20);

        assertTrue(page.content().isEmpty());
        assertEquals(0, page.totalElements());
        assertEquals(0, page.totalPages());
    }

    @Test
    void theRangeNeverIncludesReadingsOfOtherDevices() {
        var device = deviceWithReadingsAt(DAY2.plusHours(1));
        deviceWithReadingsAt(DAY2.plusHours(2), DAY2.plusHours(3));

        var page = repository.findReadingsByDeviceId(device.getId(), DAY2, DAY3, 0, 20);

        assertEquals(1, page.totalElements());
        assertTrue(page.content().stream().allMatch(r -> r.getDevice().getId().equals(device.getId())));
    }

    @Test
    void theRangeKeepsNewestFirstOrderAndPaginatesOverTheFilteredReadings() {
        var device = deviceWithReadingsAt(DAY1.plusHours(1), DAY2.plusHours(1), DAY2.plusHours(2), DAY2.plusHours(3),
                DAY3.plusHours(1));

        var first = repository.findReadingsByDeviceId(device.getId(), DAY2, DAY3, 0, 2);
        var second = repository.findReadingsByDeviceId(device.getId(), DAY2, DAY3, 1, 2);

        assertEquals(3, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(List.of(DAY2.plusHours(3), DAY2.plusHours(2)), recordedAts(first));
        assertEquals(List.of(DAY2.plusHours(1)), recordedAts(second));
    }
}
