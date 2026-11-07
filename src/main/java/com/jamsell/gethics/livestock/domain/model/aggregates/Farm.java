package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.InvalidFarmDataException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterFarmCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.FarmStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "farms", uniqueConstraints = @UniqueConstraint(
        name = "uk_farms_owner_name", columnNames = {"owner_id", "normalized_name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Farm {

    static final int NAME_MAX_LENGTH = 100;
    static final int LOCATION_MAX_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Referencia al usuario dueno (contexto IAM) por id, sin FK fisica entre bounded contexts.
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    // Nombre sin espacios sobrantes y en minusculas: con (owner_id, normalized_name) unico, "Fundo Sur" y "fundo sur"
    // son la misma granja para un mismo dueno, pero cada uno conserva el nombre tal como lo escribio.
    @Column(name = "normalized_name", nullable = false, length = NAME_MAX_LENGTH)
    private String normalizedName;

    @Column(nullable = false, length = LOCATION_MAX_LENGTH)
    private String location;

    // Tamano en hectareas. Opcional (null = no registrado); si viene, es mayor a 0.
    @Column(precision = 9, scale = 2)
    private BigDecimal sizeHectares;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FarmStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public static Farm register(RegisterFarmCommand command) {
        var farm = new Farm();
        farm.ownerId = Objects.requireNonNull(command.ownerId(), "ownerId");
        farm.name = cleanName(command.name());
        farm.normalizedName = normalizeName(command.name());
        farm.location = requireText(command.location(), "La ubicacion es obligatoria.", LOCATION_MAX_LENGTH,
                "La ubicacion no puede superar 200 caracteres.");
        var size = command.sizeHectares();
        if (size != null && size.signum() <= 0) {
            throw new InvalidFarmDataException("El tamano de la granja debe ser mayor a 0.");
        }
        farm.sizeHectares = size;
        farm.status = FarmStatus.ACTIVE;
        farm.createdAt = Instant.now();
        return farm;
    }

    /** Forma canonica del nombre (trim + minusculas): con ella el servicio detecta duplicados del mismo dueno. */
    public static String normalizeName(String name) {
        return cleanName(name).toLowerCase(Locale.ROOT);
    }

    private static String cleanName(String name) {
        return requireText(name, "El nombre de la granja es obligatorio.", NAME_MAX_LENGTH,
                "El nombre de la granja no puede superar 100 caracteres.");
    }

    private static String requireText(String value, String blankMessage, int maxLength, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            throw new InvalidFarmDataException(blankMessage);
        }
        var trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidFarmDataException(tooLongMessage);
        }
        return trimmed;
    }
}
