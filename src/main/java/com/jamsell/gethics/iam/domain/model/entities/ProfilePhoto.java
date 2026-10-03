package com.jamsell.gethics.iam.domain.model.entities;

import com.jamsell.gethics.iam.domain.model.valueobjects.ProfileImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "profile_photos")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfilePhoto {

    @Id
    private Long userId;

    @Column(nullable = false, length = 50)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea") // <-- Cambio aplicado aquí
    private byte[] imageBytes;

    public ProfilePhoto(Long userId, ProfileImage image) {
        this.userId = userId;
        replace(image);
    }

    public final void replace(ProfileImage image) {
        this.contentType = image.contentType();
        this.imageBytes = image.bytes();
    }
}