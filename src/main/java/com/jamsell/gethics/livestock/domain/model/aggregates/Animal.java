package com.jamsell.gethics.livestock.domain.model.aggregates;

import com.jamsell.gethics.livestock.domain.exceptions.FutureBirthDateException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalDataException;
import com.jamsell.gethics.livestock.domain.exceptions.InvalidAnimalWeightException;
import com.jamsell.gethics.livestock.domain.model.commands.RegisterAnimalCommand;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalSex;
import com.jamsell.gethics.livestock.domain.model.valueobjects.AnimalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "animals", uniqueConstraints = {
        @UniqueConstraint(name = "uk_animals_tag", columnNames = "tag"),
        @UniqueConstraint(name = "uk_animals_qr_code", columnNames = "qr_code")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Animal {

    static final int TAG_MAX_LENGTH = 50;
    static final int NAME_MAX_LENGTH = 100;
    static final int BREED_MAX_LENGTH = 60;
    static final int PHOTO_URL_MAX_LENGTH = 500;
    static final String QR_CODE_PREFIX = "GTH-";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Referencia a la finca (contexto Livestock, Farm aun sin implementar): por id, sin FK fisica. Opcional por ahora.
    private UUID farmId;

    // Arete (tag): unico en todo el hato. Se guarda sin espacios y en mayusculas, asi la unicidad no depende de como se escriba.
    @Column(nullable = false, updatable = false, length = TAG_MAX_LENGTH)
    private String tag;

    // Identificador del codigo QR del animal: lo genera el sistema al registrarlo y no cambia.
    @Column(name = "qr_code", nullable = false, updatable = false, length = 20)
    private String qrCode;

    @Column(length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = BREED_MAX_LENGTH)
    private String breed;

    // Opcional hasta que el formulario de registro lo pida.
    @Enumerated(EnumType.STRING)
    private AnimalSex sex;

    @Column(nullable = false)
    private LocalDate birthDate;

    // Opcional (null = no registrado); si viene, es mayor a 0.
    @Column(precision = 7, scale = 2)
    private BigDecimal initialWeightKg;

    @Column(length = PHOTO_URL_MAX_LENGTH)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnimalStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** {@code today} lo aporta Application desde el Clock: una res no puede haber nacido despues de hoy. */
    public static Animal register(RegisterAnimalCommand command, LocalDate today) {
        var birthDate = Objects.requireNonNull(command.birthDate(), "birthDate");
        if (birthDate.isAfter(Objects.requireNonNull(today, "today"))) {
            throw new FutureBirthDateException();
        }
        var weight = command.initialWeightKg();
        if (weight != null && weight.signum() <= 0) {
            throw new InvalidAnimalWeightException();
        }
        var animal = new Animal();
        animal.tag = normalizeTag(command.tag());
        animal.qrCode = generateQrCode();
        animal.name = optionalText(command.name(), NAME_MAX_LENGTH, "El nombre no puede superar 100 caracteres.");
        animal.breed = requireText(command.breed(), "La raza es obligatoria.", BREED_MAX_LENGTH,
                "La raza no puede superar 60 caracteres.");
        animal.sex = command.sex();
        animal.birthDate = birthDate;
        animal.initialWeightKg = weight;
        animal.photoUrl = optionalText(command.photoUrl(), PHOTO_URL_MAX_LENGTH,
                "La URL de la foto no puede superar 500 caracteres.");
        animal.farmId = command.farmId();
        animal.status = AnimalStatus.ACTIVE;
        animal.createdAt = Instant.now();
        return animal;
    }

    /**
     * Asigna el animal a una granja o lo mueve a otra (US-10). Devuelve false, sin cambiar nada, si ya esta en ella.
     * Un animal puede cambiar de granja pero no quedarse sin una: no hay forma de desasociarlo.
     */
    public boolean assignToFarm(UUID newFarmId) {
        Objects.requireNonNull(newFarmId, "farmId");
        if (newFarmId.equals(this.farmId)) {
            return false;
        }
        this.farmId = newFarmId;
        return true;
    }

    /** Forma canonica del arete (trim + mayusculas): con ella el servicio detecta duplicados antes de guardar. */
    public static String normalizeTag(String tag) {
        return requireText(tag, "El arete es obligatorio.", TAG_MAX_LENGTH, "El arete no puede superar 50 caracteres.")
                .toUpperCase(Locale.ROOT);
    }

    // "GTH-" + 12 caracteres hexadecimales: corto para imprimirse o leerse en campo; la restriccion unica cubre colisiones.
    private static String generateQrCode() {
        return QR_CODE_PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String blankMessage, int maxLength, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            throw new InvalidAnimalDataException(blankMessage);
        }
        return limited(value.trim(), maxLength, tooLongMessage);
    }

    private static String optionalText(String value, int maxLength, String tooLongMessage) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return limited(value.trim(), maxLength, tooLongMessage);
    }

    private static String limited(String trimmed, int maxLength, String tooLongMessage) {
        if (trimmed.length() > maxLength) {
            throw new InvalidAnimalDataException(tooLongMessage);
        }
        return trimmed;
    }
}
