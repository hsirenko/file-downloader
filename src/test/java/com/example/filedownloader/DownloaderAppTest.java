package com.example.filedownloader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("DownloaderApp")
class DownloaderAppTest {

    @Mock
    private HttpClient httpClient;

    private final ByteArrayOutputStream stderr = new ByteArrayOutputStream();
    private PrintStream originalErr;

    @BeforeEach
    void redirectStderr() {
        originalErr = System.err;
        System.setErr(new PrintStream(stderr));
    }

    @AfterEach
    void restoreStderr() {
        System.setErr(originalErr);
    }

    @Test
    @DisplayName("usage error when args are invalid")
    void usageError() {
        final DownloaderApp app = new DownloaderApp();

        assertThat(app.run(new String[]{}, httpClient)).isEqualTo(DownloaderApp.EXIT_USAGE_ERROR);
    }

    @Test
    @DisplayName("network error when client throws IOException")
    void networkError() throws Exception {
        when(httpClient.send(
            any(HttpRequest.class), 
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()
        )).thenThrow(new IOException("boom"));

        final DownloaderApp app = new DownloaderApp();

        assertThat(app.run(new String[]{"http://localhost:8080/a.bin"}, httpClient))
                .isEqualTo(DownloaderApp.EXIT_NETWORK_ERROR);
        assertThat(stderr.toString()).contains("Network error:");
    }

    @Test
    @DisplayName("HTTP error when HEAD returns 404")
    void httpErrorOn404() throws Exception {
        stubHead(404, headers("Content-Length", "0"));

        final DownloaderApp app = new DownloaderApp();

        assertThat(app.run(new String[]{"http://localhost:8080/missing.bin"}, httpClient))
                .isEqualTo(DownloaderApp.EXIT_HTTP_ERROR);
    }

    @Test
    @DisplayName("success when HEAD returns 200 with parseable headers")
    void success() throws Exception {
        stubHead(200, headers(
                "Content-Length", "10",
                "Accept-Ranges", "bytes"
        ));

        final DownloaderApp app = new DownloaderApp();

        assertThat(app.run(new String[]{"http://localhost:8080/a.bin"}, httpClient))
                .isEqualTo(DownloaderApp.EXIT_SUCCESS);
    }

    @Test
    @DisplayName("invalid Content-Length maps to dedicated exit code")
    void invalidContentLengthExitCode() throws Exception {
        stubHead(200, headers("Content-Length", "not-a-number"));

        final DownloaderApp app = new DownloaderApp();

        assertThat(app.run(new String[]{"http://localhost:8080/a.bin"}, httpClient))
                .isEqualTo(DownloaderApp.EXIT_INVALID_CONTENT_LENGTH);
    }

    private void stubHead(final int status, final HttpHeaders hdrs) throws Exception {
        @SuppressWarnings("unchecked")
        final HttpResponse<Void> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.headers()).thenReturn(hdrs);

        when(httpClient.send(
            any(HttpRequest.class),
            org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<Void>>any()
        )).thenReturn(response);
    }

    private static HttpHeaders headers(final String... keysAndValues) {
        final Map<String, List<String>> map = new java.util.HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], List.of(keysAndValues[i + 1]));
        }
        return HttpHeaders.of(map, (name, value) -> true);
    }
}