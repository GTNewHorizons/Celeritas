package org.embeddedt.embeddium.impl.render.chunk.map;

import org.embeddedt.embeddium.impl.render.chunk.RenderSectionManager;

public interface ChunkTracker {
    /// Reverts to vanilla chunk presence: a chunk renders with neighbors missing, at the cost of erroneous edge faces.
    void setFastMode(boolean fastMode);
    boolean isFastModeEnabled();

    void onChunkStatusAdded(int x, int z, int flags);
    void onChunkStatusRemoved(int x, int z, int flags);

    void forEachReady(RenderSectionManager sectionManager);
    void forEachEvent(RenderSectionManager sectionManager);
}
