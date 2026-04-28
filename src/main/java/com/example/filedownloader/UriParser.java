package com.example.filedownloader;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates program arguments and builds a URI restricted to HTTP or HTTPS.
 */
final class UriParser {
    URI parseRequiredHttpUrl(final String[] args) {

        if (args == null || args.length != 1) {
            throw new IllegalArgumentException("Usage: ./gradlew run --args=\"<url>\"");
        }

        final URI uri;
        try {
            uri = new URI(args[0]);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Error: Invalid URL: " + e.getMessage(), e);
        }
            
        final String scheme = uri.getScheme();
        final boolean isHttp = "http".equalsIgnoreCase(scheme);
        final boolean isHttps = "https".equalsIgnoreCase(scheme);

        if (!isHttp && !isHttps) {
            throw new IllegalArgumentException("Error: URL must start with http:// or https:// ");
        }

        return uri;
    }
}