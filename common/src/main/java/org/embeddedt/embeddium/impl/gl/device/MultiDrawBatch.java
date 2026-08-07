package org.embeddedt.embeddium.impl.gl.device;

import org.embeddedt.embeddium.impl.render.chunk.multidraw.DrawCommandSink;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.NULL;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.POINTER_SIZE;
import org.lwjgl.PointerBuffer;
import java.nio.IntBuffer;

/**
 * Provides a fixed-size queue for building a draw-command list usable with
 * {@link org.lwjgl.opengl.GL33C#glMultiDrawElementsBaseVertex(int, IntBuffer, int, PointerBuffer, IntBuffer)}.
 */
public final class MultiDrawBatch implements DrawCommandSink {
    private static final int POINTER_SHIFT = Integer.numberOfTrailingZeros(POINTER_SIZE);

    public final long pElementPointer;
    public final long pElementCount;
    public final long pBaseVertex;

    private final int capacity;

    public int size;

    private int maxElementCount;

    public MultiDrawBatch(int capacity) {
        this.pElementPointer = LWJGL.nmemAlignedAlloc(32, (long) capacity * POINTER_SIZE);
        if (this.pElementPointer == NULL) {
            throw new OutOfMemoryError("Failed to allocate element pointer array");
        }
        LWJGL.memSet(this.pElementPointer, 0x0, (long) capacity * POINTER_SIZE);

        this.pElementCount = LWJGL.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pElementCount == NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            throw new OutOfMemoryError("Failed to allocate element count array");
        }
        this.pBaseVertex = LWJGL.nmemAlignedAlloc(32, (long) capacity * Integer.BYTES);
        if (this.pBaseVertex == NULL) {
            LWJGL.nmemAlignedFree(this.pElementPointer);
            LWJGL.nmemAlignedFree(this.pElementCount);
            throw new OutOfMemoryError("Failed to allocate base vertex array");
        }

        this.capacity = capacity;
    }

    @Override
    public int size() {
        return this.size;
    }

    public int capacity() {
        return this.capacity;
    }

    @Override
    public void clear() {
        this.size = 0;
        this.maxElementCount = 0;
    }

    public void delete() {
        LWJGL.nmemAlignedFree(this.pElementPointer);
        LWJGL.nmemAlignedFree(this.pElementCount);
        LWJGL.nmemAlignedFree(this.pBaseVertex);
    }

    @Override
    public boolean isEmpty() {
        return this.size <= 0;
    }

    @Override
    public int getIndexBufferSize() {
        return this.maxElementCount;
    }

    @Override
    public void push(int baseVertex, int elementCount, long indexOffset) {
        int index = this.size;

        LWJGL.memPutInt(this.pBaseVertex + ((long) index << 2), baseVertex);
        LWJGL.memPutInt(this.pElementCount + ((long) index << 2), elementCount);
        LWJGL.memPutAddress(this.pElementPointer + ((long) index << POINTER_SHIFT), indexOffset);

        this.size = index + ((-elementCount) >>> 31);

        this.maxElementCount = Math.max(this.maxElementCount, elementCount);
    }
}
