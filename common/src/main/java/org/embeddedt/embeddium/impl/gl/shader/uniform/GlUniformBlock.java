package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.embeddedt.embeddium.impl.gl.buffer.GlBuffer;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;
import org.lwjgl.opengl.GL32C;

public class GlUniformBlock {

    private final int binding;

    public GlUniformBlock(int uniformBlockBinding) {
        this.binding = uniformBlockBinding;
    }

    public void bindBuffer(GlBuffer buffer) {
        LWJGL.glBindBufferBase(GL32C.GL_UNIFORM_BUFFER, this.binding, buffer.handle());
    }
}
