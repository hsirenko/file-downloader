package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ContentRangeParser")
class ContentRangeParserTest {

    private final ContentRangeParser parser = new ContentRangeParser();

    @Test
    @DisplayName("parses valid bytes 0-99/1000")
    void parsesValidContentRange() {
        final ContentRange parsed = parser.parse("bytes 0-99/1000");

        assertThat(parsed.startInclusive()).isEqualTo(0);
        assertThat(parsed.endInclusive()).isEqualTo(99);
        assertThat(parsed.totalLength()).isEqualTo(1000);
    }

    @Test
    @DisplayName("rejects invalid formats")
    void rejectsInvalidFormats() {
        assertThatThrownBy(() -> parser.parse("bytes 0-99")) // missing /total
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> parser.parse("items 0-99/1000")) // wrong unit
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> parser.parse("bytes a-b/c")) // not numeric
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rejects impossible values")
    void rejectsImpossibleValues() {
        assertThatThrownBy(() -> parser.parse("bytes 100-99/1000")) // end < start
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> parser.parse("bytes 0-1000/1000")) // total must be > end
                .isInstanceOf(IllegalArgumentException.class);
    }
}
