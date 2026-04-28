package com.example.filedownloader;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.Optional;
import java.time.Duration;

public final class DownloaderApp {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_USAGE_ERROR = 1;
    static final int EXIT_HTTP_ERROR = 2;

    static final int EXIT_NETWORK_ERROR = 3;
    static final int EXIT_INTERRUPTED = 4;
    static final int EXIT_INVALID_CONTENT_LENGTH = 5;

    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final UriParser uriParser;

    DownloaderApp() {
        this(new UriParser());
    }

    DownloaderApp(final UriParser uriParser) {
        this.uriParser = uriParser;
    }
    public static void main(final String[] args) {

        final int exitCode = new DownloaderApp().run(args);
        System.exit(exitCode);
    }

    int run(final String[] args) {
        return run(args, defaultHttpClient());
    }

    
    /**
     * Test seam: callers supply an {@link HttpClient} (Mockito mock).
     */
    int run(final String[] args, final HttpClient httpClient) {
        final URI uri;
        try {
            uri = uriParser.parseRequiredHttpUrl(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return EXIT_USAGE_ERROR;
        }

        final HttpRequest headRequest = buildHeadRequest(uri);

        final FileMetadata metadata;

        try {
            final var response = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
            metadata = HeadMetadataParser.parse(response, uri);
        } catch (IOException e) {
            System.err.println("Network error: " + e);
            if (e.getCause() != null) {
                System.err.println("Cause: " + e.getCause());
            }
            return EXIT_NETWORK_ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Request interrupted.");
            return EXIT_INTERRUPTED;
        } catch (NumberFormatException e) {
            System.err.println("Invalid Content-Length format.");
            return EXIT_INVALID_CONTENT_LENGTH;
        }

        printMetadata(metadata);

        if (metadata.statusCode() >= 400) {
            System.err.println("HEAD request failed with status " + metadata.statusCode());
            return EXIT_HTTP_ERROR;
        }

        if (metadata.contentLength() < 0) {
            System.err.println("Warning: Content-Length is missing or invalid.");
        }

        if (!metadata.supportsByteRanges()) {
            System.err.println("Warning: Accept-Ranges is not 'bytes'. Parallel ranged download may not work.");
        }

        return EXIT_SUCCESS;

    }

    private static HttpClient defaultHttpClient() {
        return HttpClient.newBuilder()
                    .connectTimeout(CONNECT_TIMEOUT)
                    .build();
    }

    private static HttpRequest buildHeadRequest(final URI uri) {
        return HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(REQUEST_TIMEOUT)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
    }
    



    private static void printMetadata(final FileMetadata metadata) {
        System.out.println("URL: " + metadata.uri());
        System.out.println("Status: " + metadata.statusCode());
        System.out.println("Content-Length: " + metadata.contentLengthHeader().orElse("<missing>"));
        System.out.println("Accept-Ranges: " + metadata.acceptRangesHeader().orElse("<missing>"));
        System.out.println("Supports byte ranges: " + metadata.supportsByteRanges());
        System.out.println("Parsed file size (bytes): " + metadata.contentLength());
    }
}