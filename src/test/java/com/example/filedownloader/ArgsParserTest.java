package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ArgsParser")
class ArgsParserTest {

    private final ArgsParser parser = new ArgsParser();

    @Test
    @DisplayName("uses defaults when only URL is provided")
    void usesDefaultsWithUrlOnly() {
        final DownloaderConfig config = parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt"
        });

        assertThat(config.uri()).isEqualTo(URI.create("http://localhost:8080/storage/example-file.txt"));
        assertThat(config.outputPath()).isEqualTo(Path.of("storage/downloaded.bin"));
        assertThat(config.threads()).isEqualTo(4);
        assertThat(config.chunkSize()).isEqualTo(4096);
    }

    @Test
    @DisplayName("parses explicit output threads and chunk-size")
    void parsesExplicitOptions() {
        final DownloaderConfig config = parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt",
                "--output", "storage/out.bin",
                "--threads", "8",
                "--chunk-size", "8192"
        });

        assertThat(config.uri()).isEqualTo(URI.create("http://localhost:8080/storage/example-file.txt"));
        assertThat(config.outputPath()).isEqualTo(Path.of("storage/out.bin"));
        assertThat(config.threads()).isEqualTo(8);
        assertThat(config.chunkSize()).isEqualTo(8192);
    }

    @Test
    @DisplayName("rejects missing args")
    void rejectsMissingArgs() {
        assertThatThrownBy(() -> parser.parse(new String[]{}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }

    @Test
    @DisplayName("rejects non-positive threads")
    void rejectsNonPositiveThreads() {
        assertThatThrownBy(() -> parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt",
                "--threads", "0"
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--threads");
    }

    @Test
    @DisplayName("rejects non-positive chunk-size")
    void rejectsNonPositiveChunkSize() {
        assertThatThrownBy(() -> parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt",
                "--chunk-size", "0"
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--chunk-size");
    }

    @Test
    @DisplayName("shows usage on --help")
    void showsUsageOnHelp() {
        assertThatThrownBy(() -> parser.parse(new String[]{"--help"}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Usage:");
    }

    @Test
    @DisplayName("rejects invalid numeric values")
    void rejectsInvalidNumericValues() {
        assertThatThrownBy(() -> parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt",
                "--threads", "abc"
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--threads must be a positive integer");

        assertThatThrownBy(() -> parser.parse(new String[]{
                "http://localhost:8080/storage/example-file.txt",
                "--chunk-size", "abc"
        }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("--chunk-size must be a positive integer");
    }
}
