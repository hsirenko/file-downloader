package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HeadMetadataParser")
class HeadMetadataParserTest {

    private static final URI URI_SAMPLE = URI.create("http://localhost:8080/a.bin");

    @Test
    @DisplayName("parses Content-Length and Accept-Ranges: bytes")
    void parsesHappyPath() {
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers(
                "Content-Length", "1234",
                "Accept-Ranges", "bytes"
        ));

        final FileMetadata meta = HeadMetadataParser.parse(response, URI_SAMPLE);

        assertThat(meta.statusCode()).isEqualTo(200);
        assertThat(meta.contentLength()).isEqualTo(1234L);
        assertThat(meta.supportsByteRanges()).isTrue();
        assertThat(meta.contentLengthHeader()).hasValue("1234");
        assertThat(meta.acceptRangesHeader()).hasValue("bytes");
    }

    @Test
    @DisplayName("missing Content-Length yields sentinel length")
    void missingContentLength() {
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers("Accept-Ranges", "bytes"));

        final FileMetadata meta = HeadMetadataParser.parse(response, URI_SAMPLE);

        assertThat(meta.contentLength()).isEqualTo(-1L);
        assertThat(meta.contentLengthHeader()).isEmpty();
    }

    @Test
    @DisplayName("invalid Content-Length triggers NumberFormatException when parsing long")
    void invalidContentLengthIsNotHandledHere() {
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers("Content-Length", "NaN"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> HeadMetadataParser.parse(response, URI_SAMPLE))
                .isInstanceOf(NumberFormatException.class);
    }

    private static HttpHeaders headers(final String... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("pairs required");
        }
        final Map<String, List<String>> map = new java.util.HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], List.of(keysAndValues[i + 1]));
        }
        return HttpHeaders.of(map, (name, value) -> true);
    }
}