package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ByteRange")
class ByteRangeTest {

    @Test
    @DisplayName("valid range calculates length")
    void validRangeCalculatesLength() {
        final ByteRange range = new ByteRange(0, 99);

        assertThat(range.length()).isEqualTo(100);
        assertThat(range.toRangeHeaderValue()).isEqualTo("bytes=0-99");
    }

    @Test
    @DisplayName("invalid range throws")
    void invalidRangeThrows() {
        assertThatThrownBy(() -> new ByteRange(-1, 99))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ByteRange(10, 9))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
