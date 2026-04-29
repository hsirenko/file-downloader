package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ParallelDownloadOrchestrator")
class ParallelDownloadOrchestratorTest {

    @Test
    @DisplayName("downloads all planned ranges and writes each chunk")
    void downloadsAllRanges() throws Exception {
        final URI uri = URI.create("http://localhost:8080/file.txt");

        final FakeFetcher fetcher = new FakeFetcher();
        final InMemorySink sink = new InMemorySink();

        final ParallelDownloadOrchestrator orchestrator = new ParallelDownloadOrchestrator(
                new RangePlanner(),
                fetcher,
                sink
        );

        orchestrator.download(uri, 10, 4, 3);

        assertThat(sink.totalLength).isEqualTo(10);
        assertThat(sink.writes).hasSize(3);
        assertThat(sink.writes.get(new ByteRange(0, 3))).hasSize(4);
        assertThat(sink.writes.get(new ByteRange(4, 7))).hasSize(4);
        assertThat(sink.writes.get(new ByteRange(8, 9))).hasSize(2);
    }

    @Test
    @DisplayName("propagates fetch failure")
    void propagatesFetchFailure() {
        final URI uri = URI.create("http://localhost:8080/file.txt");

        final ChunkFetcher failingFetcher = (u, range) -> {
            if (range.startInclusive() == 4) {
                throw new IOException("simulated fetch failure");
            }
            return new ChunkDownloadResult(range, bytes((int) range.length()));
        };

        final InMemorySink sink = new InMemorySink();

        final ParallelDownloadOrchestrator orchestrator = new ParallelDownloadOrchestrator(
                new RangePlanner(),
                failingFetcher,
                sink
        );

        assertThatThrownBy(() -> orchestrator.download(uri, 10, 4, 3))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("simulated fetch failure");
    }

    private static final class FakeFetcher implements ChunkFetcher {
        @Override
        public ChunkDownloadResult fetch(final URI uri, final ByteRange range) {
            return new ChunkDownloadResult(range, bytes((int) range.length()));
        }
    }

    private static final class InMemorySink implements ChunkSink {
        long totalLength;
        final Map<ByteRange, byte[]> writes = new ConcurrentHashMap<>();

        @Override
        public void preSize(final long totalLength) {
            this.totalLength = totalLength;
        }

        @Override
        public void write(final ByteRange range, final byte[] bytes) {
            writes.put(range, bytes);
        }
    }

    private static byte[] bytes(final int length) {
        final byte[] out = new byte[length];
        for (int i = 0; i < length; i++) {
            out[i] = (byte) ('A' + (i % 26));
        }
        return out;
    }
}
