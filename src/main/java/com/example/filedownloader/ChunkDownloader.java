package com.example.filedownloader;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.io.IOException;

final class ChunkDownloader {

    private final HttpClient httpClient;
    private final RangeRequestBuilder requestBuilder;
    private final ContentRangeParser contentRangeParser;

    ChunkDownloader(
        final HttpClient httpClient,
        final RangeRequestBuilder requestBuilder,
        ContentRangeParser contentRangeParser
    ) {
        this.httpClient = httpClient;
        this.requestBuilder = requestBuilder;
        this.contentRangeParser = contentRangeParser;
    }

    ChunkDownloadResult downloadChunk(final URI uri, final ByteRange requestedRange)
            throws IOException, InterruptedException {

        final HttpRequest request = requestBuilder.build(uri, requestedRange);
        final HttpResponse<byte[]> response = 
            httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        
        if (response.statusCode() != 206) {
            throw new IllegalStateException("Expected 206 Partial Content, got " + response.statusCode());
        }

        final String contentRangeHeader = response.headers()
                .firstValue("Content-Range")
                .orElseThrow(() -> new IllegalStateException("Missing Content-Range header"));
        
        final ContentRange actualRange = contentRangeParser.parse(contentRangeHeader);

        if (actualRange.startInclusive() != requestedRange.startInclusive()
                || actualRange.endInclusive() != requestedRange.endInclusive()) {
            throw new IllegalStateException("Server returned unexpected range: " + contentRangeHeader);
        }

        final byte[] body = response.body();
        if (body.length != requestedRange.length()) {
            throw new IllegalStateException("Unexpected chunk body length: " + body.length);
        }
        
        return new ChunkDownloadResult(requestedRange, body);
    }
}