package com.example.filedownloader;

record ChunkDownloadResult(ByteRange range, byte[] bytes) {
    ChunkDownloadResult {
        if (range == null || bytes == null) {
            throw new IllegalArgumentException("range and bytes must be non-null");
        }
    }
}