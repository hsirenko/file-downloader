package com.example.filedownloader;

import java.io.IOException;
import java.net.URI;

interface ChunkFetcher {
    ChunkDownloadResult fetch(URI uri, ByteRange range) throws IOException, InterruptedException;
}