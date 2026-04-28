package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ChunkDownloader")
class ChunkDownloaderTest {

    private static final URI URI_SAMPLE = URI.create("http://localhost:8080/storage/example-file.txt");
    private static final ByteRange RANGE = new ByteRange(0, 99);

    @Test
    @DisplayName("success path: 206 + valid Content-Range + body length matches")
    void downloadsChunkSuccessfully() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<byte[]> response = mockByteArrayResponse(
                206,
                headers("Content-Range", "bytes 0-99/1000"),
                bytesOfLength(100)
        );

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(response);

        final ChunkDownloader downloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(Duration.ofSeconds(20)),
                new ContentRangeParser()
        );

        final ChunkDownloadResult result = downloader.downloadChunk(URI_SAMPLE, RANGE);

        assertThat(result.range()).isEqualTo(RANGE);
        assertThat(result.bytes()).hasSize(100);
    }

    @Test
    @DisplayName("fails on status 200")
    void failsOnStatus200() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<byte[]> response = mockByteArrayResponse(
                200,
                headers("Content-Range", "bytes 0-99/1000"),
                bytesOfLength(100)
        );

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(response);

        final ChunkDownloader downloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(Duration.ofSeconds(20)),
                new ContentRangeParser()
        );

        assertThatThrownBy(() -> downloader.downloadChunk(URI_SAMPLE, RANGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Expected 206");
    }

    @Test
    @DisplayName("fails on missing Content-Range")
    void failsOnMissingContentRange() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<byte[]> response = mockByteArrayResponse(
                206,
                headers(), // no Content-Range
                bytesOfLength(100)
        );

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(response);

        final ChunkDownloader downloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(Duration.ofSeconds(20)),
                new ContentRangeParser()
        );

        assertThatThrownBy(() -> downloader.downloadChunk(URI_SAMPLE, RANGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing Content-Range");
    }

    @Test
    @DisplayName("fails on mismatched returned range")
    void failsOnMismatchedReturnedRange() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<byte[]> response = mockByteArrayResponse(
                206,
                headers("Content-Range", "bytes 10-109/1000"), // mismatch
                bytesOfLength(100)
        );

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(response);

        final ChunkDownloader downloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(Duration.ofSeconds(20)),
                new ContentRangeParser()
        );

        assertThatThrownBy(() -> downloader.downloadChunk(URI_SAMPLE, RANGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unexpected range");
    }

    @Test
    @DisplayName("fails on body length mismatch")
    void failsOnBodyLengthMismatch() throws Exception {
        final HttpClient httpClient = mock(HttpClient.class);
        final HttpResponse<byte[]> response = mockByteArrayResponse(
                206,
                headers("Content-Range", "bytes 0-99/1000"),
                bytesOfLength(50) // expected 100
        );

        when(httpClient.send(
                any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()
        )).thenReturn(response);

        final ChunkDownloader downloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(Duration.ofSeconds(20)),
                new ContentRangeParser()
        );

        assertThatThrownBy(() -> downloader.downloadChunk(URI_SAMPLE, RANGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected chunk body length");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<byte[]> mockByteArrayResponse(
            final int status,
            final HttpHeaders headers,
            final byte[] body
    ) {
        final HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(headers);
        when(response.body()).thenReturn(body);
        return response;
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

    private static byte[] bytesOfLength(final int length) {
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ('A' + (i % 26));
        }
        return bytes;
    }
}
