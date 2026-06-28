package org.embeddedt.embeddium.impl.render.chunk.map;

import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;

public interface ChunkTracker {
    /// Enables fast mode, which causes the tracker to revert back to the vanilla chunk presence behaviour. In vanilla,
    /// a chunk renders even if its neighbours aren't loaded, which can cause issues with water not rendering proper
    /// along the edges of the world (it will generate erroneous faces). When fastMode is disabled, trackers will
    /// require all neighbours of a chunk to be loaded before the chunk itself can render.
    void setFastMode(boolean fastMode);
    boolean isFastModeEnabled();

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
