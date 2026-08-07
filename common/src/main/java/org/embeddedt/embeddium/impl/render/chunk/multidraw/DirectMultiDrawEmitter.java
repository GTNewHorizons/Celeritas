package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.device.DrawCommandList;
import org.embeddedt.embeddium.impl.gl.device.MultiDrawBatch;
import org.embeddedt.embeddium.impl.gl.tessellation.GlIndexType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;

public record DirectMultiDrawEmitter(MultiDrawBatch batch) implements MultiDrawEmitter {
    public DirectMultiDrawEmitter() {
        this(new MultiDrawBatch(MAX_COMMAND_COUNT));
    }

    @Override
    public DrawCommandSink getCommandSink() {
        return this.batch;
    }

    @Override
    public void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType) {
        try (DrawCommandList drawCommandList = commandList.beginTessellating(tessellation)) {
            drawCommandList.multiDrawElementsBaseVertex(this.batch, primitiveType, GlIndexType.UNSIGNED_INT);
        }
    }

    @Override
    public void delete() {
        this.batch.delete();
    }
}
