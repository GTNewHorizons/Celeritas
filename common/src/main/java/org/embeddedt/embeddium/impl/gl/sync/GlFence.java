package org.embeddedt.embeddium.impl.gl.sync;

import org.taumc.celeritas.lwjgl.GL32;

import static org.taumc.celeritas.lwjgl.LWJGLServiceProvider.LWJGL;

public class GlFence {

    private static final long WAIT_SLICE_NANOS = 10_000_000L;
    private static final long WAIT_DEFAULT_NANOS = 5_000_000_000L;

    private final long id;
    private boolean disposed;

    public GlFence(long id) {
        this.id = id;
    }

    private static boolean signaled(int status) {
        return status == GL32.GL_ALREADY_SIGNALED || status == GL32.GL_CONDITION_SATISFIED;
    }

    public boolean isCompleted() {
        this.checkDisposed();
        return signaled(LWJGL.glClientWaitSync(this.id, 0, 0L));
    }

    public boolean sync() {
        return this.sync(WAIT_DEFAULT_NANOS);
    }

    public boolean sync(long timeoutNanos) {
        this.checkDisposed();

        if (timeoutNanos <= 0) {
            return signaled(LWJGL.glClientWaitSync(this.id, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, 0L));
        }

        final long deadline = System.nanoTime() + timeoutNanos;
        long remaining = timeoutNanos;
        int status;

        do {
            status = LWJGL.glClientWaitSync(this.id, GL32.GL_SYNC_FLUSH_COMMANDS_BIT, Math.min(WAIT_SLICE_NANOS, remaining));
            remaining = deadline - System.nanoTime();
        } while (status == GL32.GL_TIMEOUT_EXPIRED && remaining > 0);

        return signaled(status);
    }

    public void delete() {
        LWJGL.glDeleteSync(this.id);
        this.disposed = true;
    }

    private void checkDisposed() {
        if (this.disposed) {
            throw new IllegalStateException("Fence object has been disposed");
        }
    }
}
