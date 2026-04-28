package com.example.filedownloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans non-overlapping inclusive byte ranges that fully cover [0..contentLength-1].
 */
final class RangePlanner {

    List<ByteRange> plan(final long contentLength, final int chunkSize) {

        if (contentLength <= 0) {
            throw new IllegalArgumentException("contentLength must be > 0");
            
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }
        
        final List<ByteRange> ranges = new ArrayList<>();

        long start = 0;
        while (start < contentLength) {
            final long endExclusive = Math.min(start + (long) chunkSize, contentLength);
            final long endInclusive = endExclusive - 1;

            ranges.add(new ByteRange(start, endInclusive));
            start = endExclusive;
        }
        return ranges;
    }
}