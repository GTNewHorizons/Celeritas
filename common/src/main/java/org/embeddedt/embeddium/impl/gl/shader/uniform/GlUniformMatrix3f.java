package org.embeddedt.embeddium.impl.gl.shader.uniform;

import org.joml.Matrix3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformMatrix3f extends GlUniform<Matrix3f> {

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(12);

    public GlUniformMatrix3f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix3f value) {
        value.get(buf);
        LWJGL.glUniformMatrix3fv(this.index, false, buf);
    }
}
