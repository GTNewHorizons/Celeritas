package org.embeddedt.embeddium.impl.render.chunk.data;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Holds the buffers of discarded cached batches until a rebuilt batch takes them. Kept free of GL so it can
 * be tested without a context.
 */
final class BatchBufferRecycler<T> {
    // A storage caches at most two batches, so there are never more than two buffers to hold.
    static final int CAPACITY = 2;

    private final Object[] spares = new Object[CAPACITY];
    private int count;
    private boolean enabled;

    boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Disabling hands everything held to {@code deleter}.
     */
    void setEnabled(boolean enabled, Consumer<? super T> deleter) {
        if (this.enabled == enabled) {
            return;
        }

        this.enabled = enabled;

        if (!enabled) {
            this.drain(deleter);
        }
    }

    /**
     * Holds {@code buffer} for reuse, or deletes it right away when disabled or full.
     */
    void retire(@Nullable T buffer, Consumer<? super T> deleter) {
        if (buffer == null) {
            return;
        }

        if (!this.enabled || this.count == CAPACITY) {
            deleter.accept(buffer);
            return;
        }

        this.spares[this.count++] = buffer;
    }

    @SuppressWarnings("unchecked")
    @Nullable T take() {
        if (!this.enabled || this.count == 0) {
            return null;
        }

        int index = --this.count;
        T buffer = (T) this.spares[index];
        this.spares[index] = null;
        return buffer;
    }

    @SuppressWarnings("unchecked")
    void drain(Consumer<? super T> deleter) {
        while (this.count > 0) {
            int index = --this.count;
            T buffer = (T) this.spares[index];
            this.spares[index] = null;
            deleter.accept(buffer);
        }
    }

    int size() {
        return this.count;
    }
}
