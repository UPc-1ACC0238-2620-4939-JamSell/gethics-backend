package com.jamsell.gethics.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void normalizeOrNull_removesSeparators() {
        assertThat(PhoneNumber.normalizeOrNull("+51 987-654-321")).isEqualTo("+51987654321");
    }

    @Test
    void normalizeOrNull_withBlankOrNull_returnsNull() {
        assertThat(PhoneNumber.normalizeOrNull("  ")).isNull();
        assertThat(PhoneNumber.normalizeOrNull(null)).isNull();
    }

    @Test
    void normalizeOrNull_withLetters_isRejected() {
        assertThatThrownBy(() -> PhoneNumber.normalizeOrNull("abc123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("teléfono");
    }

    @Test
    void normalizeOrNull_withTooFewDigits_isRejected() {
        assertThatThrownBy(() -> PhoneNumber.normalizeOrNull("12345"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
