package com.example.filedownloader;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.io.IOException;
import java.time.Duration;

public final class DownloaderApp {

    static final int EXIT_SUCCESS = 0;
    static final int EXIT_USAGE_ERROR = 1;
    static final int EXIT_HTTP_ERROR = 2;

    static final int EXIT_NETWORK_ERROR = 3;
    static final int EXIT_INTERRUPTED = 4;
    static final int EXIT_INVALID_CONTENT_LENGTH = 5;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private static final String PHASE2_DEMO_FLAG = "--phase2-demo";
    private static final int PHASE2_DEMO_CHUNK_END = 99;

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
            final String[] urlOnlyArgs = extractUrlOnlyArgs(args);
            uri = uriParser.parseRequiredHttpUrl(urlOnlyArgs);
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

        if (isPhase2DemoEnabled(args)) {
            return runPhase2ChunkDemo(uri, httpClient);
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

    private static boolean isPhase2DemoEnabled(final String[] args) {
        if (args == null) {
            return false;
        }
        for (final String arg : args) {
            if (PHASE2_DEMO_FLAG.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String[] extractUrlOnlyArgs(final String[] args) {
        if (args == null) {
            return null;
        }

        final java.util.List<String> filtered = new java.util.ArrayList<>();
        for (final String arg : args) {
            if (!PHASE2_DEMO_FLAG.equals(arg)) {
                filtered.add(arg);
            }
        }
        return filtered.toArray(new String[0]);
    }

    private int runPhase2ChunkDemo(final URI uri, final HttpClient httpClient) {
        final ByteRange range = new ByteRange(0, PHASE2_DEMO_CHUNK_END);
        final ChunkDownloader chunkDownloader = new ChunkDownloader(
                httpClient,
                new RangeRequestBuilder(REQUEST_TIMEOUT),
                new ContentRangeParser()
        );
        try {
            final ChunkDownloadResult result = chunkDownloader.downloadChunk(uri, range);
            System.out.println("Phase 2 demo: single range download");
            System.out.println("Downloaded range: " + range.toRangeHeaderValue());
            System.out.println("Chunk size (bytes): " + result.bytes().length);
            System.out.println("First bytes preview (UTF-8): " + previewUtf8(result.bytes(), 32));
            return EXIT_SUCCESS;
        } catch (IOException e) {
            System.err.println("Network error during chunk download: " + e);
            return EXIT_NETWORK_ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Chunk download interrupted.");
            return EXIT_INTERRUPTED;
        } catch (IllegalStateException e) {
            // protocol/validation failures (wrong status, missing headers, mismatch)
            System.err.println("Chunk download validation failed: " + e.getMessage());
            return EXIT_HTTP_ERROR;
        }
    }

    private static String previewUtf8(final byte[] bytes, final int maxBytes) {
        final int length = Math.min(bytes.length, maxBytes);
        return new String(bytes, 0, length, java.nio.charset.StandardCharsets.UTF_8)
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}