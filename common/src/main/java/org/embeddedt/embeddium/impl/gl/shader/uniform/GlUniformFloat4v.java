package org.embeddedt.embeddium.impl.gl.shader.uniform;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlUniformFloat4v extends GlUniform<float[]> {

    public GlUniformFloat4v(int index) {
        super(index);
    }

    @Override
    public void set(float[] value) {
        if (value.length != 4) {
            throw new IllegalArgumentException("value.length != 4");
        }

        LWJGL.glUniform4fv(this.index, value);
    }
}
