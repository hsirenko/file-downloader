package com.example.filedownloader;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

final class RandomAccessFileChunkSink implements ChunkSink {

    private final File outputFile;

    RandomAccessFileChunkSink(final File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public void preSize(final long totalLength) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.setLength(totalLength);
        }
    }

    @Override
    public void write(final ByteRange range, final byte[] bytes) throws IOException {
        if (bytes.length != range.length()) {
            throw new IllegalArgumentException("Byte length does not match range length");
        }

        // Safe simple approach: each call opens its own RAF and writes a non-overlapping range.
        try (RandomAccessFile raf = new RandomAccessFile(outputFile, "rw")) {
            raf.seek(range.startInclusive());
            raf.write(bytes);
        }
    }
}