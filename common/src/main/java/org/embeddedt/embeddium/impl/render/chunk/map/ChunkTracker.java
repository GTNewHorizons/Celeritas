package org.embeddedt.embeddium.impl.render.chunk.map;

import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;

public interface ChunkTracker {
    void onChunkStatusAdded(int x, int z, int flags);
    void onChunkStatusRemoved(int x, int z, int flags);

    void forEachReady(RenderSectionManager sectionManager);
    void forEachEvent(RenderSectionManager sectionManager);

    /// Creates a new [ChunkTracker] instance. This is pulled into a static method so that it can be targeted by mixins
    /// easily.
    static ChunkTracker create() {
        return new ChunkTrackerImpl();
    }
}
