package com.example.filedownloader;

import java.io.IOException;
import java.net.URI;

final class HttpChunkFetcher implements ChunkFetcher {

    private final ChunkDownloader chunkDownloader;

    HttpChunkFetcher(final ChunkDownloader chunkDownloader) {
        this.chunkDownloader = chunkDownloader;
    }

    @Override
    public ChunkDownloadResult fetch(final URI uri, final ByteRange range)
            throws IOException, InterruptedException {
        return chunkDownloader.downloadChunk(uri, range);
    }
}