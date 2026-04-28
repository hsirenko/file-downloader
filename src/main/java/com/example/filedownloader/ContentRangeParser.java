package com.example.filedownloader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ContentRangeParser {

    private static final Pattern CONTENT_RANGE_PATTERN = 
        Pattern.compile("^bytes\\s+(\\d+)-(\\d+)/(\\d+)$");
    
    ContentRange parse(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Missing Content-Range");
        }

        final Matcher matcher = CONTENT_RANGE_PATTERN.matcher(rawValue.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid Content-Range format: " + rawValue);
        }

        final long start = Long.parseLong(matcher.group(1));
        final long end = Long.parseLong(matcher.group(2));
        final long total = Long.parseLong(matcher.group(3));

        if (start < 0 || end < start || total <= end) {
            throw new IllegalArgumentException("Invalid Content-Range values: " + rawValue);
        }

        return new ContentRange(start, end, total);
    }
}