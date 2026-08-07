package org.embeddedt.embeddium.impl.render.chunk.data;

import org.embeddedt.embeddium.impl.gl.util.VertexRange;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SectionRenderDataLayoutTest {
    private static final ChunkPrimitiveType QUADS = new StubPrimitiveType(4, 6);
    private static final ChunkPrimitiveType TRIANGLES = new StubPrimitiveType(3, 3);

    private record StubPrimitiveType(int verticesPerPrimitive, int elementsPerPrimitive) implements ChunkPrimitiveType {
        @Override
        public int getVerticesPerPrimitive() {
            return this.verticesPerPrimitive;
        }

        @Override
        public int getIndexBufferElementsPerPrimitive() {
            return this.elementsPerPrimitive;
        }

        @Override
        public void generateSimpleIndexBuffer(ByteBuffer indexBuffer, int numPrimitives) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void generateSortedIndexBuffer(ByteBuffer indexBuffer, int numPrimitives, TranslucentQuadAnalyzer.SortState chunkData, float x, float y, float z) {
            throw new UnsupportedOperationException();
        }
    }

    private static Map<ModelQuadFacing, VertexRange> ranges(ChunkPrimitiveType type, int... primitivesPerFacing) {
        Map<ModelQuadFacing, VertexRange> map = new EnumMap<>(ModelQuadFacing.class);
        int start = 0;

        for (int facing = 0; facing < primitivesPerFacing.length; facing++) {
            int vertexCount = primitivesPerFacing[facing] * type.getVerticesPerPrimitive();

            if (vertexCount > 0) {
                map.put(ModelQuadFacing.VALUES[facing], new VertexRange(start, vertexCount));
            }

            start += vertexCount;
        }

        return map;
    }

    private static long alloc(SectionRenderDataUnsafe.Strategy strategy) {
        long ptr = strategy.allocateHeap(1);
        assertNotEquals(0L, ptr, "allocation failed");
        return ptr;
    }

    @Test
    void bothLayoutsAgreeOnEveryPerFacingField() {
        Random random = new Random(20260801L);

        for (int trial = 0; trial < 200; trial++) {
            int[] primitives = new int[ModelQuadFacing.COUNT];
            for (int facing = 0; facing < primitives.length; facing++) {
                primitives[facing] = random.nextInt(4) == 0 ? 0 : random.nextInt(64);
            }

            int vertexBase = random.nextInt(100_000);
            var meshes = ranges(QUADS, primitives);

            long full = alloc(SectionRenderDataUnsafe.Strategy.FULL);
            long compact = alloc(SectionRenderDataUnsafe.Strategy.COMPACT);

            try {
                SectionRenderDataUnsafe.Strategy.FULL.writeMeshes(full, vertexBase, 0, meshes, QUADS);
                SectionRenderDataUnsafe.Strategy.COMPACT.writeMeshes(compact, vertexBase, 0, meshes, QUADS);

                assertEquals(SectionRenderDataUnsafe.getSliceMask(full), SectionRenderDataUnsafe.getSliceMask(compact), "slice mask must be layout independent");

                for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                    assertEquals(SectionRenderDataUnsafe.Strategy.FULL.getVertexOffset(full, facing),
                            SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(compact, facing),
                            "vertex offset for facing " + facing);

                    assertEquals(SectionRenderDataUnsafe.Strategy.FULL.getElementCount(full, facing, QUADS),
                            SectionRenderDataUnsafe.Strategy.COMPACT.getElementCount(compact, facing, QUADS),
                            "element count for facing " + facing);

                    assertEquals(SectionRenderDataUnsafe.Strategy.FULL.getRunVertexEnd(full, facing, QUADS),
                            SectionRenderDataUnsafe.Strategy.COMPACT.getRunVertexEnd(compact, facing, QUADS),
                            "run end for facing " + facing);
                }
            } finally {
                SectionRenderDataUnsafe.Strategy.FULL.freeHeap(full);
                SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(compact);
            }
        }
    }

    @Test
    void mergedRunElementCountEqualsSumOfItsFacings() {
        for (ChunkPrimitiveType type : new ChunkPrimitiveType[] { QUADS, TRIANGLES }) {
            var meshes = ranges(type, 3, 0, 7, 11, 0, 5, 2);
            long ptr = alloc(SectionRenderDataUnsafe.Strategy.COMPACT);

            try {
                SectionRenderDataUnsafe.Strategy.COMPACT.writeMeshes(ptr, 512, 0, meshes, type);

                for (int first = 0; first < ModelQuadFacing.COUNT; first++) {
                    for (int last = first; last < ModelQuadFacing.COUNT; last++) {
                        int summed = 0;
                        for (int facing = first; facing <= last; facing++) {
                            summed += SectionRenderDataUnsafe.Strategy.COMPACT.getElementCount(ptr, facing, type);
                        }

                        int start = SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(ptr, first);
                        int end = SectionRenderDataUnsafe.Strategy.COMPACT.getRunVertexEnd(ptr, last, type);
                        int merged = SectionRenderDataUnsafe.elementsForVertices(end - start, type);

                        assertEquals(summed, merged, "run [" + first + ", " + last + "]");
                    }
                }
            } finally {
                SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(ptr);
            }
        }
    }

    @Test
    void compactRebasePreservesEveryFacingSpan() {
        var meshes = ranges(QUADS, 5, 0, 9, 0, 0, 13, 1);

        long ptr = alloc(SectionRenderDataUnsafe.Strategy.COMPACT);

        try {
            SectionRenderDataUnsafe.Strategy.COMPACT.writeMeshes(ptr, 1024, 0, meshes, QUADS);

            int[] spansBefore = new int[ModelQuadFacing.COUNT];
            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                spansBefore[facing] = SectionRenderDataUnsafe.Strategy.COMPACT.getElementCount(ptr, facing, QUADS);
            }

            SectionRenderDataUnsafe.Strategy.COMPACT.rebase(ptr, 8192, 0, QUADS);

            assertEquals(8192, SectionRenderDataUnsafe.Strategy.COMPACT.getVertexOffset(ptr, 0), "rebased base offset");

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                assertEquals(spansBefore[facing], SectionRenderDataUnsafe.Strategy.COMPACT.getElementCount(ptr, facing, QUADS), "element count for facing " + facing + " changed across rebase");
            }
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(ptr);
        }
    }

    @Test
    void fullRebasePreservesEveryFacingSpan() {
        var meshes = ranges(QUADS, 5, 0, 9, 0, 0, 13, 1);

        long ptr = alloc(SectionRenderDataUnsafe.Strategy.FULL);

        try {
            SectionRenderDataUnsafe.Strategy.FULL.writeMeshes(ptr, 1024, 256, meshes, QUADS);

            int[] spansBefore = new int[ModelQuadFacing.COUNT];
            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                spansBefore[facing] = SectionRenderDataUnsafe.Strategy.FULL.getElementCount(ptr, facing, QUADS);
            }

            SectionRenderDataUnsafe.Strategy.FULL.rebase(ptr, 8192, 512, QUADS);

            assertEquals(8192, SectionRenderDataUnsafe.Strategy.FULL.getVertexOffset(ptr, 0), "rebased base offset");
            assertEquals(512, SectionRenderDataUnsafe.Strategy.FULL.getIndexOffset(ptr, 0), "rebased index offset");

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                assertEquals(spansBefore[facing], SectionRenderDataUnsafe.Strategy.FULL.getElementCount(ptr, facing, QUADS), "element count for facing " + facing + " changed across rebase");
            }
        } finally {
            SectionRenderDataUnsafe.Strategy.FULL.freeHeap(ptr);
        }
    }

    @Test
    void compactDrawsFromTheSharedIndexBufferSoEveryFacingHasAZeroIndexOffset() {
        var meshes = ranges(QUADS, 4, 4, 4, 4, 4, 4, 4);
        long ptr = alloc(SectionRenderDataUnsafe.Strategy.COMPACT);

        try {
            SectionRenderDataUnsafe.Strategy.COMPACT.writeMeshes(ptr, 0, 4096, meshes, QUADS);

            for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                assertEquals(0, SectionRenderDataUnsafe.Strategy.COMPACT.getIndexOffset(ptr, facing), "facing " + facing + " must draw from the shared index buffer");
            }
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(ptr);
        }
    }

    @Test
    void compactRejectsPerFacingIndexOffsets() {
        long ptr = alloc(SectionRenderDataUnsafe.Strategy.COMPACT);

        try {
            assertThrows(UnsupportedOperationException.class, () -> SectionRenderDataUnsafe.Strategy.COMPACT.writeIndexOffsets(ptr, 0, QUADS));
        } finally {
            SectionRenderDataUnsafe.Strategy.COMPACT.freeHeap(ptr);
        }
    }

    @Test
    void compactIsSmallerThanFull() {
        assertEquals(36, SectionRenderDataUnsafe.Strategy.COMPACT.getStride());
        assertEquals(92, SectionRenderDataUnsafe.Strategy.FULL.getStride());
    }

    @Test
    void clearedRowsHaveNoPopulatedFacings() {
        var meshes = ranges(QUADS, 1, 2, 3, 4, 5, 6, 7);

        for (var strategy : SectionRenderDataUnsafe.Strategy.values()) {
            long ptr = alloc(strategy);

            try {
                strategy.writeMeshes(ptr, 64, 64, meshes, QUADS);
                assertNotEquals(0, SectionRenderDataUnsafe.getSliceMask(ptr));

                strategy.clear(ptr);
                assertEquals(0, SectionRenderDataUnsafe.getSliceMask(ptr), strategy + " slice mask after clear");

                for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                    assertEquals(0, strategy.getElementCount(ptr, facing, QUADS), strategy + " element count for facing " + facing + " after clear");
                }
            } finally {
                strategy.freeHeap(ptr);
            }
        }
    }

    @Test
    void heapRowsAreStridedAndZeroInitialized() {
        for (var strategy : SectionRenderDataUnsafe.Strategy.values()) {
            long base = strategy.allocateHeap(8);
            assertNotEquals(0L, base);

            try {
                for (int i = 0; i < 8; i++) {
                    assertEquals(base + (i * strategy.getStride()), strategy.heapPointer(base, i), strategy + " row " + i);
                    assertEquals(0, LWJGL.memGetInt(strategy.heapPointer(base, i)), strategy + " row " + i + " not zeroed");
                }
            } finally {
                strategy.freeHeap(base);
            }
        }
    }
}
