package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCoalescingTest {
    private static final ChunkPrimitiveType QUADS = new StubPrimitiveType();

    private static final class StubPrimitiveType implements ChunkPrimitiveType {
        @Override
        public int getVerticesPerPrimitive() {
            return 4;
        }

        @Override
        public int getIndexBufferElementsPerPrimitive() {
            return 6;
        }

        @Override
        public void generateSimpleIndexBuffer(ByteBuffer indexBuffer, int numPrimitives) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void generateSortedIndexBuffer(ByteBuffer indexBuffer, int numPrimitives,
                                              TranslucentQuadAnalyzer.SortState chunkData, float x, float y, float z) {
            throw new UnsupportedOperationException();
        }
    }

    private record Command(int baseVertex, int elementCount, long indexOffset) {
    }

    private static final class RecordingSink implements DrawCommandSink {
        final List<Command> commands = new ArrayList<>();
        int maxElementCount;

        @Override
        public void clear() {
            this.commands.clear();
            this.maxElementCount = 0;
        }

        @Override
        public int size() {
            return this.commands.size();
        }

        @Override
        public int getIndexBufferSize() {
            return this.maxElementCount;
        }

        @Override
        public void push(int baseVertex, int elementCount, long indexOffset) {
            if (elementCount == 0) {
                return;
            }

            this.commands.add(new Command(baseVertex, elementCount, indexOffset));
            this.maxElementCount = Math.max(this.maxElementCount, elementCount);
        }

        List<Integer> drawnQuads() {
            List<Integer> quads = new ArrayList<>();

            for (Command command : this.commands) {
                int firstQuad = command.baseVertex() / 4;
                int quadCount = command.elementCount() / 6;

                for (int i = 0; i < quadCount; i++) {
                    quads.add(firstQuad + i);
                }
            }

            return quads;
        }
    }

    @Test
    void abuttingSpansCollapseIntoOneCommand() {
        RecordingSink sink = new RecordingSink();

        BatchAssembler.emitRun(sink, 0, 16, QUADS);
        BatchAssembler.emitRun(sink, 16, 40, QUADS);

        assertEquals(2, sink.size(), "emitRun itself does not coalesce; the fill loops do");
    }

    @Test
    void coalescedCommandDrawsExactlyTheSameQuads() {
        RecordingSink coalesced = new RecordingSink();
        RecordingSink separate = new RecordingSink();

        // One command covering vertices [0, 40), versus the two abutting spans it replaces.
        BatchAssembler.emitRun(coalesced, 0, 40, QUADS);
        BatchAssembler.emitRun(separate, 0, 16, QUADS);
        BatchAssembler.emitRun(separate, 16, 40, QUADS);

        assertEquals(1, coalesced.size());
        assertEquals(2, separate.size());
        assertEquals(separate.drawnQuads(), coalesced.drawnQuads(), "coalescing changed which quads are drawn");
    }

    @Test
    void mergedElementCountIsTheSumOfWhatItReplaces() {
        RecordingSink coalesced = new RecordingSink();
        RecordingSink separate = new RecordingSink();

        BatchAssembler.emitRun(coalesced, 100, 232, QUADS);
        BatchAssembler.emitRun(separate, 100, 148, QUADS);
        BatchAssembler.emitRun(separate, 148, 232, QUADS);

        int summed = separate.commands.stream().mapToInt(Command::elementCount).sum();
        assertEquals(summed, coalesced.commands.get(0).elementCount());
    }

    @Test
    void emptySpansProduceNoCommand() {
        RecordingSink sink = new RecordingSink();

        BatchAssembler.emitRun(sink, 64, 64, QUADS);

        assertTrue(sink.isEmpty(), "a zero-length span must not produce a draw command");
    }

    @Test
    void nonAbuttingSpansStaySeparate() {
        RecordingSink sink = new RecordingSink();

        BatchAssembler.emitRun(sink, 0, 16, QUADS);
        BatchAssembler.emitRun(sink, 32, 48, QUADS);

        assertEquals(2, sink.size());
        assertEquals(List.of(0, 1, 2, 3, 8, 9, 10, 11), sink.drawnQuads(), "a gap must not be swallowed");
    }

    private static final int WHOLE_SECTION_LAST_FACING = ModelQuadFacing.COUNT - 1;

    private static long buildSections(int[] quadsPerSection, int[] gapQuadsBefore) {
        var layout = SectionRenderDataUnsafe.Strategy.COMPACT;
        long base = layout.allocateHeap(quadsPerSection.length);

        int vertexCursor = 0;

        for (int section = 0; section < quadsPerSection.length; section++) {
            vertexCursor += gapQuadsBefore[section] * 4;

            Map<ModelQuadFacing, VertexRange> ranges = new EnumMap<>(ModelQuadFacing.class);
            ranges.put(ModelQuadFacing.UNASSIGNED, new VertexRange(0, quadsPerSection[section] * 4));

            layout.writeMeshes(layout.heapPointer(base, section), vertexCursor, 0, ranges, QUADS);
            vertexCursor += quadsPerSection[section] * 4;
        }

        return base;
    }

    private static byte[] sectionIndices(int count) {
        byte[] indices = new byte[count];
        for (int i = 0; i < count; i++) {
            indices[i] = (byte) i;
        }
        return indices;
    }

    @Test
    void contiguousSectionsCollapseToOneCommand() {
        int[] quads = { 3, 5, 7, 2 };
        long base = buildSections(quads, new int[quads.length]);

        try {
            RecordingSink sink = new RecordingSink();
            BatchAssembler.fillSingleRun(sink, base, sectionIndices(quads.length), quads.length, false, QUADS, 0, WHOLE_SECTION_LAST_FACING);

            assertEquals(1, sink.size(), "four contiguous sections should collapse into one command");
            assertEquals(17 * 6, sink.commands.get(0).elementCount(), "merged command must cover every quad");
            assertEquals(0, sink.commands.get(0).baseVertex());
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(base);
        }
    }

    @Test
    void aGapBreaksTheRun() {
        int[] quads = { 3, 5, 7 };
        long base = buildSections(quads, new int[] { 0, 4, 0 });

        try {
            RecordingSink sink = new RecordingSink();
            BatchAssembler.fillSingleRun(sink, base, sectionIndices(quads.length), quads.length, false, QUADS, 0, WHOLE_SECTION_LAST_FACING);

            assertEquals(2, sink.size(), "the gap must split the run");
            assertEquals(3 * 6, sink.commands.get(0).elementCount());
            assertEquals(12 * 6, sink.commands.get(1).elementCount(), "the last two sections are still contiguous");
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(base);
        }
    }

    @Test
    void coalescingNeverChangesWhichQuadsAreDrawn() {
        int[] quads = { 3, 5, 7, 2, 9 };
        int[] gaps = { 0, 0, 4, 0, 2 };
        long base = buildSections(quads, gaps);

        try {
            RecordingSink merged = new RecordingSink();
            BatchAssembler.fillSingleRun(merged, base, sectionIndices(quads.length), quads.length, false, QUADS, 0, WHOLE_SECTION_LAST_FACING);

            RecordingSink perSection = new RecordingSink();
            var layout = SectionRenderDataUnsafe.Strategy.COMPACT;
            for (int section = 0; section < quads.length; section++) {
                long ptr = layout.heapPointer(base, section);
                BatchAssembler.emitRun(perSection, layout.getVertexOffset(ptr, 0), layout.getRunVertexEnd(ptr, WHOLE_SECTION_LAST_FACING, QUADS), QUADS);
            }

            assertTrue(merged.size() < perSection.size(), "this layout should have coalesced something");
            assertEquals(perSection.drawnQuads(), merged.drawnQuads(), "coalescing changed the drawn primitives");
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(base);
        }
    }

    @Test
    void reverseOrderCoalescesWhenSectionsRunBackwards() {
        int[] quads = { 4, 4, 4 };
        long base = buildSections(quads, new int[quads.length]);

        try {
            RecordingSink sink = new RecordingSink();
            BatchAssembler.fillSingleRun(sink, base, sectionIndices(quads.length), quads.length, true, QUADS, 0, WHOLE_SECTION_LAST_FACING);

            assertEquals(3, sink.size(), "reverse iteration cannot coalesce ascending allocations");
            assertEquals(List.of(8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3), sink.drawnQuads(), "reverse order must be preserved");
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(base);
        }
    }
}
