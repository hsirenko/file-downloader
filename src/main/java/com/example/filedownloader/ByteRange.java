package com.example.filedownloader;


record ByteRange(long startInclusive, long endInclusive) {
    
    ByteRange {
        if (startInclusive < 0 || endInclusive < startInclusive) {
            throw new IllegalArgumentException("Invalid byte range");
        }
    }

    long length() {
        return endInclusive - startInclusive + 1;
    }

    String toRangeHeaderValue() {
        return "bytes=" + startInclusive + "-" + endInclusive;
    }
}