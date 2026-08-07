package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

import com.mitchej123.lwjgl.LWJGLServiceProvider;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferTarget;
import org.embeddedt.embeddium.impl.gl.buffer.GlBufferUsage;
import org.embeddedt.embeddium.impl.gl.buffer.GlMutableBuffer;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

/**
 * A multidraw emitter that uses indirect rendering to exploit hardware acceleration, which
 * reduces CPU overhead on some platforms.
 * @author Ven
 */
public class IndirectMultiDrawEmitter implements MultiDrawEmitter {
    // uint  count;
    // uint  instanceCount;
    // uint  firstIndex;
    // int   baseVertex;
    // uint  baseInstance;
    static final int COMMAND_SIZE = 4 * 5;

    private static final long OFFSET_COUNT = 0;
    private static final long OFFSET_INSTANCE_COUNT = 4;
    private static final long OFFSET_FIRST_INDEX = 8;
    private static final long OFFSET_BASE_VERTEX = 12;
    private static final long OFFSET_BASE_INSTANCE = 16;

    private long indirectBuffer;
    private int commandCapacity;

    private final GlMutableBuffer indirectBufferGpu;
    private long gpuCapacityBytes;

    private final Sink sink = new Sink();

    private int drawStart;
    private int drawCount;

    private static int arenaGrowths;

    public IndirectMultiDrawEmitter() {
        this.indirectBufferGpu = new GlMutableBuffer();
        this.allocateArena(MultiDrawEmitter.MAX_COMMAND_COUNT);
    }

    /**
     * {@return the number of times an indirect arena has been grown since the last call}
     */
    public static int takeArenaGrowths() {
        int growths = arenaGrowths;
        arenaGrowths = 0;
        return growths;
    }

    private void allocateArena(int commands) {
        long buffer = LWJGL.nmemAlignedAlloc(32, (long) commands * COMMAND_SIZE);
        if (buffer == LWJGLServiceProvider.NULL) {
            throw new OutOfMemoryError("Failed to allocate indirect buffer");
        }

        if (this.indirectBuffer != LWJGLServiceProvider.NULL) {
            LWJGL.nmemAlignedFree(this.indirectBuffer);
        }

        this.indirectBuffer = buffer;
        this.commandCapacity = commands;
        this.sink.pCommands = buffer;

        this.prefillConstants();
    }

    /**
     * Every command draws a single non-instanced primitive batch, so two of the five fields are the same for the life
     * of the buffer. Assembly only writes the other three.
     */
    private void prefillConstants() {
        long ptr = this.indirectBuffer;
        for (int i = 0; i < this.commandCapacity; i++) {
            LWJGL.memPutInt(ptr + OFFSET_INSTANCE_COUNT, 1);
            LWJGL.memPutInt(ptr + OFFSET_BASE_INSTANCE, 0);
            ptr += COMMAND_SIZE;
        }
    }

    private static final class Sink implements DrawCommandSink {
        private long pCommands;

        private int regionStart;
        private int size;
        private int maxElementCount;

        void reset() {
            this.regionStart = 0;
            this.size = 0;
            this.maxElementCount = 0;
        }

        int passSize() {
            return this.regionStart + this.size;
        }

        /**
         * Commits the region assembled so far and opens the next one at the following command. A region that is never
         * assembled contributes nothing, so the ranges stay contiguous in assembly order.
         */
        @Override
        public void clear() {
            this.regionStart += this.size;
            this.size = 0;
            this.maxElementCount = 0;
        }

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public int getIndexBufferSize() {
            return this.maxElementCount;
        }

        @Override
        public void push(int baseVertex, int elementCount, long indexOffset) {
            long ptr = this.pCommands + ((long) (this.regionStart + this.size) * COMMAND_SIZE);

            LWJGL.memPutInt(ptr + OFFSET_COUNT, elementCount);
            LWJGL.memPutInt(ptr + OFFSET_FIRST_INDEX, (int) (indexOffset >>> 2));
            LWJGL.memPutInt(ptr + OFFSET_BASE_VERTEX, baseVertex);

            // Element counts are never negative, so the sign bit of the negation is set iff the command is non-empty.
            this.size += ((-elementCount) >>> 31);

            this.maxElementCount = Math.max(this.maxElementCount, elementCount);
        }
    }

    @Override
    public DrawCommandSink getCommandSink() {
        return this.sink;
    }

    @Override
    public boolean batchesWholePass() {
        return true;
    }

    @Override
    public void beginPass(CommandList commandList, int sectionCount) {
        int needed = sectionCount * ModelQuadFacing.COUNT;

        if (needed > this.commandCapacity) {
            this.allocateArena(Math.max(needed, this.commandCapacity * 2));
            arenaGrowths++;
        }

        long gpuBytes = (long) this.commandCapacity * COMMAND_SIZE;

        if (gpuBytes != this.gpuCapacityBytes) {
            commandList.uploadData(this.indirectBufferGpu, LWJGLServiceProvider.NULL, gpuBytes,
                    GlBufferUsage.STREAM_DRAW, GlBufferTarget.DRAW_INDIRECT_BUFFER);
            this.gpuCapacityBytes = gpuBytes;
        }

        this.sink.reset();
        this.drawStart = 0;
        this.drawCount = 0;
    }

    @Override
    public void finishAssembly(CommandList commandList) {
        int commands = this.sink.passSize();

        if (commands == 0) {
            return;
        }

        commandList.uploadSubData(this.indirectBufferGpu, 0L, this.indirectBuffer, (long) commands * COMMAND_SIZE,
                GlBufferTarget.DRAW_INDIRECT_BUFFER);
    }

    @Override
    public void selectDrawRange(int firstCommand, int commandCount) {
        this.drawStart = firstCommand;
        this.drawCount = commandCount;
    }

    @Override
    public int getPendingCommandCount() {
        return this.drawCount;
    }

    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        if (this.drawCount == 0) {
            return;
        }

        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, this.indirectBufferGpu);
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsIndirect(this.indirectBufferGpu, (long) this.drawStart * COMMAND_SIZE,
                    this.drawCount, primitiveType, GlIndexType.UNSIGNED_INT);
        }
    }

    @Override
    public void onPassFinished(CommandList commandList) {
        commandList.bindBuffer(GlBufferTarget.DRAW_INDIRECT_BUFFER, null);
    }

    @Override
    public void delete() {
        LWJGL.nmemAlignedFree(this.indirectBuffer);
        this.indirectBuffer = LWJGLServiceProvider.NULL;
        this.indirectBufferGpu.delete();
    }
}
