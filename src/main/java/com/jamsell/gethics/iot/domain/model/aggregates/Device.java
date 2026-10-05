package com.jamsell.gethics.iot.domain.model.aggregates;

import com.jamsell.gethics.iot.domain.model.commands.LinkDeviceCommand;
import com.jamsell.gethics.iot.domain.model.entities.SensorReading;
import com.jamsell.gethics.iot.domain.model.valueobjects.DeviceStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(name = "uk_devices_code", columnNames = "code"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device {

    static final int CODE_MAX_LENGTH = 50;
    static final int METRIC_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    // Codigo de vinculacion (US-23 Escenario 1): unico en todo el sistema, se guarda sin espacios y en mayusculas.
    @Column(nullable = false, updatable = false, length = CODE_MAX_LENGTH)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    // Null hasta que llegue la primera lectura.
    @Column(name = "last_reading_at")
    private LocalDateTime lastReadingAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SensorReading> readings = new ArrayList<>();

    /** {@code now} lo aporta Application desde el Clock compartido (igual que Animal/sanitary). */
    public static Device link(LinkDeviceCommand command, LocalDateTime now) {
        if (command.ownerId() == null) {
            throw new IllegalArgumentException("El ownerId es obligatorio.");
        }
        if (command.code() == null || command.code().isBlank()) {
            throw new IllegalArgumentException("El codigo del dispositivo es obligatorio.");
        }
        var code = normalizeCode(command.code());
        if (code.length() > CODE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "El codigo del dispositivo no puede superar " + CODE_MAX_LENGTH + " caracteres.");
        }
        var device = new Device();
        device.ownerId = command.ownerId();
        device.code = code;
        device.status = DeviceStatus.CONNECTED;
        device.linkedAt = now;
        return device;
    }

    /**
     * US-23 Escenario 1: cada lectura recibida reconecta el dispositivo. {@code recordedAt} es el instante que
     * reporta el propio sensor (null = se usa {@code receivedAt}); {@code receivedAt} es la hora del servidor, la
     * que cuenta para decidir si el dispositivo sigue conectado (ver {@link #isStale}).
     */
    public SensorReading registerReading(String metric, BigDecimal value, LocalDateTime recordedAt,
                                         LocalDateTime receivedAt) {
        if (metric == null || metric.isBlank()) {
            throw new IllegalArgumentException("El metric de la lectura es obligatorio.");
        }
        var trimmedMetric = metric.trim();
        if (trimmedMetric.length() > METRIC_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "El metric de la lectura no puede superar " + METRIC_MAX_LENGTH + " caracteres.");
        }
        if (value == null) {
            throw new IllegalArgumentException("El valor de la lectura es obligatorio.");
        }
        var reading = new SensorReading(this, trimmedMetric, value, recordedAt != null ? recordedAt : receivedAt);
        readings.add(reading);
        status = DeviceStatus.CONNECTED;
        lastReadingAt = receivedAt;
        return reading;
    }

    /** US-23 Escenario 2: el dispositivo dejo de sincronizar datos. */
    public void markDisconnected() {
        status = DeviceStatus.DISCONNECTED;
    }

    /** Sin lecturas desde hace mas de {@code offlineThreshold} (o nunca recibio ninguna desde que se vinculo). */
    public boolean isStale(LocalDateTime now, Duration offlineThreshold) {
        var reference = lastReadingAt != null ? lastReadingAt : linkedAt;
        return reference.plus(offlineThreshold).isBefore(now);
    }

    public static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }
}
