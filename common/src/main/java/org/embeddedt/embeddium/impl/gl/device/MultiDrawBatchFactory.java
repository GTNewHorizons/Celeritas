package org.embeddedt.embeddium.impl.gl.device;

@FunctionalInterface
public interface MultiDrawBatchFactory {
    MultiDrawBatch create(int capacity);
}
