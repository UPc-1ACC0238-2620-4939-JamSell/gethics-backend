package com.jamsell.gethics.notifications.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Preferencia de notificaciones push de un usuario (US-22, Escenario 2). Por defecto las notificaciones estan
 * activas: un usuario sin fila propia se trata como {@code pushEnabled = true}, ver
 * {@code NotificationPreferenceQueryServiceImpl}.
 */
@Entity
@Table(name = "notification_preferences")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Version
    private Long version;

    public NotificationPreference(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        this.userId = userId;
        this.pushEnabled = true;
    }

    public void setPushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
    }
}
