package com.jamsell.gethics.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileImageTest {

    @Test
    void from_withJpegSignature_detectsJpeg() {
        var bytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00, 0x01};

        assertThat(ProfileImage.from(bytes).contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void from_withPngSignature_detectsPng() {
        var bytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

        assertThat(ProfileImage.from(bytes).contentType()).isEqualTo("image/png");
    }

    @Test
    void from_withWebpSignature_detectsWebp() {
        var bytes = "RIFF....WEBPVP8 ".getBytes();

        assertThat(ProfileImage.from(bytes).contentType()).isEqualTo("image/webp");
    }

    @Test
    void from_withTextFile_isRejected() {
        assertThatThrownBy(() -> ProfileImage.from("no soy una imagen".getBytes()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Formato de imagen no soportado");
    }

    @Test
    void from_withEmptyContent_isRejected() {
        assertThatThrownBy(() -> ProfileImage.from(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void from_withMoreThanTwoMegabytes_isRejected() {
        var bytes = new byte[ProfileImage.MAX_SIZE_BYTES + 1];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;

        assertThatThrownBy(() -> ProfileImage.from(bytes))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 MB");
    }
}
