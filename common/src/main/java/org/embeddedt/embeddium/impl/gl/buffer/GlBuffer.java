package org.embeddedt.embeddium.impl.gl.buffer;

import org.embeddedt.embeddium.impl.gl.GlObject;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public abstract class GlBuffer extends GlObject {

    private GlBufferMapping activeMapping;

    protected GlBuffer() {
        this.setHandle(LWJGL.glGenBuffers());
    }

    public GlBufferMapping getActiveMapping() {
        return this.activeMapping;
    }

    public void setActiveMapping(GlBufferMapping mapping) {
        this.activeMapping = mapping;
    }

    @Override
    protected void destroyInternal() {
        LWJGL.glDeleteBuffers(this.handle());
    }
}
