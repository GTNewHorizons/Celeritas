package org.embeddedt.embeddium.impl.render.chunk.map;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;
import org.embeddedt.embeddium.impl.util.PositionUtil;

public class ChunkTrackerImpl implements ChunkTracker {
    protected final Long2IntOpenHashMap chunkStatus = new Long2IntOpenHashMap();
    protected final LongOpenHashSet chunkReady = new LongOpenHashSet();

    protected final LongSet unloadQueue = new LongOpenHashSet();
    protected final LongSet loadQueue = new LongOpenHashSet();

    public ChunkTrackerImpl() {

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

        this.updateNeighbors(x, z);
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

        this.updateNeighbors(x, z);
    }

    protected void updateNeighbors(int x, int z) {
        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                this.updateMerged(ox + x, oz + z);
            }
        }
    }

    protected void updateMerged(int x, int z) {
        long key = PositionUtil.packChunk(x, z);

        int flags = this.chunkStatus.get(key);

        for (int ox = -1; ox <= 1; ox++) {
            for (int oz = -1; oz <= 1; oz++) {
                flags &= this.chunkStatus.get(PositionUtil.packChunk(ox + x, oz + z));
            }
        }

        if (flags == ChunkStatus.FLAG_ALL) {
            if (this.chunkReady.add(key) && !this.unloadQueue.remove(key)) {
                this.loadQueue.add(key);
            }
        } else {
            if (this.chunkReady.remove(key) && !this.loadQueue.remove(key)) {
                this.unloadQueue.add(key);
            }
        }
    }

    @Override
    public void forEachReady(RenderSectionManager sectionManager) {
        int min = sectionManager.getMinSection();
        int max = sectionManager.getMaxSection();

        forEachChunk(chunkReady, (x, z) -> {
            for (int y = min; y < max; y++) {
                sectionManager.onSectionAdded(x, y, z);
            }
        });
    }

    @Override
    public void forEachEvent(RenderSectionManager sectionManager) {
        int min = sectionManager.getMinSection();
        int max = sectionManager.getMaxSection();

        forEachChunk(unloadQueue, (x, z) -> {
            for (int y = min; y < max; y++) {
                sectionManager.onSectionRemoved(x, y, z);
            }
        });
        this.unloadQueue.clear();

        forEachChunk(loadQueue, (x, z) -> {
            for (int y = min; y < max; y++) {
                sectionManager.onSectionAdded(x, y, z);
            }
        });
        this.loadQueue.clear();
    }

    protected interface ChunkConsumer {
        void accept(int x, int z);
    }

    protected static void forEachChunk(LongCollection queue, ChunkConsumer consumer) {
        var iterator = queue.iterator();

        while (iterator.hasNext()) {
            var pos = iterator.nextLong();

            var x = PositionUtil.unpackChunkX(pos);
            var z = PositionUtil.unpackChunkZ(pos);

            consumer.accept(x, z);
        }
    }
}