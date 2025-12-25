package org.embeddedt.embeddium.impl.gl.tessellation;

import org.embeddedt.embeddium.impl.gl.attribute.GlVertexAttributeBinding;
import org.embeddedt.embeddium.impl.gl.device.CommandList;
import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;

public abstract class GlAbstractTessellation implements GlTessellation {

    protected final TessellationBinding[] bindings;

    protected GlAbstractTessellation(TessellationBinding[] bindings) {
        this.bindings = bindings;
    }

    protected void bindAttributes(CommandList commandList) {
        for (TessellationBinding binding : this.bindings) {
            commandList.bindBuffer(binding.target(), binding.buffer());

            for (GlVertexAttributeBinding attrib : binding.attributeBindings()) {
                if (attrib.isIntType()) {
                    LWJGL.glVertexAttribIPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(),
                            attrib.getStride(), attrib.getPointer());
                } else {
                    LWJGL.glVertexAttribPointer(attrib.getIndex(), attrib.getCount(), attrib.getFormat().typeId(), attrib.isNormalized(),
                            attrib.getStride(), attrib.getPointer());
                }
                LWJGL.glEnableVertexAttribArray(attrib.getIndex());
            }
        }
    }
}
