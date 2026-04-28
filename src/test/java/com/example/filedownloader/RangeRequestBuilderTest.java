package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RangeRequestBuilder")
class RangeRequestBuilderTest {

    @Test
    @DisplayName("request method is GET and Range header is correct")
    void buildsGetRequestWithRangeHeader() {
        final RangeRequestBuilder builder = new RangeRequestBuilder(Duration.ofSeconds(20));
        final URI uri = URI.create("http://localhost:8080/storage/example-file.txt");
        final ByteRange range = new ByteRange(0, 99);

        final HttpRequest request = builder.build(uri, range);

        assertThat(request.method()).isEqualTo("GET");
        assertThat(request.headers().firstValue("Range")).hasValue("bytes=0-99");
        assertThat(request.uri()).isEqualTo(uri);
    }
}