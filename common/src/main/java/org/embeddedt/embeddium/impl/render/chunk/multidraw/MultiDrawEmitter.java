package org.embeddedt.embeddium.impl.render.chunk.multidraw;

import org.embeddedt.embeddium.impl.gl.device.CommandList;
import org.embeddedt.embeddium.impl.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.impl.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.render.chunk.region.RenderRegion;

public interface MultiDrawEmitter {
    int MAX_COMMAND_COUNT = (ModelQuadFacing.COUNT * RenderRegion.REGION_SIZE) + 1;

    /**
     * {@return the sink that assembly writes this emitter's commands into, in their final layout}
     * <p>
     * The same instance every call, cleared at the start of each region's assembly.
     */
    DrawCommandSink getCommandSink();

    /**
     * {@return whether the whole pass must be assembled before any of it is drawn}
     * <p>
     * True for emitters whose commands live in a GPU buffer, so the pass uploads once instead of once per region.
     * The renderer then separates assembly from drawing around a single {@link #finishAssembly}, and drives
     * {@link #selectDrawRange} rather than reading the sink at draw time.
     */
    default boolean batchesWholePass() {
        return false;
    }

    /**
     * Begins a pass that will assemble at most {@code sectionCount} sections' worth of commands. Any growth the
     * emitter needs happens here, before the first command of the pass is written.
     */
    default void beginPass(CommandList commandList, int sectionCount) {
    }

    /**
     * Uploads everything assembled since {@link #beginPass}. Called once, after the last region has been assembled and
     * before the first has been drawn.
     */
    default void finishAssembly(CommandList commandList) {
    }

    /**
     * Selects the commands that the next {@link #executeBatch} draws, as an index and length into the pass.
     */
    default void selectDrawRange(int firstCommand, int commandCount) {
    }

    /**
     * {@return the number of commands the next {@link #executeBatch} will issue}
     */
    default int getPendingCommandCount() {
        return this.getCommandSink().size();
    }

    void executeBatch(CommandList commandList, GlTessellation tessellation, GlPrimitiveType primitiveType);

    /**
     * Releases whatever binding {@link #finishAssembly} left in place for the pass.
     */
    default void onPassFinished(CommandList commandList) {
    }

    default void delete() {
    }
}
