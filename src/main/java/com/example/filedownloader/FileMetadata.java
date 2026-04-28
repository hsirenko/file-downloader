package com.example.filedownloader;

import java.net.URI;
import java.util.Optional;

/**
 * Snapshot of HTTP metadata relevant for parallel ranged downloads.
 */
record FileMetadata(
    URI uri,
    int statusCode,
    Optional<String> contentLengthHeader,
    Optional<String> acceptRangesHeader,
    long contentLength,
    boolean supportsByteRanges
) {

}