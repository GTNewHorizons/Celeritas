package org.embeddedt.embeddium.impl.render.chunk.map;

public enum PendingState {
    /// Object (chunk or cube) will be added to the [RenderSectionManager].
    Add,
    /// Object (chunk or cube) will be removed from the [RenderSectionManager].
    Remove
}
