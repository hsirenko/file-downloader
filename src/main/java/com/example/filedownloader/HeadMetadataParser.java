package com.example.filedownloader;

import java.net.URI;
import java.util.Optional;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;

/**
 * Maps a HEAD response to structured metadata used by the downloader.
 */
final class HeadMetadataParser {

    private static final long MISSING_CONTENT_LENGTH = -1L;

    private HeadMetadataParser() {
    }

    static FileMetadata parse(final HttpResponse<Void> response, final URI uri) {

        final int statusCode = response.statusCode();

        final HttpHeaders headers = response.headers();

        final Optional<String> contentLengthHeader = headers.firstValue("Content-Length");
        final Optional<String> acceptRangesHeader = headers.firstValue("Accept-Ranges");

        final long contentLength = contentLengthHeader
            .map(Long::parseLong)
            .orElse(MISSING_CONTENT_LENGTH);

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
}