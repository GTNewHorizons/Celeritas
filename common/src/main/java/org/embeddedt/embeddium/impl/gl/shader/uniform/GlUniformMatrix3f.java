package org.embeddedt.embeddium.impl.gl.shader.uniform;

import com.mitchej123.lwjgl.MemoryStack;
import org.joml.Matrix3f;

import java.nio.FloatBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformMatrix3f extends GlUniform<Matrix3f> {

    public GlUniformMatrix3f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix3f value) {
        try (MemoryStack stack = LWJGL.stackPush()) {
            FloatBuffer buf = stack.callocFloat(12);
            value.get(buf);

            LWJGL.glUniformMatrix3fv(this.index, false, buf);
        }
    }
}
