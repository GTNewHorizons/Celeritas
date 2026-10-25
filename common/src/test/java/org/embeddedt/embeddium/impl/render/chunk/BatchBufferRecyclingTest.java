package org.embeddedt.embeddium.impl.render.chunk;

import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.IndirectMultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatchFactory;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.lists.RenderVisualsService;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.BatchAssembler;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.CachedBatch;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.region.TestRegions;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.sorting.SortState;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.taumc.celeritas.lwjgl.FakeGlLWJGLService;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

/** Buffer names created, uploaded and deleted by DefaultChunkRenderer.getRegionBatch. */
class BatchBufferRecyclingTest {
    private static final ChunkPrimitiveType QUADS = new ChunkPrimitiveType() {
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
        public void generateSortedIndexBuffer(ByteBuffer indexBuffer, int numPrimitives, SortState chunkData, float x, float y, float z) {
            throw new UnsupportedOperationException();
        }
    };

    private static final SectionRenderDataStorage.BatchCacheParams PARAMS = new SectionRenderDataStorage.BatchCacheParams(false);

    private MultiDrawBatchFactory previousFactory;
    private SectionRenderDataStorage storage;
    private RenderRegion region;
    private ChunkRenderList renderList;
    private TerrainRenderPass pass;
    private TestRenderer renderer;

    @BeforeEach
    void setUp() throws Exception {
        this.previousFactory = BatchAssembler.getBatchFactory();
        BatchAssembler.setBatchFactory(IndirectMultiDrawBatch::new);

        // The sections have no meshes, so every command is dropped, but the batch is still created and uploaded.
        this.storage = new SectionRenderDataStorage(QUADS, false, 1);
        this.region = TestRegions.create(0, 0, 0, 0);
        this.pass = (TerrainRenderPass) unsafe().allocateInstance(TerrainRenderPass.class);
        this.renderList = new ChunkRenderList(this.region);
        this.renderList.add(0, 1 << RenderVisualsService.HAS_BLOCK_GEOMETRY);

        this.renderer = new TestRenderer();
        FakeGlLWJGLService.reset();
    }

    @AfterEach
    void tearDown() {
        this.storage.delete();
        BatchAssembler.setBatchFactory(this.previousFactory);
    }

    @Test
    void invalidatedBatchHandsItsBufferToTheRebuild() {
        this.batch();
        this.storage.invalidateCachedBatches();
        this.batch();

        assertEquals(List.of("gen 1", "data 1", "data 1"), FakeGlLWJGLService.LOG);
    }

    @Test
    void staleBatchIsRetiredBeforeTheRebuildCreatesAName() {
        this.batch();
        this.renderList.add(1, 1 << RenderVisualsService.HAS_BLOCK_GEOMETRY);
        this.batch();

        assertEquals(List.of("gen 1", "data 1", "data 1"), FakeGlLWJGLService.LOG);
    }

    @Test
    void deletingTheStorageDeletesTheSpareBuffer() {
        this.batch();
        this.storage.invalidateCachedBatches();
        this.storage.delete();
        this.storage = new SectionRenderDataStorage(QUADS, false, 1);

        assertEquals(List.of("gen 1", "data 1", "del 1"), FakeGlLWJGLService.LOG);
    }

    private CachedBatch batch() {
        CachedBatch cached = this.renderer.getRegionBatch(this.commandList(), this.region, this.storage, this.renderList,
                new CameraTransform(0, 0, 0), this.pass, false, PARAMS);
        assertNotNull(cached);
        return cached;
    }

    private CommandList commandList() {
        return (CommandList) Proxy.newProxyInstance(CommandList.class.getClassLoader(), new Class<?>[] { CommandList.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "uploadData" -> {
                        GlMutableBuffer buffer = (GlMutableBuffer) args[0];
                        LWJGL.glBindBuffer(((GlBufferTarget) args[4]).getTargetParameter(), buffer.handle());
                        LWJGL.glBufferData(((GlBufferTarget) args[4]).getTargetParameter(), (long) args[2], (long) args[1], 0);
                        yield null;
                    }
                    case "bindBuffer" -> null;
                    default -> throw new UnsupportedOperationException("CommandList." + method.getName());
                });
    }

    private static final class TestRenderer extends DefaultChunkRenderer {
        TestRenderer() {
            super(null, null, null);
        }

        @Override
        protected void configureShaderInterface(ChunkShaderInterface shader) {
        }
    }

    private static Unsafe unsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
