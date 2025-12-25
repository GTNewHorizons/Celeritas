package org.embeddedt.embeddium.impl.gl.shader.uniform;

import com.mitchej123.lwjgl.MemoryStack;
import org.joml.Matrix4fc;

import java.nio.FloatBuffer;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformMatrix4f extends GlUniform<Matrix4fc>  {

    public GlUniformMatrix4f(int index) {
        super(index);
    }

    @Override
    public void set(Matrix4fc value) {
        try (MemoryStack stack = LWJGL.stackPush()) {
            FloatBuffer buf = stack.callocFloat(16);
            value.get(buf);

            LWJGL.glUniformMatrix4fv(this.index, false, buf);
        }
    }
}
