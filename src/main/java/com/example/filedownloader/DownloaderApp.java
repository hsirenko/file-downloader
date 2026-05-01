package com.example.filedownloader;

import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final ArgsParser argsParser;

    DownloaderApp() {
        this(new ArgsParser());
    }

    DownloaderApp(final ArgsParser argsParser) {
        this.argsParser = argsParser;
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
        final DownloaderConfig config;
        try {
            config = argsParser.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return EXIT_USAGE_ERROR;
        }

        final FileMetadata metadata;

        try {
            final HttpRequest headRequest = buildHeadRequest(config.uri());
            final HttpResponse<Void> response = httpClient.send(headRequest, HttpResponse.BodyHandlers.discarding());
            metadata = HeadMetadataParser.parse(response, config.uri());
        } catch (IOException e) {
            System.err.println(describeNetworkFailure(e, config.uri()));
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

        if (metadata.contentLength() <= 0) {
            System.err.println("Cannot download: invalid Content-Length.");
            return EXIT_INVALID_CONTENT_LENGTH;
        }

        if (!metadata.supportsByteRanges()) {
            System.err.println("Cannot download in parallel: Accept-Ranges is not 'bytes'.");
            return EXIT_HTTP_ERROR;
        }

        try {
            ensureParentDirectory(config.outputPath());

            final ChunkDownloader chunkDownloader = new ChunkDownloader(
                    httpClient,
                    new RangeRequestBuilder(REQUEST_TIMEOUT),
                    new ContentRangeParser()
            );
            final ChunkFetcher fetcher = new HttpChunkFetcher(chunkDownloader);
            final ChunkSink sink = new RandomAccessFileChunkSink(config.outputPath().toFile());

            final ParallelDownloadOrchestrator orchestrator = new ParallelDownloadOrchestrator(
                    new RangePlanner(),
                    fetcher,
                    sink
            );

            orchestrator.download(
                    config.uri(),
                    metadata.contentLength(),
                    config.chunkSize(),
                    config.threads()
            );

            final long writtenSize = Files.size(config.outputPath());

            System.out.println("Parallel download complete.");
            System.out.println("Output: " + config.outputPath().toAbsolutePath());
            System.out.println("Expected size (bytes): " + metadata.contentLength());
            System.out.println("Written size (bytes): " + writtenSize);
            System.out.println("Tip: verify with shasum/cmp against source file.");

            return EXIT_SUCCESS;
        } catch (IOException e) {
            System.err.println(describeNetworkFailure(e, config.uri()));
            return EXIT_NETWORK_ERROR;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Parallel download interrupted.");
            return EXIT_INTERRUPTED;
        } catch (IllegalStateException e) {
            System.err.println("Protocol validation failed: " + e.getMessage());
            return EXIT_HTTP_ERROR;
        }
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

    private static void ensureParentDirectory(final Path outputPath) throws IOException {
        final Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String describeNetworkFailure(final IOException e, final URI uri) {
        final ConnectException connect = findInChain(e, ConnectException.class);
        if (connect != null) {
            final String detail = connect.getMessage() != null ? connect.getMessage() : "connection failed";
            final String target = uri.getAuthority() != null ? uri.getAuthority() : uri.toString();
            return "Network error: cannot connect to " + target + " (" + detail + "). "
                    + "Check that the server is running and the URL is correct.";
        }
        final UnknownHostException unknownHost = findInChain(e, UnknownHostException.class);
        if (unknownHost != null) {
            final String host = unknownHost.getMessage() != null ? unknownHost.getMessage() : "(unknown)";
            return "Network error: unknown host '" + host + "'.";
        }
        if (findInChain(e, HttpTimeoutException.class) != null) {
            return "Network error: request timed out.";
        }
        final String msg = e.getMessage();
        if (msg != null && !msg.isBlank()) {
            return "Network error: " + msg;
        }
        return "Network error: " + e.getClass().getSimpleName();
    }

    private static <T extends Throwable> T findInChain(Throwable t, final Class<T> type) {
        while (t != null) {
            if (type.isInstance(t)) {
                return type.cast(t);
            }
            final Throwable cause = t.getCause();
            if (cause == t) {
                break;
            }
            t = cause;
        }
        return null;
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