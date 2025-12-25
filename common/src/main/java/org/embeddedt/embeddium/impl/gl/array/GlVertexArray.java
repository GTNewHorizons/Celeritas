package org.embeddedt.embeddium.impl.gl.array;

import org.embeddedt.embeddium.impl.gl.GlObject;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public static final int NULL_ARRAY_ID = 0;

    public GlVertexArray() {
        this.setHandle(LWJGL.glGenVertexArrays());
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteVertexArrays(this.handle());
    }
}
