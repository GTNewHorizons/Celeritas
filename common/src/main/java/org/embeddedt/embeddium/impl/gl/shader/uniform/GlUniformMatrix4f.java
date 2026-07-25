package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.joml.Matrix4fc;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformMatrix4f extends GlUniform<Matrix4fc>  {

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(16);

    public GlUniformMatrix4f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix4fc value) {
        value.get(buf);
        LWJGL.glUniformMatrix4fv(this.index, false, buf);
    }
}
