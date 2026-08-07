package org.embeddedt.embeddium.impl.render.chunk;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import org.embeddedt.embeddium.impl.gl.array.GlVertexArray;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.impl.gl.debug.GLDebug;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.gl.tessellation.*;
import org.embeddedt.embeddium.impl.render.chunk.compile.sorting.ChunkPrimitiveType;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderListIterable;
import org.embeddedt.embeddium.impl.render.chunk.lists.ChunkRenderList;
import org.embeddedt.embeddium.impl.render.chunk.data.SectionRenderDataStorage;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.BatchAssembler;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.DrawCommandSink;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.DirectMultiDrawEmitter;
import org.embeddedt.embeddium.impl.render.chunk.multidraw.MultiDrawEmitter;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;
import org.embeddedt.embeddium.impl.render.chunk.shader.ChunkShaderInterface;
import org.embeddedt.embeddium.impl.render.chunk.terrain.TerrainRenderPass;
import org.embeddedt.embeddium.impl.render.viewport.CameraTransform;
import java.util.Arrays;
import java.util.Iterator;

public abstract class DefaultChunkRenderer extends ShaderChunkRenderer {
    private final MultiDrawEmitter emitter;

    private final Reference2ReferenceMap<ChunkPrimitiveType, SharedQuadIndexBuffer> sharedIndexBuffers;

    private TerrainRenderPass currentRenderPass;
    private GlVertexFormat currentVertexFormat;

    private int[] regionCommandCounts = new int[256];

    public DefaultChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration) {
        this(device, renderPassConfiguration, new DirectMultiDrawEmitter());
    }

    public DefaultChunkRenderer(RenderDevice device, RenderPassConfiguration<?> renderPassConfiguration, MultiDrawEmitter emitter) {
        super(device, renderPassConfiguration);

        this.emitter = emitter;
        this.sharedIndexBuffers = new Reference2ReferenceOpenHashMap<>();
    }

    protected boolean useBlockFaceCulling() {
        return true;
    }

    protected final SharedQuadIndexBuffer getSharedIndexBuffer(ChunkPrimitiveType type, CommandList commandList) {
        var buffer = this.sharedIndexBuffers.get(type);
        if (buffer == null) {
            buffer = new SharedQuadIndexBuffer(commandList, type);
            this.sharedIndexBuffers.put(type, buffer);
        }
        return buffer;
    }

    protected abstract void configureShaderInterface(ChunkShaderInterface shader);

    @Override
    public void render(ChunkRenderMatrices matrices,
                       CommandList commandList,
                       ChunkRenderListIterable renderLists,
                       TerrainRenderPass renderPass,
                       CameraTransform occlusionCamera,
                       CameraTransform camera) {
        if (!renderLists.hasPass(renderPass)) {
            return;
        }

        this.begin(renderPass);

        // If there is no active program, shader compilation probably failed, and we can't render anything.
        if (this.activeProgram != null) {
            boolean useBlockFaceCulling = this.useBlockFaceCulling();

            GLDebug.pushGroup(770, renderPass.name() + " terrain pass");

            ChunkShaderInterface shader = this.activeProgram.getInterface();
            shader.setProjectionMatrix(matrices.projection());
            shader.setModelViewMatrix(matrices.modelView());

            this.currentRenderPass = renderPass;
            this.currentVertexFormat = this.renderPassConfiguration.getVertexTypeForPass(this.currentRenderPass).getVertexFormat();

            this.configureShaderInterface(shader);

            long timestamp = System.nanoTime();

            if (this.emitter.batchesWholePass()) {
                this.renderPassBatched(shader, commandList, renderLists, renderPass, occlusionCamera, camera, timestamp, useBlockFaceCulling);
            } else {
                Iterator<ChunkRenderList> iterator = renderLists.iterator(renderPass.isReverseOrder());

                while (iterator.hasNext()) {
                    this.renderRegion(shader, commandList, iterator.next(), renderPass, occlusionCamera, camera, timestamp, useBlockFaceCulling);
                }
            }

            this.currentVertexFormat = null;
            this.currentRenderPass = null;

            GLDebug.popGroup();
        }

        this.end(renderPass);
    }

    protected void renderRegion(ChunkShaderInterface shader, CommandList commandList, ChunkRenderList renderList, TerrainRenderPass renderPass, CameraTransform occlusionCamera, CameraTransform camera, long timestamp, boolean useBlockFaceCulling) {
        var region = renderList.getRegion();
        var storage = region.getStorage(renderPass);

        if (storage == null) {
            return;
        }

        var sink = this.emitter.getCommandSink();

        this.assembleRegion(sink, region, storage, renderList, occlusionCamera, renderPass,
                useBlockFaceCulling && !renderPass.isSorted());

        if (sink.isEmpty()) {
            return;
        }

        if (!renderPass.isSorted()) {
           getSharedIndexBuffer(renderPassConfiguration.getPrimitiveTypeForPass(renderPass), commandList).ensureCapacity(commandList, sink.getIndexBufferSize());
        }

        this.drawRegion(shader, commandList, region, camera, timestamp);
    }

    private void renderPassBatched(ChunkShaderInterface shader, CommandList commandList, ChunkRenderListIterable renderLists, TerrainRenderPass renderPass, CameraTransform occlusionCamera, CameraTransform camera, long timestamp, boolean useBlockFaceCulling) {
        boolean reverse = renderPass.isReverseOrder();

        this.emitter.beginPass(commandList, renderLists.getTotalSectionsWithGeometry());

        var sink = this.emitter.getCommandSink();
        int[] commandCounts = this.regionCommandCounts;
        int regionCount = 0;
        int indexBufferSize = 0;

        for (Iterator<ChunkRenderList> iterator = renderLists.iterator(reverse); iterator.hasNext(); ) {
            var renderList = iterator.next();
            var region = renderList.getRegion();
            var storage = region.getStorage(renderPass);

            int commands = 0;

            if (storage != null) {
                this.assembleRegion(sink, region, storage, renderList, occlusionCamera, renderPass,
                        useBlockFaceCulling && !renderPass.isSorted());

                commands = sink.size();
                indexBufferSize = Math.max(indexBufferSize, sink.getIndexBufferSize());
            }

            if (regionCount == commandCounts.length) {
                commandCounts = this.regionCommandCounts = Arrays.copyOf(commandCounts, regionCount * 2);
            }

            commandCounts[regionCount++] = commands;
        }

        if (indexBufferSize > 0 && !renderPass.isSorted()) {
            getSharedIndexBuffer(renderPassConfiguration.getPrimitiveTypeForPass(renderPass), commandList)
                    .ensureCapacity(commandList, indexBufferSize);
        }

        this.emitter.finishAssembly(commandList);

        int firstCommand = 0;
        int regionIndex = 0;

        for (Iterator<ChunkRenderList> iterator = renderLists.iterator(reverse); iterator.hasNext(); ) {
            var renderList = iterator.next();
            int commands = commandCounts[regionIndex++];

            if (commands == 0) {
                continue;
            }

            this.emitter.selectDrawRange(firstCommand, commands);
            firstCommand += commands;

            this.drawRegion(shader, commandList, renderList.getRegion(), camera, timestamp);
        }

        this.emitter.onPassFinished(commandList);
    }

    protected void drawRegion(ChunkShaderInterface shader, CommandList commandList, RenderRegion region, CameraTransform camera, long timestamp) {
        var tessellation = this.prepareTessellation(commandList, region);

        setModelMatrixUniforms(shader, region, camera);
        shader.setSectionAges(timestamp, region.getSectionLoadTimes(), region.getNewestSectionLoadTime());
        this.executeBatch(commandList, tessellation, shader.getPrimitiveType());
    }

    /**
     * Produces the region's draw commands. Overridable so that a renderer whose commands are built elsewhere (on the
     * GPU, say) can substitute its own source without reimplementing the surrounding draw setup.
     */
    protected void assembleRegion(DrawCommandSink sink, RenderRegion region, SectionRenderDataStorage storage,
                                  ChunkRenderList renderList, CameraTransform occlusionCamera,
                                  TerrainRenderPass renderPass, boolean useBlockFaceCulling) {
        BatchAssembler.fillRegion(sink, region, storage, renderList, occlusionCamera, renderPass, useBlockFaceCulling);
    }

    protected final MultiDrawEmitter getEmitter() {
        return this.emitter;
    }

    protected void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        this.emitter.executeBatch(commandList, tessellation, primitiveType);
    }

    private static void setModelMatrixUniforms(ChunkShaderInterface shader, RenderRegion region, CameraTransform camera) {
        float x = getCameraTranslation(region.getOriginX(), camera.intX, camera.fracX);
        float y = getCameraTranslation(region.getOriginY(), camera.intY, camera.fracY);
        float z = getCameraTranslation(region.getOriginZ(), camera.intZ, camera.fracZ);

        shader.setRegionOffset(x, y, z);
    }

    private static float getCameraTranslation(int chunkBlockPos, int cameraBlockPos, float cameraPos) {
        return (chunkBlockPos - cameraBlockPos) - cameraPos;
    }

    private GlTessellation prepareTessellation(CommandList commandList, RenderRegion region) {
        var resources = region.getResources(this.currentVertexFormat);
        var tessellation = this.currentRenderPass.isSorted() ? resources.getIndexedTessellation() : resources.getTessellation();

        if (tessellation == null) {
            tessellation = this.createRegionTessellation(commandList, resources);
            if (this.currentRenderPass.isSorted()) {
                resources.updateIndexedTessellation(commandList, tessellation);
            } else {
                resources.updateTessellation(commandList, tessellation);
            }
        }

        return tessellation;
    }

    protected TessellationBinding[] makeTessellationBindingArray(CommandList commandList, RenderRegion.DeviceResources resources) {
        return new TessellationBinding[] {
                TessellationBinding.forVertexBuffer(resources.getVertexBuffer(), this.currentVertexFormat),
                TessellationBinding.forElementBuffer(this.currentRenderPass.isSorted() ? resources.getIndexBuffer() : this.getSharedIndexBuffer(this.renderPassConfiguration.getPrimitiveTypeForPass(this.currentRenderPass), commandList).getBufferObject())
        };
    }

    protected GlTessellation createRegionTessellation(CommandList commandList, RenderRegion.DeviceResources resources) {
        var bindings = makeTessellationBindingArray(commandList, resources);
        GlVertexArrayTessellation tessellation = new GlVertexArrayTessellation(new GlVertexArray(), bindings);
        tessellation.init(commandList);

        return tessellation;
    }

    @Override
    public void delete(CommandList commandList) {
        super.delete(commandList);

        this.sharedIndexBuffers.values().forEach(buffer -> buffer.delete(commandList));
        this.emitter.delete();
    }
}
