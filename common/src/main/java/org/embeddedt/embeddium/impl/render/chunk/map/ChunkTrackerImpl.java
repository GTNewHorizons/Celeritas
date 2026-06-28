package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.*;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.util.PositionUtil;

import static org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus.FLAG_ALL;

public class ChunkTrackerImpl implements ChunkTracker {
    /// The status of every chunk (see [ChunkStatus]).
    protected final Long2IntOpenHashMap chunkStatus = new Long2IntOpenHashMap();

    /// The set of chunks that are actively rendered.
    protected final LongOpenHashSet chunkReady = new LongOpenHashSet();

    /// Any pending loads or unloads
    protected final Long2ObjectOpenHashMap<PendingState> pendingOperations = new Long2ObjectOpenHashMap<>();

    /// When true, chunks will be loaded regardless of whether their neighbours are loaded (assuming they are themselves
    /// fully loaded).
    private boolean fastMode = false;

    public ChunkTrackerImpl() {

    }

    @Override
    public void setFastMode(boolean fastMode) {
        this.fastMode = fastMode;

        var iter = this.chunkStatus.long2IntEntrySet().fastIterator();

        while (iter.hasNext()) {
            Long2IntMap.Entry e = iter.next();

            int x = PositionUtil.unpackChunkX(e.getLongKey());
            int z = PositionUtil.unpackChunkZ(e.getLongKey());

            // Poll every chunk regardless of whether it's fully loaded, because we might need to remove it
            pollChunk(x, z);
        }
    }

    @Override
    public boolean isFastModeEnabled() {
        return fastMode;
    }

    @Override
    public void onChunkStatusAdded(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        var cur = prev | flags;

        if (prev == cur) {
            return;
        }

        this.chunkStatus.put(key, cur);

        this.onChunkChanged(x, z);
    }

    @Override
    public void onChunkStatusRemoved(int x, int z, int flags) {
        var key = PositionUtil.packChunk(x, z);

        var prev = this.chunkStatus.get(key);
        int cur = prev & ~flags;

        if (prev == cur) {
            return;
        }

        if (cur == this.chunkStatus.defaultReturnValue()) {
            this.chunkStatus.remove(key);
        } else {
            this.chunkStatus.put(key, cur);
        }

        this.onChunkChanged(x, z);
    }

    protected void onChunkChanged(int x, int z) {
        this.pollChunk(x, z);

        if (!fastMode) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    if (ox == 0 && oz == 0) continue;

                    this.pollChunk(ox + x, oz + z);
                }
            }
        }
    }

    protected void pollChunk(int x, int z) {
        long key = PositionUtil.packChunk(x, z);

        boolean renderable = canChunkRender(x, z);
        boolean isRendering = chunkReady.contains(key);

        if (renderable != isRendering) {
            pendingOperations.put(key, renderable ? PendingState.Add : PendingState.Remove);
        } else {
            pendingOperations.remove(key);
        }
    }

    protected boolean canChunkRender(int x, int z) {
        long key = PositionUtil.packChunk(x, z);

        // Chunk itself can't render
        if (this.chunkStatus.get(key) != FLAG_ALL) return false;

        // If fast mode is disabled check the neighbours
        if (!fastMode) {
            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    // A neighbour isn't fully loaded: this chunk can't render
                    if (this.chunkStatus.get(PositionUtil.packChunk(ox + x, oz + z)) != FLAG_ALL) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void forEachReady(RenderSectionManager sectionManager) {
        int min = sectionManager.getMinSection();
        int max = sectionManager.getMaxSection();

        var iter = pendingOperations.long2ObjectEntrySet().fastIterator();

        // Apply any pending operations (which typically come from changing fastMode)
        while (iter.hasNext()) {
            Long2ObjectMap.Entry<PendingState> e = iter.next();

            switch (e.getValue()) {
                case Add -> {
                    chunkReady.add(e.getLongKey());
                }
                case Remove -> {
                    chunkReady.remove(e.getLongKey());
                }
            }
        }

        this.pendingOperations.clear();

        var iter2 = chunkReady.iterator();

        while (iter2.hasNext()) {
            long key = iter2.nextLong();

            int x = PositionUtil.unpackChunkX(key);
            int z = PositionUtil.unpackChunkZ(key);

            for (int y = min; y < max; y++) {
                sectionManager.onSectionAdded(x, y, z);
            }
        }
    }

    @Override
    public void forEachEvent(RenderSectionManager sectionManager) {
        int min = sectionManager.getMinSection();
        int max = sectionManager.getMaxSection();

        var iter = pendingOperations.long2ObjectEntrySet().fastIterator();

        while (iter.hasNext()) {
            Long2ObjectMap.Entry<PendingState> e = iter.next();

            int x = PositionUtil.unpackChunkX(e.getLongKey());
            int z = PositionUtil.unpackChunkZ(e.getLongKey());

            switch (e.getValue()) {
                case Add -> {
                    chunkReady.add(e.getLongKey());

                    for (int y = min; y < max; y++) {
                        sectionManager.onSectionAdded(x, y, z);
                    }
                }
                case Remove -> {
                    chunkReady.remove(e.getLongKey());

                    for (int y = min; y < max; y++) {
                        sectionManager.onSectionRemoved(x, y, z);
                    }
                }
            }
        }

        this.pendingOperations.clear();
    }
}