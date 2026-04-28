package com.example.filedownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UriParser")
class UriParserTest {

    private UriParser parser;

    @BeforeEach
    void setUp() {
        parser = new UriParser();
    }

    @Test
    @DisplayName("parses valid http URL")
    void parsesHttp() throws Exception {
        final URI uri = parser.parseRequiredHttpUrl(new String[]{"http://localhost:8080/path"});
        assertThat(uri).hasToString("http://localhost:8080/path");
    }

    @Test
    @DisplayName("parses valid https URL")
    void parsesHttps() throws Exception {
        final URI uri = parser.parseRequiredHttpUrl(new String[]{"https://example.com/file.bin"});
        assertThat(uri.getScheme()).isEqualTo("https");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"ftp://x", "mailto:a@b.com", "not-a-url"})
    @DisplayName("rejects unsupported or invalid URLs")
    void rejectsBadInput(final String raw) {
        final String[] args = raw == null ? null : new String[]{raw};
        assertThatThrownBy(() -> parser.parseRequiredHttpUrl(args))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("requires exactly one argument")
    void requiresOneArgument() {
        assertThatThrownBy(() -> parser.parseRequiredHttpUrl(new String[]{}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }
}