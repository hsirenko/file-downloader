package com.example.filedownloader;

import java.io.IOException;

interface ChunkSink {
    void preSize(long totalLength) throws IOException;
    void write(ByteRange range, byte[] bytes) throws IOException;
}