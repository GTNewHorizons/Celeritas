package org.embeddedt.embeddium.impl.gl.functions;

public record DeviceFunctions(BufferStorageFunctions bufferStorageFunctions,
                              MultidrawFunctions multidrawFunctions,
                              BufferCopyFunctions bufferCopyFunctions,
                              BufferMapRangeFunctions bufferMapRangeFunctions) {
    public DeviceFunctions() {
        this(
                BufferStorageFunctions.pickBest(),
                MultidrawFunctions.pickBest(),
                BufferCopyFunctions.pickBest(),
                BufferMapRangeFunctions.pickBest()
        );
    }
}
