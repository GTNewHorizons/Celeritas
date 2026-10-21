package org.embeddedt.embeddium.impl.render.chunk.sorting;

import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TranslucentQuadRecorderTest {
    /** Two groups of this size exceed {@link TranslucentSorter#MAX_TOPO_QUADS}, so the sort needs a partition tree. */
    private static final int QUADS_PER_GROUP = 1100;

    private final ChunkVertexEncoder.Vertex vertex = new ChunkVertexEncoder.Vertex();

    private void quad(TranslucentQuadRecorder recorder, float... positions) {
        for (int i = 0; i < 12; i += 3) {
            this.vertex.x = positions[i];
            this.vertex.y = positions[i + 1];
            this.vertex.z = positions[i + 2];
            recorder.capture(this.vertex);
        }
    }

    @Test
    void nonFiniteQuadDoesNotStallTheSort() throws InterruptedException {
        TranslucentQuadRecorder recorder = new TranslucentQuadRecorder();

        for (int i = 0; i < QUADS_PER_GROUP; i++) {
            float x = i * 0.01f;
            quad(recorder, x, 0, 0, x, 1, 0, x, 1, 1, x, 0, 1);
        }

        for (int i = 0; i < QUADS_PER_GROUP; i++) {
            float y = 12 + i * 0.01f;
            quad(recorder, 12, y, 0, 12, y, 1, 13, y, 1, 13, y, 0);
        }

        quad(recorder, 0, 0, 0, 0, 1, 0, Float.NaN, 1, 1, 0, 0, 1);

        // A daemon thread, so a sort that never finishes cannot keep the test JVM alive.
        AtomicReference<SortState> state = new AtomicReference<>();
        Thread sorter = new Thread(() -> state.set(recorder.getSortState()));
        sorter.setDaemon(true);
        sorter.start();
        sorter.join(10_000);

        assertFalse(sorter.isAlive(), () -> "sort did not finish, at " + Arrays.toString(sorter.getStackTrace()));

        int quadCount = 2 * QUADS_PER_GROUP + 1;
        int[] order = TranslucentSorter.order(state.get(), 8, 8, 8);
        assertNotNull(order);

        BitSet seen = new BitSet(quadCount);
        for (int quad : order) {
            assertFalse(seen.get(quad), "quad " + quad + " drawn twice");
            seen.set(quad);
        }
        assertTrue(order.length == quadCount && seen.cardinality() == quadCount, "every quad drawn once");
    }
}
