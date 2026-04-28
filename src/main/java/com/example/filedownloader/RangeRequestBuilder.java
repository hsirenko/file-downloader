package com.example.filedownloader;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

final class RangeRequestBuilder {
    
    private final Duration requestTimeout;

    RangeRequestBuilder(final Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    HttpRequest build(final URI uri, final ByteRange range) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .timeout(requestTimeout)
                .header("Range", range.toRangeHeaderValue())
                .GET()
                .build();
    }
}
