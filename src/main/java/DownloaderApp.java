import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.Optional;
import java.time.Duration;

public final class DownloaderApp {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_USAGE_ERROR = 1;
    private static final int EXIT_HTTP_ERROR = 2;

    private static final int EXIT_NETWORK_ERROR = 3;
    private static final int EXIT_INTERRUPTED = 4;
    private static final int EXIT_INVALID_CONTENT_LENGTH = 5;

    private static final long INVALID_CONTENT_LENGTH = -1L;

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private DownloaderApp() {

    }
    public static void main(final String[] args) {

        final int exitCode = run(args);
        System.exit(exitCode);
    }

    static int run(final String[] args) {
        final URI uri;
        try {
            uri = parseAndValidateUri(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return EXIT_USAGE_ERROR;
        }

        final HttpClient client = createHttpClient();
        final HttpRequest headRequest = buildHeadRequest(uri);

        final FileMetadata metadata;

        try {
            metadata = fetchFileMetadata(client, headRequest, uri);
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

    private static URI parseAndValidateUri(final String[] args) {

        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: ./gradlew run --args=\"<url>\"");
        }

        final URI uri;
        try {
            uri = new URI(args[0]);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Error: Invalid URL: " + e.getMessage());
        }
            
        final String scheme = uri.getScheme();
        final boolean isHttp = "http".equalsIgnoreCase(scheme);
        final boolean isHttps = "https".equalsIgnoreCase(scheme);

        if (!isHttp && !isHttps) {
            throw new IllegalArgumentException("Error: URL must start with http:// or https:// ");
        }

        return uri;
    }

    private static HttpClient createHttpClient() {
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
    

    private static FileMetadata fetchFileMetadata(
        final HttpClient client,
        final HttpRequest request,
        final URI uri
    ) throws IOException, InterruptedException {
        final HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

        final int statusCode = response.statusCode();
        final Optional<String> contentLengthHeader = response.headers().firstValue("Content-Length");
        final Optional<String> acceptRangesHeader = response.headers().firstValue("Accept-Ranges");

        final long contentLength = contentLengthHeader
            .map(Long::parseLong)
            .orElse(INVALID_CONTENT_LENGTH);

        final boolean supportsByteRanges = acceptRangesHeader
            .map(value -> value.equalsIgnoreCase("bytes"))
            .orElse(false);


        return new FileMetadata(
            uri,
            statusCode,
            contentLengthHeader,
            acceptRangesHeader,
            contentLength,
            supportsByteRanges
        );
    }

    private static void printMetadata(final FileMetadata metadata) {
        System.out.println("URL: " + metadata.uri());
        System.out.println("Status: " + metadata.statusCode());
        System.out.println("Content-Length: " + metadata.contentLengthHeader().orElse("<missing>"));
        System.out.println("Accept-Ranges: " + metadata.acceptRangesHeader().orElse("<missing>"));
        System.out.println("Supports byte ranges: " + metadata.supportsByteRanges());
        System.out.println("Parsed file size (bytes): " + metadata.contentLength());
    }

    private record FileMetadata(
        URI uri,
        int statusCode,
        Optional<String> contentLengthHeader,
        Optional<String> acceptRangesHeader,
        long contentLength,
        boolean supportsByteRanges
    ) {

    }
}