package com.example.filedownloader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RangePlanner")
class RangePlannerTest {

    private final RangePlanner planner = new RangePlanner();

    @Test
    @DisplayName("single range when content length is smaller than chunk size")
    void smallerThanChunkSize() {
        final List<ByteRange> ranges = planner.plan(5, 10);

        assertThat(ranges).containsExactly(new ByteRange(0, 4));
    }

    @Test
    @DisplayName("exact multiple of chunk size")
    void exactMultiple() {
        final List<ByteRange> ranges = planner.plan(12, 4);

        assertThat(ranges).containsExactly(
                new ByteRange(0, 3),
                new ByteRange(4, 7),
                new ByteRange(8, 11)
        );
    }

    @Test
    @DisplayName("non-multiple produces smaller last chunk")
    void nonMultipleLastChunkSmaller() {
        final List<ByteRange> ranges = planner.plan(10, 4);

        assertThat(ranges).containsExactly(
                new ByteRange(0, 3),
                new ByteRange(4, 7),
                new ByteRange(8, 9)
        );
    }

    @Test
    @DisplayName("rejects zero or negative content length")
    void rejectsInvalidContentLength() {
        assertThatThrownBy(() -> planner.plan(0, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentLength");

        assertThatThrownBy(() -> planner.plan(-1, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contentLength");
    }

    @Test
    @DisplayName("rejects zero or negative chunk size")
    void rejectsInvalidChunkSize() {
        assertThatThrownBy(() -> planner.plan(10, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");

        assertThatThrownBy(() -> planner.plan(10, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chunkSize");
    }

    @Test
    @DisplayName("off-by-one correctness for first and last byte")
    void offByOneCorrectness() {
        final long contentLength = 19098;
        final int chunkSize = 4096;

        final List<ByteRange> ranges = planner.plan(contentLength, chunkSize);

        assertThat(ranges.get(0).startInclusive()).isEqualTo(0);
        assertThat(ranges.get(ranges.size() - 1).endInclusive()).isEqualTo(contentLength - 1);

        // No gaps and no overlaps
        for (int i = 1; i < ranges.size(); i++) {
            final ByteRange prev = ranges.get(i - 1);
            final ByteRange curr = ranges.get(i);
            assertThat(curr.startInclusive()).isEqualTo(prev.endInclusive() + 1);
        }
    }

    @Test
    @DisplayName("handles large file lengths with long arithmetic")
    void handlesLargeLengths() {
    final long contentLength = (long) Integer.MAX_VALUE + 10_000L;
    final int chunkSize = 1_048_576; // 1 MiB
    final List<ByteRange> ranges = planner.plan(contentLength, chunkSize);
    assertThat(ranges).isNotEmpty();
    assertThat(ranges.get(0).startInclusive()).isEqualTo(0);
    assertThat(ranges.get(ranges.size() - 1).endInclusive()).isEqualTo(contentLength - 1);
    }
}
