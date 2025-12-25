package org.embeddedt.embeddium.impl.gl.shader.uniform;

import com.mitchej123.lwjgl.MemoryStack;

import java.nio.FloatBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformFloatArray extends GlUniform<float[]> {

    public GlUniformFloatArray(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        try (MemoryStack stack = LWJGL.stackPush()) {
            FloatBuffer buf = stack.callocFloat(value.length);
            buf.put(value);

            LWJGL.glUniform1fv(this.index, buf);
        }
    }

    public void set(FloatBuffer value) {
        LWJGL.glUniform1fv(this.index, value);
    }
}
