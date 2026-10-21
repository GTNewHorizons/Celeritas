package org.embeddedt.embeddium.impl.render.chunk.region;

public final class TestRegions {
    private TestRegions() {
    }

    public static RenderRegion create(int x, int y, int z, int id) {
        return new RenderRegion(x, y, z, id, null, 0);
    }
}
