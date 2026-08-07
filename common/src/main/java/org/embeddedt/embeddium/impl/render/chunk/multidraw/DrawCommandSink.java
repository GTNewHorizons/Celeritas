package org.embeddedt.embeddium.impl.render.chunk.multidraw;

/**
 * Receives draw commands from {@link BatchAssembler} in whatever layout the consuming emitter draws from, so that
 * assembly writes the final records once rather than building one layout and transcoding it into another.
 * <p>
 * Implementations must be {@code final} and few: the assembler's fill loops are shared across all of them, so the
 * dispatch is only cheap while the call site stays inlineable.
 */
public interface DrawCommandSink {
    /**
     * Ends the current region's batch; subsequent pushes belong to the next one. Whether the ended batch is discarded
     * or retained for a later draw is up to the implementation, so {@link #size()} and {@link #getIndexBufferSize()}
     * afterwards describe only the new batch.
     */
    void clear();

    /**
     * {@return the number of commands in the current batch}
     */
    int size();

    default boolean isEmpty() {
        return this.size() <= 0;
    }

    /**
     * {@return the largest element count of any command in the current batch}
     * <p>
     * Sizes the shared quad index buffer, which every command with a zero index offset draws through.
     */
    int getIndexBufferSize();

    /**
     * Appends one command. Commands with a zero element count are discarded rather than stored.
     *
     * @param indexOffset byte offset of the command's first index, or zero to draw from the shared index buffer
     */
    void push(int baseVertex, int elementCount, long indexOffset);
}
