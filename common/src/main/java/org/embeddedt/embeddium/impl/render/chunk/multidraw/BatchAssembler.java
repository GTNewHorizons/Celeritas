package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.LocalSectionIndex;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataUnsafe;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.embeddedt.embeddium.impl.util.BitwiseMath;

/**
 * Turns a region's visible section list into draw commands.
 */
public final class BatchAssembler {
    /**
     * Returned by {@link #uniformCullMask} when sections in the region have differing cull masks.
     */
    private static final int MASK_NOT_UNIFORM = -1;

    private BatchAssembler() {
    }

    /**
     * Assembles every draw command for one region into {@code sink}, which is cleared first.
     *
     * @param useBlockFaceCulling whether the caller wants face culling; already false for sorted passes
     */
    public static void fillRegion(DrawCommandSink sink, RenderRegion region, SectionRenderDataStorage storage, ChunkRenderList renderList, CameraTransform camera, TerrainRenderPass pass, boolean useBlockFaceCulling) {
        sink.clear();

        var sections = renderList.getSectionsWithGeometry();
        int sectionCount = renderList.getSectionsWithGeometryCount();

        if (sectionCount == 0) {
            return;
        }

        boolean reverse = pass.isReverseOrder();
        var primitiveType = storage.getPrimitiveType();
        final long pBase = storage.getBasePointer();

        if (pass.isSorted()) {
            // Sorted passes keep the FULL layout: real per-facing index offsets, so no merging is possible, and no
            // culling is applied. In practice only UNASSIGNED is ever populated.
            fillSorted(sink, pBase, sections, sectionCount, reverse, primitiveType);
            return;
        }

        if (!useBlockFaceCulling) {
            // Every facing visible everywhere: one run covering the whole section.
            fillSingleRun(sink, pBase, sections, sectionCount, reverse, primitiveType, 0, ModelQuadFacing.COUNT - 1);
            return;
        }

        // Determine whether every section in the region will use the same cull mask. If so, the relevant runs
        // can be precomputed at the region level rather than once per section.
        int uniformMask = uniformCullMask(region, camera);

        if (uniformMask == MASK_NOT_UNIFORM) {
            fillPerSectionRuns(sink, region, pBase, sections, sectionCount, reverse, primitiveType, camera);
            return;
        }

        long runs = packRuns(uniformMask);
        int runCount = runCount(runs);

        if (runCount == 0) {
            return;
        }

        if (runCount == 1) {
            fillSingleRun(sink, pBase, sections, sectionCount, reverse, primitiveType,
                    runFirst(runs, 0), runLast(runs, 0));
        } else {
            fillUniformRuns(sink, pBase, sections, sectionCount, reverse, primitiveType, runs, runCount);
        }
    }

    /**
     * {@return the cull mask that every section in the region shares, or {@link #MASK_NOT_UNIFORM} if they differ}
     * <p>
     * Testing all sections would cost more than it saves, but the two opposite corners are enough to decide it.
     * Consider the POS_X bit: it is set when the camera lies past the section's lower X bound. Stepping a section along
     * +X only raises that bound, so across the region the bit can flip from set to clear but never back. Every other bit
     * behaves the same way on its own axis. A bit that agrees at the lowest and the highest section therefore agrees at
     * every section in between, so if the two corner masks are equal, that mask is the mask of the whole region.
     */
    private static int uniformCullMask(RenderRegion region, CameraTransform camera) {
        int minX = region.getChunkX(), minY = region.getChunkY(), minZ = region.getChunkZ();
        int maxX = minX + RenderRegion.REGION_WIDTH - 1;
        int maxY = minY + RenderRegion.REGION_HEIGHT - 1;
        int maxZ = minZ + RenderRegion.REGION_LENGTH - 1;

        int atMin = getVisibleFaces(camera.intX, camera.intY, camera.intZ, minX, minY, minZ);
        int atMax = getVisibleFaces(camera.intX, camera.intY, camera.intZ, maxX, maxY, maxZ);

        return atMin == atMax ? atMin : MASK_NOT_UNIFORM;
    }

    // A run plan is a cull mask decomposed into maximal groups of consecutive set bits, packed into one long.
    //
    //   bits  0..3   run 0 first facing      bits  4..7   run 0 last facing
    //   bits  8..11  run 1 first facing      bits 12..15  run 1 last facing
    //   ...                                  ...
    //   bits 32..35  run count
    //
    // Run i occupies nibbles 2i and 2i+1. A 7-bit facing mask can decompose into at most 4 runs (0b1010101),
    // which need two 4-bit nibbles each. At 8 bits per run, that brings us to precisely the 32 bits below the count.
    private static final int RUN_COUNT_SHIFT = 32;

    /**
     * Decomposes a packed cull mask of visible facings into a run plan.
     */
    public static long packRuns(int mask) {
        long runs = 0;
        int count = 0;

        while (mask != 0) {
            int first = Integer.numberOfTrailingZeros(mask);

            // Length of the run of set bits starting at `first`. Shifting the run to start at bit 0 and complementing
            // turns its end into the lowest set bit, so it can be located with another NTZ.
            int length = Integer.numberOfTrailingZeros(~(mask >>> first));
            int last = (first + length) - 1;

            int shift = count << 3;
            runs |= ((long) first << shift) | ((long) last << (shift + 4));
            count++;

            mask &= ~(((1 << length) - 1) << first);
        }

        return runs | ((long) count << RUN_COUNT_SHIFT);
    }

    public static int runCount(long runs) {
        return (int) (runs >>> RUN_COUNT_SHIFT) & 0xF;
    }

    public static int runFirst(long runs, int run) {
        return (int) (runs >>> (run << 3)) & 0xF;
    }

    public static int runLast(long runs, int run) {
        return (int) (runs >>> ((run << 3) + 4)) & 0xF;
    }

    private static final int MODEL_UNASSIGNED = ModelQuadFacing.UNASSIGNED.ordinal();
    private static final int MODEL_POS_X      = ModelQuadFacing.POS_X.ordinal();
    private static final int MODEL_POS_Y      = ModelQuadFacing.POS_Y.ordinal();
    private static final int MODEL_POS_Z      = ModelQuadFacing.POS_Z.ordinal();

    private static final int MODEL_NEG_X      = ModelQuadFacing.NEG_X.ordinal();
    private static final int MODEL_NEG_Y      = ModelQuadFacing.NEG_Y.ordinal();
    private static final int MODEL_NEG_Z      = ModelQuadFacing.NEG_Z.ordinal();

    /**
     * When true, block face culling checks are inverted to debug if the feature works properly.
     */
    private static final boolean DEBUG_BLOCK_FACE_CULLING = false;

    public static int getVisibleFaces(int originX, int originY, int originZ, int chunkX, int chunkY, int chunkZ) {
        // This is carefully written so that we can keep everything branch-less.
        //
        // Normally, this would be a ridiculous way to handle the problem. But the Hotspot VM's
        // heuristic for generating SETcc/CMOV instructions is broken, and it will always create a
        // branch even when a trivial ternary is encountered.
        //
        // For example, the following will never be transformed into a SETcc:
        //   (a > b) ? 1 : 0
        //
        // So we have to instead rely on sign-bit extension and masking (which generates a ton
        // of unnecessary instructions) to get this to be branch-less.
        //
        // To do this, we can transform the previous expression into the following.
        //   (b - a) >> 31
        //
        // This works because if (a > b) then (b - a) will always create a negative number. We then shift the sign bit
        // into the least significant bit's position (which also discards any bits following the sign bit) to get the
        // output we are looking for.
        //
        // If you look at the output which LLVM produces for a series of ternaries, you will instantly become distraught,
        // because it manages to a) correctly evaluate the cost of instructions, and b) go so far
        // as to actually produce vector code.  (https://godbolt.org/z/GaaEx39T9)

        int boundsMinX = (chunkX << 4), boundsMaxX = boundsMinX + 16;
        int boundsMinY = (chunkY << 4), boundsMaxY = boundsMinY + 16;
        int boundsMinZ = (chunkZ << 4), boundsMaxZ = boundsMinZ + 16;

        // the "unassigned" plane is always front-facing, since we can't check it
        int planes = (1 << MODEL_UNASSIGNED);

        if (DEBUG_BLOCK_FACE_CULLING) {
            planes |= BitwiseMath.lessThan(originX, (boundsMaxX + 3)) << MODEL_POS_X;
            planes |= BitwiseMath.lessThan(originY, (boundsMaxY + 3)) << MODEL_POS_Y;
            planes |= BitwiseMath.lessThan(originZ, (boundsMaxZ + 3)) << MODEL_POS_Z;

            planes |= BitwiseMath.greaterThan(originX, (boundsMinX - 3)) << MODEL_NEG_X;
            planes |= BitwiseMath.greaterThan(originY, (boundsMinY - 3)) << MODEL_NEG_Y;
            planes |= BitwiseMath.greaterThan(originZ, (boundsMinZ - 3)) << MODEL_NEG_Z;
        } else {
            planes |= BitwiseMath.greaterThan(originX, (boundsMinX - 3)) << MODEL_POS_X;
            planes |= BitwiseMath.greaterThan(originY, (boundsMinY - 3)) << MODEL_POS_Y;
            planes |= BitwiseMath.greaterThan(originZ, (boundsMinZ - 3)) << MODEL_POS_Z;

            planes |= BitwiseMath.lessThan(originX, (boundsMaxX + 3)) << MODEL_NEG_X;
            planes |= BitwiseMath.lessThan(originY, (boundsMaxY + 3)) << MODEL_NEG_Y;
            planes |= BitwiseMath.lessThan(originZ, (boundsMaxZ + 3)) << MODEL_NEG_Z;
        }

        return planes;
    }

    // The fill loops below share a structure, and differ only in how much of the run plan varies per section.

    /**
     * Sentinel for "nothing accumulated yet". Vertex offsets are never negative.
     */
    private static final int NO_PENDING = -1;

    /**
     * Emits one command for a vertex span, in the shared-index-buffer form every unsorted command takes.
     */
    static void emitRun(DrawCommandSink sink, int start, int end, ChunkPrimitiveType primitiveType) {
        sink.push(start, SectionRenderDataUnsafe.elementsForVertices(end - start, primitiveType), 0L);
    }

    static void fillSingleRun(DrawCommandSink sink,
                              long pBase,
                              byte[] sections, int sectionCount, boolean reverse,
                              ChunkPrimitiveType primitiveType,
                              int firstFacing, int lastFacing) {
        final var layout = SectionRenderDataUnsafe.Strategy.COMPACT;
        final long stride = layout.getStride();

        int cursor = reverse ? sectionCount - 1 : 0;
        final int step = reverse ? -1 : 1;

        int pendingStart = NO_PENDING;
        int pendingEnd = NO_PENDING;

        for (int i = 0; i < sectionCount; i++, cursor += step) {
            long pMeshData = pBase + ((sections[cursor] & 0xFF) * stride);

            int start = layout.getVertexOffset(pMeshData, firstFacing);
            int end = layout.getRunVertexEnd(pMeshData, lastFacing, primitiveType);

            if (start == pendingEnd) {
                pendingEnd = end;
                continue;
            }

            if (pendingStart != NO_PENDING) {
                emitRun(sink, pendingStart, pendingEnd, primitiveType);
            }

            pendingStart = start;
            pendingEnd = end;
        }

        if (pendingStart != NO_PENDING) {
            emitRun(sink, pendingStart, pendingEnd, primitiveType);
        }
    }

    /**
     * Uniform cull mask, but with more than one run to emit
     */
    static void fillUniformRuns(DrawCommandSink sink,
                                long pBase,
                                byte[] sections, int sectionCount, boolean reverse,
                                ChunkPrimitiveType primitiveType,
                                long runs, int runCount) {
        final var layout = SectionRenderDataUnsafe.Strategy.COMPACT;
        final long stride = layout.getStride();

        int cursor = reverse ? sectionCount - 1 : 0;
        final int step = reverse ? -1 : 1;

        int pendingStart = NO_PENDING;
        int pendingEnd = NO_PENDING;

        for (int i = 0; i < sectionCount; i++, cursor += step) {
            long pMeshData = pBase + ((sections[cursor] & 0xFF) * stride);

            for (int run = 0; run < runCount; run++) {
                int firstFacing = runFirst(runs, run);
                int lastFacing = runLast(runs, run);

                int start = layout.getVertexOffset(pMeshData, firstFacing);
                int end = layout.getRunVertexEnd(pMeshData, lastFacing, primitiveType);

                if (start == pendingEnd) {
                    pendingEnd = end;
                    continue;
                }

                if (pendingStart != NO_PENDING) {
                    emitRun(sink, pendingStart, pendingEnd, primitiveType);
                }

                pendingStart = start;
                pendingEnd = end;
            }
        }

        if (pendingStart != NO_PENDING) {
            emitRun(sink, pendingStart, pendingEnd, primitiveType);
        }
    }

    /**
     * The worst case scenario, where the cull mask is not uniform across the region, and the cull mask must be
     * recomputed per section. Run planning is not used here.
     */
    static void fillPerSectionRuns(DrawCommandSink sink,
                                   RenderRegion region,
                                   long pBase,
                                   byte[] sections, int sectionCount, boolean reverse,
                                   ChunkPrimitiveType primitiveType,
                                   CameraTransform camera) {
        final var layout = SectionRenderDataUnsafe.Strategy.COMPACT;
        final long stride = layout.getStride();

        final int originX = region.getChunkX();
        final int originY = region.getChunkY();
        final int originZ = region.getChunkZ();

        final int cameraX = camera.intX, cameraY = camera.intY, cameraZ = camera.intZ;

        int cursor = reverse ? sectionCount - 1 : 0;
        final int step = reverse ? -1 : 1;

        int pendingStart = NO_PENDING;
        int pendingEnd = NO_PENDING;

        for (int i = 0; i < sectionCount; i++, cursor += step) {
            int sectionIndex = sections[cursor] & 0xFF;

            int mask = getVisibleFaces(cameraX, cameraY, cameraZ,
                    originX + LocalSectionIndex.unpackX(sectionIndex),
                    originY + LocalSectionIndex.unpackY(sectionIndex),
                    originZ + LocalSectionIndex.unpackZ(sectionIndex));

            long pMeshData = pBase + (sectionIndex * stride);

            // A do-while loop is chosen because UNASSIGNED is always marked as visible by getVisibleFaces, so the mask
            // is never zero and there is always at least one run to emit.
            do {
                int firstFacing = Integer.numberOfTrailingZeros(mask);
                int lastFacing = firstFacing + Integer.numberOfTrailingZeros(~(mask >>> firstFacing)) - 1;

                int start = layout.getVertexOffset(pMeshData, firstFacing);
                int end = layout.getRunVertexEnd(pMeshData, lastFacing, primitiveType);

                if (start == pendingEnd) {
                    pendingEnd = end;
                } else {
                    if (pendingStart != NO_PENDING) {
                        emitRun(sink, pendingStart, pendingEnd, primitiveType);
                    }

                    pendingStart = start;
                    pendingEnd = end;
                }

                // Runs are found in ascending order, so clear all bits below what we just consumed.
                mask &= -1 << (lastFacing + 1);
            } while (mask != 0);
        }

        if (pendingStart != NO_PENDING) {
            emitRun(sink, pendingStart, pendingEnd, primitiveType);
        }
    }

    /**
     * Sorted pass: FULL layout, one command per populated facing, real index offsets, no culling.
     */
    private static void fillSorted(DrawCommandSink sink,
                                   long pBase,
                                   byte[] sections, int sectionCount, boolean reverse,
                                   ChunkPrimitiveType primitiveType) {
        final var layout = SectionRenderDataUnsafe.Strategy.FULL;
        final long stride = layout.getStride();

        int cursor = reverse ? sectionCount - 1 : 0;
        final int step = reverse ? -1 : 1;

        for (int i = 0; i < sectionCount; i++, cursor += step) {
            long pMeshData = pBase + ((sections[cursor] & 0xFF) * stride);

            int sliceMask = SectionRenderDataUnsafe.getSliceMask(pMeshData);

            while (sliceMask != 0) {
                int facing = Integer.numberOfTrailingZeros(sliceMask);
                sliceMask &= sliceMask - 1;

                sink.push(layout.getVertexOffset(pMeshData, facing),
                        layout.getElementCount(pMeshData, facing, primitiveType),
                        layout.getIndexOffset(pMeshData, facing));
            }
        }
    }
}
