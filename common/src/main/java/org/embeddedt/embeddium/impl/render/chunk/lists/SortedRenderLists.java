package org.embeddedt.embeddium.impl.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.util.iterator.ReversibleObjectArrayIterator;

import java.util.Set;

public class SortedRenderLists implements ChunkRenderListIterable {
    private static final SortedRenderLists EMPTY = new SortedRenderLists(new ObjectArrayList<>());

    private final ObjectArrayList<ChunkRenderList> lists;
    private final ReferenceOpenHashSet<TerrainRenderPass> passes;
    private final int totalSectionsWithGeometry;

    SortedRenderLists(ObjectArrayList<ChunkRenderList> lists) {
        this.lists = lists;

        ReferenceOpenHashSet<TerrainRenderPass> usedPasses = new ReferenceOpenHashSet<>();
        int sections = 0;

        for (var list : lists) {
            usedPasses.addAll(list.getRegion().getPasses());
            sections += list.getSectionsWithGeometryCount();
        }

        this.passes = usedPasses;
        this.totalSectionsWithGeometry = sections;
    }

    @Override
    public int getTotalSectionsWithGeometry() {
        return this.totalSectionsWithGeometry;
    }

    @Override
    public ReversibleObjectArrayIterator<ChunkRenderList> iterator(boolean reverse) {
        return new ReversibleObjectArrayIterator<>(this.lists, reverse);
    }

    @Override
    public boolean hasPass(TerrainRenderPass pass) {
        return this.passes.contains(pass);
    }

    public Set<TerrainRenderPass> getPasses() {
        return this.passes;
    }

    public static SortedRenderLists empty() {
        return EMPTY;
    }
}
