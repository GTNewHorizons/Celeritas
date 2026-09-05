package org.embeddedt.embeddium.impl.render.vertex.buffer;

import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;

/**
 * Optional format-specific work performed by the in-place BufferBuilder fast path.
 *
 * This is deliberately an extension point rather than a dependency on any shader
 * implementation. Iris uses it for its extended vertex formats on 1.20.1 and 1.21.
 */
public interface FastVertexExtension {
    boolean canUseFastFormat(VertexFormatDescription format);

    /**
     * Completes format-specific data for the vertex at {@code pointer}.
     * The returned mask contains any additional common attributes written by the extension.
     */
    int finishFastVertex(long pointer, int stride, int writtenAttributes);

    default void resetFastVertexState() {
    }
}
