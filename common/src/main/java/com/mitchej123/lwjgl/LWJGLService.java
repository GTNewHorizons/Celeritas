package com.mitchej123.lwjgl;

import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * LWJGL2/LWJGL3 abstraction.
 * - GL constants are inlined and identical across versions - use directly.
 */
public abstract class LWJGLService {

    /** Higher priority services are preferred. LWJGL3 = 100, LWJGL2 = 0. */
    public int getPriority() { return 0; }

    // ===================== CAPABILITIES =====================

    public abstract boolean isOpenGLVersionSupported(int major, int minor);
    public abstract boolean isExtensionSupported(GLExtension extension);
    public abstract int getPointerSize();

    // ===================== BUFFER OPERATIONS =====================

    public abstract int glGenBuffers();
    public abstract void glDeleteBuffers(int buffer);
    public abstract void glBindBuffer(int target, int buffer);
    public abstract void glBufferData(int target, long size, int usage);
    public abstract void glBufferData(int target, ByteBuffer data, int usage);
    public abstract void glBufferData(int target, long size, long data, int usage);
    public abstract void glBufferStorage(int target, long size, int flags);
    public abstract ByteBuffer glMapBufferRange(int target, long offset, long length, int flags);
    public abstract long nglMapBuffer(int target, int access);
    public abstract ByteBuffer glMapBuffer(int target, int access);
    public abstract void glUnmapBuffer(int target);
    public abstract void glFlushMappedBufferRange(int target, long offset, long length);
    public abstract void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size);
    public abstract void glBindBufferBase(int target, int index, int buffer);

    // ===================== VAO OPERATIONS =====================

    public abstract int glGenVertexArrays();
    public abstract void glDeleteVertexArrays(int array);
    public abstract void glBindVertexArray(int array);
    public abstract void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer);
    public abstract void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer);
    public abstract void glEnableVertexAttribArray(int index);

    // ===================== SHADER OPERATIONS =====================

    public abstract int glCreateShader(int type);
    public abstract void glShaderSource(int shader, CharSequence source);

    /**
     * Identical in function to {@link #glShaderSource(int, CharSequence)} but
     * passes a null pointer for string length to force the driver to rely on the null
     * terminator for string length. This is a workaround for an apparent flaw with some
     * AMD drivers that don't receive or interpret the length correctly, resulting in
     * an access violation when the driver tries to read past the string memory.
     *
     * <p>Hat tip to fewizz for the find and the fix.
     *
     * @see <a href="https://github.com/grondag/canvas/commit/820bf754092ccaf8d0c169620c2ff575722d7d96">Original Canvas commit</a>
     */
    public abstract void glShaderSourceSafe(int shader, CharSequence source);
    public abstract void glCompileShader(int shader);
    public abstract String glGetShaderInfoLog(int shader, int maxLength);
    public abstract int glGetShaderi(int shader, int pname);
    public abstract void glDeleteShader(int shader);

    public abstract int glCreateProgram();
    public abstract void glAttachShader(int program, int shader);
    public abstract void glLinkProgram(int program);
    public abstract String glGetProgramInfoLog(int program, int maxLength);
    public abstract int glGetProgrami(int program, int pname);
    public abstract void glUseProgram(int program);
    public abstract void glDeleteProgram(int program);
    public abstract void glBindAttribLocation(int program, int index, CharSequence name);
    public abstract void glBindFragDataLocation(int program, int colorNumber, CharSequence name);

    // ===================== UNIFORM OPERATIONS =====================

    public abstract int glGetUniformLocation(int program, CharSequence name);
    public abstract int glGetUniformBlockIndex(int program, CharSequence name);
    public abstract void glUniformBlockBinding(int program, int blockIndex, int blockBinding);
    public abstract void glUniform1f(int location, float v0);
    public abstract void glUniform1i(int location, int v0);
    public abstract void glUniform1fv(int location, FloatBuffer value);
    public abstract void glUniform2i(int location, int v0, int v1);
    public abstract void glUniform3f(int location, float v0, float v1, float v2);
    public abstract void glUniform3fv(int location, FloatBuffer value);
    public abstract void glUniform3fv(int location, float[] value);
    public abstract void glUniform4fv(int location, FloatBuffer value);
    public abstract void glUniform4fv(int location, float[] value);
    public abstract void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value);
    public abstract void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value);

    // ===================== DRAW OPERATIONS =====================

    public abstract void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex);
    public abstract void glMultiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int drawcount, long pBaseVertex);
    public abstract void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride);

    // ===================== SYNC OPERATIONS =====================

    public abstract long glFenceSync(int condition, int flags);
    public abstract int glClientWaitSync(long sync, int flags, long timeout);
    public abstract int glGetSynci(long sync, int pname, IntBuffer length);
    public abstract void glWaitSync(long sync, int flags, long timeout);
    public abstract void glDeleteSync(long sync);

    // ===================== QUERY OPERATIONS =====================

    public abstract int glGenQueries();
    public abstract void glDeleteQueries(int query);
    public abstract void glQueryCounter(int id, int target);
    public abstract long glGetQueryObjectui64(int id, int pname);

    // ===================== DEBUG OPERATIONS =====================

    public PrintStream getDebugStream() { return System.err; }
    public abstract int setupDebugCallback(DebugMessageHandler handler); // returns 0=unsupported, 1=success, 2=restart needed
    public abstract void disableDebugCallback();
    public abstract void glObjectLabel(int identifier, int name, CharSequence label);
    public abstract void glPushDebugGroup(int source, int id, CharSequence message);
    public abstract void glPopDebugGroup();

    // ===================== TEXTURE OPERATIONS =====================

    public abstract int glGenTextures();
    public abstract void glGenTextures(int[] textures);
    public abstract void glDeleteTextures(int texture);
    public abstract void glDeleteTextures(int[] textures);
    public abstract void glBindTexture(int target, int texture);
    public abstract void glActiveTexture(int texture);
    public abstract int glGetTexLevelParameteri(int target, int level, int pname);
    public abstract void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);
    public abstract void glPixelStorei(int pname, int param);

    // ===================== FRAMEBUFFER OPERATIONS =====================

    public abstract int glGenFramebuffers();
    public abstract void glDeleteFramebuffers(int framebuffer);
    public abstract void glBindFramebuffer(int target, int framebuffer);
    public abstract int glCheckFramebufferStatus(int target);
    public abstract void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level);

    // ===================== STATE OPERATIONS =====================

    public abstract void glEnable(int cap);
    public abstract void glDisable(int cap);
    public abstract void glBlendFunc(int sfactor, int dfactor);
    public abstract void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);
    public abstract void glDepthFunc(int func);
    public abstract void glDepthMask(boolean flag);
    public abstract void glColorMask(boolean red, boolean green, boolean blue, boolean alpha);
    public abstract void glViewport(int x, int y, int width, int height);
    public abstract void glClear(int mask);
    public abstract void glClearColor(float red, float green, float blue, float alpha);
    public abstract int glGetError();

    // ===================== COMPATIBILITY PROFILE =====================

    public abstract void glMatrixMode(int mode);
    public abstract void glLoadMatrixf(FloatBuffer m);

    // ===================== MISC GL =====================

    public abstract int glGetInteger(int pname);
    public abstract void glGetIntegerv(int pname, int[] params);
    public abstract boolean glGetBoolean(int pname);
    public abstract String glGetString(int pname);
    public abstract int glGetAttribLocation(int program, CharSequence name);

    // ===================== MEMORY STACK =====================

    public abstract MemoryStack stackPush();

    // ===================== NATIVE MEMORY =====================

    public abstract long nmemAlloc(long size);
    public abstract long nmemCalloc(long count, long size);
    public abstract long nmemAlignedAlloc(long alignment, long size);
    public abstract long nmemRealloc(long ptr, long size);
    public abstract void nmemFree(long ptr);
    public abstract void nmemAlignedFree(long ptr);

    public abstract ByteBuffer memAlloc(int size);
    public abstract ByteBuffer memCalloc(int size);
    public abstract ByteBuffer memRealloc(ByteBuffer buffer, int size);
    public abstract void memFree(Buffer buffer);
    public abstract ByteBuffer memByteBuffer(long address, int capacity);
    public abstract long memAddress(Buffer buffer);
    public abstract long memAddress(Buffer buffer, int position);

    public abstract void memSet(long address, int value, long bytes);
    public abstract void memCopy(long src, long dst, long bytes);

    public abstract void memPutByte(long address, byte value);
    public abstract void memPutShort(long address, short value);
    public abstract void memPutInt(long address, int value);
    public abstract void memPutFloat(long address, float value);
    public abstract void memPutLong(long address, long value);
    public abstract void memPutAddress(long address, long value);

    public abstract byte memGetByte(long address);
    public abstract short memGetShort(long address);
    public abstract int memGetInt(long address);
    public abstract float memGetFloat(long address);
    public abstract long memGetLong(long address);
    public abstract long memGetAddress(long address);
    public abstract ByteBuffer memSlice(ByteBuffer buffer, int offset, int capacity);
}
