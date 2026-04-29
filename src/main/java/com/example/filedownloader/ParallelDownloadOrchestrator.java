package com.example.filedownloader;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

final class ParallelDownloadOrchestrator {

    private final RangePlanner rangePlanner;
    private final ChunkFetcher chunkFetcher;
    private final ChunkSink chunkSink;

    ParallelDownloadOrchestrator(
            final RangePlanner rangePlanner,
            final ChunkFetcher chunkFetcher,
            final ChunkSink chunkSink
    ) {
        this.rangePlanner = rangePlanner;
        this.chunkFetcher = chunkFetcher;
        this.chunkSink = chunkSink;
    }

    void download(
            final URI uri,
            final long contentLength,
            final int chunkSize,
            final int parallelism
    ) throws IOException, InterruptedException {

        if (parallelism <= 0) {
            throw new IllegalArgumentException("parallelism must be > 0");
        }

        final List<ByteRange> ranges = rangePlanner.plan(contentLength, chunkSize);
        chunkSink.preSize(contentLength);

        final ExecutorService executor = Executors.newFixedThreadPool(parallelism);
        try {
            final List<Callable<Void>> tasks = new ArrayList<>();
            for (final ByteRange range : ranges) {
                tasks.add(() -> {
                    final ChunkDownloadResult result = chunkFetcher.fetch(uri, range);
                    chunkSink.write(result.range(), result.bytes());
                    return null;
                });
            }

            final List<Future<Void>> futures = executor.invokeAll(tasks);

            for (final Future<Void> future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    final Throwable cause = e.getCause();
                    if (cause instanceof IOException io) {
                        throw io;
                    }
                    if (cause instanceof InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ie;
                    }
                    throw new IllegalStateException("Parallel download task failed", cause);
                }
            }

        } finally {
            executor.shutdownNow();
        }
    }
}