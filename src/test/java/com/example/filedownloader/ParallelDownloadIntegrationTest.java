package com.example.filedownloader;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParallelDownload integration")
class ParallelDownloadIntegrationTest {

    private static final Pattern RANGE_PATTERN = Pattern.compile("^bytes=(\\d+)-(\\d+)$");

    @Test
    @DisplayName("downloads full file in parallel from HTTP range server")
    void downloadsInParallelAndMatchesChecksum() throws Exception {
        final byte[] source = buildSourceBytes();

        final HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file.txt", exchange -> handleFileExchange(exchange, source));
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        try {
            final int port = server.getAddress().getPort();
            final URI uri = URI.create("http://localhost:" + port + "/file.txt");

            final Path tempOut = Files.createTempFile("downloader-it-", ".bin");
            try {
                final HttpClient client = HttpClient.newHttpClient();
                final ChunkDownloader chunkDownloader = new ChunkDownloader(
                        client,
                        new RangeRequestBuilder(java.time.Duration.ofSeconds(20)),
                        new ContentRangeParser()
                );

                final ChunkFetcher fetcher = new HttpChunkFetcher(chunkDownloader);
                final ChunkSink sink = new RandomAccessFileChunkSink(tempOut.toFile());
                final ParallelDownloadOrchestrator orchestrator = new ParallelDownloadOrchestrator(
                        new RangePlanner(),
                        fetcher,
                        sink
                );

                orchestrator.download(uri, source.length, 1024, 4);

                final byte[] downloaded = Files.readAllBytes(tempOut);

                assertThat(downloaded.length).isEqualTo(source.length);
                assertThat(downloaded).isEqualTo(source);
                assertThat(sha256Hex(downloaded)).isEqualTo(sha256Hex(source));
            } finally {
                Files.deleteIfExists(tempOut);
            }
        } finally {
            server.stop(0);
        }
    }

    private static void handleFileExchange(final HttpExchange exchange, final byte[] source) throws IOException {
        final String method = exchange.getRequestMethod();
        final Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.add("Accept-Ranges", "bytes");
        responseHeaders.add("Content-Type", "text/plain");

        if ("HEAD".equalsIgnoreCase(method)) {
            responseHeaders.add("Content-Length", String.valueOf(source.length));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            final String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
            if (rangeHeader == null) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            final Matcher m = RANGE_PATTERN.matcher(rangeHeader);
            if (!m.matches()) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }

            final int start = Integer.parseInt(m.group(1));
            final int end = Integer.parseInt(m.group(2));

            if (start < 0 || end < start || end >= source.length) {
                exchange.sendResponseHeaders(416, -1);
                exchange.close();
                return;
            }

            final byte[] slice = Arrays.copyOfRange(source, start, end + 1);

            responseHeaders.add("Content-Range", "bytes " + start + "-" + end + "/" + source.length);
            responseHeaders.add("Content-Length", String.valueOf(slice.length));

            exchange.sendResponseHeaders(206, slice.length);
            exchange.getResponseBody().write(slice);
            exchange.close();
            return;
        }

        exchange.sendResponseHeaders(405, -1);
        exchange.close();
    }

    private static byte[] buildSourceBytes() {
        // Repeat predictable content to produce enough chunks for parallelism.
        final String text = "The Art of Doing Science and Engineering\n";
        return text.repeat(3000).getBytes(StandardCharsets.UTF_8);
    }

    private static String sha256Hex(final byte[] data) throws Exception {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        final byte[] digest = md.digest(data);

        final StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
