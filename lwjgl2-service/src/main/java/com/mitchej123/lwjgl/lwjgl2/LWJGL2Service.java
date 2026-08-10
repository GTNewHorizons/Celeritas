package com.mitchej123.lwjgl.lwjgl2;

import com.gtnewhorizon.gtnhlib.bytebuf.MemoryUtilities;
import com.gtnewhorizon.gtnhlib.bytebuf.Pointer;
import com.mitchej123.lwjgl.DebugMessageHandler;
import com.mitchej123.lwjgl.GLExtension;
import com.mitchej123.lwjgl.LWJGLService;
import com.mitchej123.lwjgl.MemoryStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.APPLEVertexArrayObject;
import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBCopyBuffer;
import org.lwjgl.opengl.ARBDrawElementsBaseVertex;
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.ARBMapBufferRange;
import org.lwjgl.opengl.ARBMultiDrawIndirect;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.ARBUniformBufferObject;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTGpuShader4;
import org.lwjgl.opengl.EXTTimerQuery;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;
import org.lwjgl.opengl.KHRDebug;

import java.io.PrintStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

/**
 * LWJGL2 implementation of {@link LWJGLService}.
 */
public final class LWJGL2Service extends LWJGLService {
    private static final Logger LOGGER = LogManager.getLogger("Celeritas/LWJGL2Service");
    private final LWJGL2DebugSupport debugSupport = new LWJGL2DebugSupport();

    // Map pointer -> GLSync for sync operations
    private final Long2ObjectOpenHashMap<GLSync> syncObjects = new Long2ObjectOpenHashMap<>();

    private enum VAOMode { CORE, ARB, APPLE, NONE }
    private enum TimerQueryMode { CORE, ARB, EXT, NONE }
    private enum DebugMode { KHR, NONE }
    private enum VertexAttribIMode { CORE, EXT, NONE }

    private final VAOMode vaoMode;
    private final TimerQueryMode timerQueryMode;
    private final DebugMode debugMode;
    private final VertexAttribIMode vertexAttribIMode;

    public LWJGL2Service() {
        ContextCapabilities caps = GLContext.getCapabilities();

        if (caps.OpenGL30) {
            vaoMode = VAOMode.CORE;
        } else if (caps.GL_ARB_vertex_array_object) {
            vaoMode = VAOMode.ARB;
        } else if (caps.GL_APPLE_vertex_array_object) {
            vaoMode = VAOMode.APPLE;
        } else {
            vaoMode = VAOMode.NONE;
        }

        if (caps.OpenGL33) {
            timerQueryMode = TimerQueryMode.CORE;
        } else if (caps.GL_ARB_timer_query) {
            timerQueryMode = TimerQueryMode.ARB;
        } else if (caps.GL_EXT_timer_query) {
            timerQueryMode = TimerQueryMode.EXT;
        } else {
            timerQueryMode = TimerQueryMode.NONE;
            LOGGER.warn("Timer query support not available - GPU profiling will be disabled");
        }

        if (caps.GL_KHR_debug || caps.OpenGL43) {
            debugMode = DebugMode.KHR;
        } else {
            debugMode = DebugMode.NONE;
        }

        if (caps.OpenGL30) {
            vertexAttribIMode = VertexAttribIMode.CORE;
        } else if (caps.GL_EXT_gpu_shader4) {
            vertexAttribIMode = VertexAttribIMode.EXT;
        } else {
            vertexAttribIMode = VertexAttribIMode.NONE;
        }
    }

    @Override
    public int getPriority() { return 0; }

    // ===================== CAPABILITIES =====================

    @Override
    public boolean isOpenGLVersionSupported(int major, int minor) {
        ContextCapabilities caps = GLContext.getCapabilities();
        switch (major * 10 + minor) {
            case 11: return caps.OpenGL11;
            case 12: return caps.OpenGL12;
            case 13: return caps.OpenGL13;
            case 14: return caps.OpenGL14;
            case 15: return caps.OpenGL15;
            case 20: return caps.OpenGL20;
            case 21: return caps.OpenGL21;
            case 30: return caps.OpenGL30;
            case 31: return caps.OpenGL31;
            case 32: return caps.OpenGL32;
            case 33: return caps.OpenGL33;
            case 40: return caps.OpenGL40;
            case 41: return caps.OpenGL41;
            case 42: return caps.OpenGL42;
            case 43: return caps.OpenGL43;
            case 44: return caps.OpenGL44;
            case 45: return caps.OpenGL45;
            default: return false;
        }
    }

    @Override
    public boolean isExtensionSupported(GLExtension extension) {
        ContextCapabilities caps = GLContext.getCapabilities();
        switch (extension) {
            case ARB_buffer_storage: return caps.GL_ARB_buffer_storage;
            case ARB_multi_draw_indirect: return caps.GL_ARB_multi_draw_indirect;
            case ARB_draw_elements_base_vertex: return caps.GL_ARB_draw_elements_base_vertex;
            case ARB_direct_state_access: return caps.GL_ARB_direct_state_access;
            case ARB_shader_storage_buffer_object: return caps.GL_ARB_shader_storage_buffer_object;
            case ARB_sync: return caps.GL_ARB_sync;
            case ARB_timer_query: return caps.GL_ARB_timer_query;
            case ARB_debug_output: return caps.GL_ARB_debug_output;
            case KHR_debug: return caps.GL_KHR_debug;
            case AMD_debug_output: return caps.GL_AMD_debug_output;
            case ARB_uniform_buffer_object: return caps.GL_ARB_uniform_buffer_object;
            case ARB_vertex_array_object: return caps.GL_ARB_vertex_array_object;
            case ARB_map_buffer_range: return caps.GL_ARB_map_buffer_range;
            case ARB_copy_buffer: return caps.GL_ARB_copy_buffer;
            case ARB_texture_storage: return caps.GL_ARB_texture_storage;
            case ARB_base_instance: return caps.GL_ARB_base_instance;
            case ARB_instanced_arrays: return caps.GL_ARB_instanced_arrays;
            case ARB_compatibility: return caps.GL_ARB_compatibility;
            default: return false;
        }
    }

    @Override
    public int getPointerSize() {
        return Pointer.POINTER_SIZE;
    }

    // ===================== BUFFER OPERATIONS =====================

    @Override
    public int glGenBuffers() {
        return GL15.glGenBuffers();
    }

    @Override
    public void glDeleteBuffers(int buffer) {
        GL15.glDeleteBuffers(buffer);
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }

    @Override
    public void glBufferData(int target, long size, int usage) {
        GL15.glBufferData(target, size, usage);
    }

    @Override
    public void glBufferData(int target, ByteBuffer data, int usage) {
        GL15.glBufferData(target, data, usage);
    }

    @Override
    public void glBufferData(int target, long size, long data, int usage) {
        // LWJGL2 nglBufferData has different signature - wrap the pointer
        if (data == 0) {
            GL15.glBufferData(target, size, usage);
        } else {
            ByteBuffer buf = MemoryUtilities.memByteBuffer(data, (int) size);
            GL15.glBufferData(target, buf, usage);
        }
    }

    @Override
    public void glBufferSubData(int target, long offset, ByteBuffer data) {
        GL15.glBufferSubData(target, offset, data);
    }

    @Override
    public void glBufferSubData(int target, long offset, long size, long data) {
        GL15.glBufferSubData(target, offset, MemoryUtilities.memByteBuffer(data, (int) size));
    }

    @Override
    public void glBufferStorage(int target, long size, int flags) {
        if (GLContext.getCapabilities().GL_ARB_buffer_storage) {
            ARBBufferStorage.glBufferStorage(target, size, flags);
        } else {
            throw new UnsupportedOperationException("GL_ARB_buffer_storage not available");
        }
    }

    @Override
    public ByteBuffer glMapBufferRange(int target, long offset, long length, int flags) {
        if (GLContext.getCapabilities().OpenGL30) {
            return GL30.glMapBufferRange(target, offset, length, flags, null);
        } else if (GLContext.getCapabilities().GL_ARB_map_buffer_range) {
            return ARBMapBufferRange.glMapBufferRange(target, offset, length, flags, null);
        } else {
            throw new UnsupportedOperationException("glMapBufferRange not available");
        }
    }

    @Override
    public long nglMapBuffer(int target, int access) {
        ByteBuffer buf = GL15.glMapBuffer(target, access, null);
        return buf != null ? MemoryUtilities.memAddress(buf) : 0L;
    }

    @Override
    public ByteBuffer glMapBuffer(int target, int access) {
        return GL15.glMapBuffer(target, access, null);
    }

    @Override
    public void glUnmapBuffer(int target) {
        GL15.glUnmapBuffer(target);
    }

    @Override
    public void glFlushMappedBufferRange(int target, long offset, long length) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glFlushMappedBufferRange(target, offset, length);
        } else if (GLContext.getCapabilities().GL_ARB_map_buffer_range) {
            ARBMapBufferRange.glFlushMappedBufferRange(target, offset, length);
        } else {
            throw new UnsupportedOperationException("glFlushMappedBufferRange not available");
        }
    }

    @Override
    public void glCopyBufferSubData(int readTarget, int writeTarget, long readOffset, long writeOffset, long size) {
        if (GLContext.getCapabilities().OpenGL31) {
            GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        } else if (GLContext.getCapabilities().GL_ARB_copy_buffer) {
            ARBCopyBuffer.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
        } else {
            throw new UnsupportedOperationException("glCopyBufferSubData not available");
        }
    }

    @Override
    public void glBindBufferBase(int target, int index, int buffer) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glBindBufferBase(target, index, buffer);
        } else if (GLContext.getCapabilities().GL_ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glBindBufferBase(target, index, buffer);
        } else {
            throw new UnsupportedOperationException("glBindBufferBase not available");
        }
    }

    // ===================== VAO OPERATIONS =====================

    @Override
    public int glGenVertexArrays() {
        switch (vaoMode) {
            case CORE:
                return GL30.glGenVertexArrays();
            case ARB:
                return ARBVertexArrayObject.glGenVertexArrays();
            case APPLE:
                return APPLEVertexArrayObject.glGenVertexArraysAPPLE();
            case NONE:
            default:
                throw new UnsupportedOperationException("VAO not supported");
        }
    }

    @Override
    public void glDeleteVertexArrays(int array) {
        switch (vaoMode) {
            case CORE:
                GL30.glDeleteVertexArrays(array);
                break;
            case ARB:
                ARBVertexArrayObject.glDeleteVertexArrays(array);
                break;
            case APPLE:
                APPLEVertexArrayObject.glDeleteVertexArraysAPPLE(array);
                break;
            case NONE:
            default:
                throw new UnsupportedOperationException("VAO not supported");
        }
    }

    @Override
    public void glBindVertexArray(int array) {
        switch (vaoMode) {
            case CORE:
                GL30.glBindVertexArray(array);
                break;
            case ARB:
                ARBVertexArrayObject.glBindVertexArray(array);
                break;
            case APPLE:
                APPLEVertexArrayObject.glBindVertexArrayAPPLE(array);
                break;
            case NONE:
            default:
                throw new UnsupportedOperationException("VAO not supported");
        }
    }

    @Override
    public void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }

    @Override
    public void glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        switch (vertexAttribIMode) {
            case CORE:
                GL30.glVertexAttribIPointer(index, size, type, stride, pointer);
                break;
            case EXT:
                EXTGpuShader4.glVertexAttribIPointerEXT(index, size, type, stride, pointer);
                break;
            case NONE:
            default:
                throw new UnsupportedOperationException("glVertexAttribIPointer not supported");
        }
    }

    @Override
    public void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }

    @Override
    public void glVertexAttribDivisor(int index, int divisor) {
        ContextCapabilities caps = GLContext.getCapabilities();
        if (caps.OpenGL33) {
            GL33.glVertexAttribDivisor(index, divisor);
        } else if (caps.GL_ARB_instanced_arrays) {
            ARBInstancedArrays.glVertexAttribDivisorARB(index, divisor);
        } else {
            throw new UnsupportedOperationException("glVertexAttribDivisor not available");
        }
    }

    // ===================== SHADER OPERATIONS =====================

    @Override
    public int glCreateShader(int type) {
        return GL20.glCreateShader(type);
    }

    @Override
    public void glShaderSource(int shader, CharSequence source) {
        GL20.glShaderSource(shader, source);
    }

    @Override
    public void glShaderSourceSafe(int shader, CharSequence source) {
        // AMD driver workaround: pass null for string length to force null-terminator reliance.
        // Some AMD drivers don't receive or interpret the length correctly, resulting in an
        // access violation when the driver tries to read past the string memory.
        // In LWJGL2, we use the normal method which should be null-terminated aware.
        ByteBuffer sourceBuffer = MemoryUtilities.memUTF8(source, true);
        GL20.glShaderSource(shader, sourceBuffer);
    }

    @Override
    public void glCompileShader(int shader) {
        GL20.glCompileShader(shader);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        return GL20.glGetShaderInfoLog(shader, maxLength);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return GL20.glGetShaderi(shader, pname);
    }

    @Override
    public void glDeleteShader(int shader) {
        GL20.glDeleteShader(shader);
    }

    @Override
    public int glCreateProgram() {
        return GL20.glCreateProgram();
    }

    @Override
    public void glAttachShader(int program, int shader) {
        GL20.glAttachShader(program, shader);
    }

    @Override
    public void glLinkProgram(int program) {
        GL20.glLinkProgram(program);
    }

    @Override
    public String glGetProgramInfoLog(int program, int maxLength) {
        return GL20.glGetProgramInfoLog(program, maxLength);
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return GL20.glGetProgrami(program, pname);
    }

    @Override
    public void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }

    @Override
    public void glDeleteProgram(int program) {
        GL20.glDeleteProgram(program);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        GL20.glBindAttribLocation(program, index, name);
    }

    @Override
    public void glBindFragDataLocation(int program, int colorNumber, CharSequence name) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glBindFragDataLocation(program, colorNumber, name);
        } else {
            throw new UnsupportedOperationException("glBindFragDataLocation not supported (requires OpenGL 3.0)");
        }
    }

    // ===================== UNIFORM OPERATIONS =====================

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return GL20.glGetUniformLocation(program, name);
    }

    @Override
    public int glGetUniformBlockIndex(int program, CharSequence name) {
        if (GLContext.getCapabilities().OpenGL31) {
            return GL31.glGetUniformBlockIndex(program, name);
        } else if (GLContext.getCapabilities().GL_ARB_uniform_buffer_object) {
            return ARBUniformBufferObject.glGetUniformBlockIndex(program, name);
        } else {
            throw new UnsupportedOperationException("glGetUniformBlockIndex not available");
        }
    }

    @Override
    public void glUniformBlockBinding(int program, int blockIndex, int blockBinding) {
        if (GLContext.getCapabilities().OpenGL31) {
            GL31.glUniformBlockBinding(program, blockIndex, blockBinding);
        } else if (GLContext.getCapabilities().GL_ARB_uniform_buffer_object) {
            ARBUniformBufferObject.glUniformBlockBinding(program, blockIndex, blockBinding);
        } else {
            throw new UnsupportedOperationException("glUniformBlockBinding not available");
        }
    }

    @Override
    public void glUniform1f(int location, float v0) {
        GL20.glUniform1f(location, v0);
    }

    @Override
    public void glUniform1i(int location, int v0) {
        GL20.glUniform1i(location, v0);
    }

    @Override
    public void glUniform1fv(int location, FloatBuffer value) {
        GL20.glUniform1(location, value);
    }

    @Override
    public void glUniform2i(int location, int v0, int v1) {
        GL20.glUniform2i(location, v0, v1);
    }

    @Override
    public void glUniform3f(int location, float v0, float v1, float v2) {
        GL20.glUniform3f(location, v0, v1, v2);
    }

    @Override
    public void glUniform3fv(int location, FloatBuffer value) {
        GL20.glUniform3(location, value);
    }

    @Override
    public void glUniform3fv(int location, float[] value) {
        if (value.length == 3) {
            GL20.glUniform3f(location, value[0], value[1], value[2]);
            return;
        }

        if (value.length % 3 != 0) {
            throw new IllegalArgumentException("Array length must be a multiple of 3");
        }

        try (MemoryStack stack = this.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(value.length);
            buffer.put(value).flip();
            GL20.glUniform3(location, buffer);
        }
    }

    @Override
    public void glUniform4fv(int location, FloatBuffer value) {
        GL20.glUniform4(location, value);
    }

    @Override
    public void glUniform4fv(int location, float[] value) {
        if (value.length == 4) {
            GL20.glUniform4f(location, value[0], value[1], value[2], value[3]);
            return;
        }

        if (value.length % 4 != 0) {
            throw new IllegalArgumentException("Array length must be a multiple of 4");
        }

        try (MemoryStack stack = this.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(value.length);
            buffer.put(value).flip();
            GL20.glUniform4(location, buffer);
        }
    }

    @Override
    public void glUniformMatrix3fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4(location, transpose, value);
    }

    // ===================== DRAW OPERATIONS =====================

    @Override
    public void glDrawElementsBaseVertex(int mode, int count, int type, long indices, int basevertex) {
        if (GLContext.getCapabilities().OpenGL32) {
            GL32.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        } else if (GLContext.getCapabilities().GL_ARB_draw_elements_base_vertex) {
            ARBDrawElementsBaseVertex.glDrawElementsBaseVertex(mode, count, type, indices, basevertex);
        } else {
            throw new UnsupportedOperationException("glDrawElementsBaseVertex not available");
        }
    }

    @Override
    public void glMultiDrawElementsBaseVertex(int mode, long pCount, int type, long pIndices, int drawcount, long pBaseVertex) {
        // LWJGL2 doesn't have native pointer version - use fallback loop
        ContextCapabilities caps = GLContext.getCapabilities();
        if (caps.OpenGL32 || caps.GL_ARB_draw_elements_base_vertex) {
            for (int i = 0; i < drawcount; i++) {
                int count = MemoryUtilities.memGetInt(pCount + (long) i * 4);
                if (count > 0) {
                    long indices = MemoryUtilities.memGetAddress(pIndices + (long) i * Pointer.POINTER_SIZE);
                    int baseVertex = MemoryUtilities.memGetInt(pBaseVertex + (long) i * 4);
                    if (caps.OpenGL32) {
                        GL32.glDrawElementsBaseVertex(mode, count, type, indices, baseVertex);
                    } else {
                        ARBDrawElementsBaseVertex.glDrawElementsBaseVertex(mode, count, type, indices, baseVertex);
                    }
                }
            }
        } else {
            throw new UnsupportedOperationException("glMultiDrawElementsBaseVertex not available");
        }
    }

    @Override
    public void glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawcount, int stride) {
        if (GLContext.getCapabilities().OpenGL43) {
            GL43.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else if (GLContext.getCapabilities().GL_ARB_multi_draw_indirect) {
            ARBMultiDrawIndirect.glMultiDrawElementsIndirect(mode, type, indirect, drawcount, stride);
        } else {
            throw new UnsupportedOperationException("glMultiDrawElementsIndirect not available");
        }
    }

    // ===================== SYNC OPERATIONS =====================

    @Override
    public long glFenceSync(int condition, int flags) {
        GLSync sync;
        if (GLContext.getCapabilities().OpenGL32) {
            sync = GL32.glFenceSync(condition, flags);
        } else if (GLContext.getCapabilities().GL_ARB_sync) {
            sync = ARBSync.glFenceSync(condition, flags);
        } else {
            throw new UnsupportedOperationException("glFenceSync not available");
        }
        long pointer = sync.getPointer();
        syncObjects.put(pointer, sync);
        return pointer;
    }

    @Override
    public int glClientWaitSync(long sync, int flags, long timeout) {
        GLSync obj = syncObjects.get(sync);
        if (GLContext.getCapabilities().OpenGL32) {
            return GL32.glClientWaitSync(obj, flags, timeout);
        } else if (GLContext.getCapabilities().GL_ARB_sync) {
            return ARBSync.glClientWaitSync(obj, flags, timeout);
        } else {
            throw new UnsupportedOperationException("glClientWaitSync not available");
        }
    }

    @Override
    public int glGetSynci(long sync, int pname, IntBuffer length) {
        // LWJGL2 glGetSynci doesn't take length buffer - it returns single value directly
        GLSync obj = syncObjects.get(sync);
        int result;
        if (GLContext.getCapabilities().OpenGL32) {
            result = GL32.glGetSynci(obj, pname);
        } else if (GLContext.getCapabilities().GL_ARB_sync) {
            result = ARBSync.glGetSynci(obj, pname);
        } else {
            throw new UnsupportedOperationException("glGetSynci not available");
        }
        if (length != null) {
            length.put(0, 1); // Always returns single value
        }
        return result;
    }

    @Override
    public void glWaitSync(long sync, int flags, long timeout) {
        GLSync obj = syncObjects.get(sync);
        if (GLContext.getCapabilities().OpenGL32) {
            GL32.glWaitSync(obj, flags, timeout);
        } else if (GLContext.getCapabilities().GL_ARB_sync) {
            ARBSync.glWaitSync(obj, flags, timeout);
        } else {
            throw new UnsupportedOperationException("glWaitSync not available");
        }
    }

    @Override
    public void glDeleteSync(long sync) {
        GLSync obj = syncObjects.remove(sync);
        if (obj == null) return;
        if (GLContext.getCapabilities().OpenGL32) {
            GL32.glDeleteSync(obj);
        } else if (GLContext.getCapabilities().GL_ARB_sync) {
            ARBSync.glDeleteSync(obj);
        } else {
            throw new UnsupportedOperationException("glDeleteSync not available");
        }
    }

    // ===================== QUERY OPERATIONS =====================

    @Override
    public int glGenQueries() {
        return GL15.glGenQueries();
    }

    @Override
    public void glDeleteQueries(int query) {
        GL15.glDeleteQueries(query);
    }

    @Override
    public void glBeginQuery(int target, int id) {
        if (timerQueryMode != TimerQueryMode.NONE) {
            GL15.glBeginQuery(target, id);
        }
    }

    @Override
    public void glEndQuery(int target) {
        if (timerQueryMode != TimerQueryMode.NONE) {
            GL15.glEndQuery(target);
        }
    }

    @Override
    public long glGetQueryObjectui64(int id, int pname) {
        switch (timerQueryMode) {
            case CORE:
                return GL33.glGetQueryObjectui64(id, pname);
            case ARB:
                return ARBTimerQuery.glGetQueryObjectui64(id, pname);
            case EXT:
                return EXTTimerQuery.glGetQueryObjectuEXT(id, pname);
            case NONE:
            default:
                return 0L;
        }
    }

    // ===================== DEBUG OPERATIONS =====================

    @Override
    public PrintStream getDebugStream() { return System.err; }

    @Override
    public int setupDebugCallback(DebugMessageHandler handler) {
        return debugSupport.setupDebugCallback(handler);
    }

    @Override
    public void disableDebugCallback() {
        debugSupport.disableDebugCallback();
    }

    @Override
    public void glObjectLabel(int identifier, int name, CharSequence label) {
        if (debugMode == DebugMode.KHR) {
            KHRDebug.glObjectLabel(identifier, name, label);
        }
    }

    @Override
    public void glPushDebugGroup(int source, int id, CharSequence message) {
        if (debugMode == DebugMode.KHR) {
            KHRDebug.glPushDebugGroup(source, id, message);
        }
    }

    @Override
    public void glPopDebugGroup() {
        if (debugMode == DebugMode.KHR) {
            KHRDebug.glPopDebugGroup();
        }
    }

    // ===================== TEXTURE OPERATIONS =====================

    @Override
    public int glGenTextures() {
        return GL11.glGenTextures();
    }

    @Override
    public void glGenTextures(int[] textures) {
        IntBuffer buf = MemoryUtilities.memAllocInt(textures.length);
        GL11.glGenTextures(buf);
        buf.get(textures);
        MemoryUtilities.memFree(buf);
    }

    @Override
    public void glDeleteTextures(int texture) {
        GL11.glDeleteTextures(texture);
    }

    @Override
    public void glDeleteTextures(int[] textures) {
        IntBuffer buf = (IntBuffer) MemoryUtilities.memAllocInt(textures.length).put(textures).flip();
        GL11.glDeleteTextures(buf);
        MemoryUtilities.memFree(buf);
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target, texture);
    }

    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return GL11.glGetTexLevelParameteri(target, level, pname);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        GL11.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        GL11.glPixelStorei(pname, param);
    }

    // ===================== FRAMEBUFFER OPERATIONS =====================

    @Override
    public int glGenFramebuffers() {
        if (GLContext.getCapabilities().OpenGL30) {
            return GL30.glGenFramebuffers();
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            return EXTFramebufferObject.glGenFramebuffersEXT();
        } else {
            throw new UnsupportedOperationException("Framebuffers not available");
        }
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glDeleteFramebuffers(framebuffer);
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            EXTFramebufferObject.glDeleteFramebuffersEXT(framebuffer);
        } else {
            throw new UnsupportedOperationException("Framebuffers not available");
        }
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glBindFramebuffer(target, framebuffer);
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            EXTFramebufferObject.glBindFramebufferEXT(target, framebuffer);
        } else {
            throw new UnsupportedOperationException("Framebuffers not available");
        }
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        if (GLContext.getCapabilities().OpenGL30) {
            return GL30.glCheckFramebufferStatus(target);
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            return EXTFramebufferObject.glCheckFramebufferStatusEXT(target);
        } else {
            throw new UnsupportedOperationException("Framebuffers not available");
        }
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        if (GLContext.getCapabilities().OpenGL30) {
            GL30.glFramebufferTexture2D(target, attachment, textarget, texture, level);
        } else if (GLContext.getCapabilities().GL_EXT_framebuffer_object) {
            EXTFramebufferObject.glFramebufferTexture2DEXT(target, attachment, textarget, texture, level);
        } else {
            throw new UnsupportedOperationException("Framebuffers not available");
        }
    }

    // ===================== STATE OPERATIONS =====================

    @Override
    public void glEnable(int cap) {
        GL11.glEnable(cap);
    }

    @Override
    public void glDisable(int cap) {
        GL11.glDisable(cap);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GL11.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL14.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void glDepthFunc(int func) {
        GL11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        GL11.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void glClear(int mask) {
        GL11.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public int glGetError() {
        return GL11.glGetError();
    }

    // ===================== COMPATIBILITY PROFILE (GL1.x) =====================

    @Override
    public void glMatrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    @Override
    public void glLoadMatrixf(FloatBuffer m) {
        GL11.glLoadMatrix(m);
    }

    // ===================== MISC GL =====================

    @Override
    public int glGetInteger(int pname) {
        return GL11.glGetInteger(pname);
    }

    @Override
    public void glGetIntegerv(int pname, int[] params) {
        IntBuffer buf = MemoryUtilities.memAllocInt(params.length);
        GL11.glGetInteger(pname, buf);
        buf.get(params);
        MemoryUtilities.memFree(buf);
    }

    @Override
    public boolean glGetBoolean(int pname) {
        return GL11.glGetBoolean(pname);
    }

    @Override
    public String glGetString(int pname) {
        return GL11.glGetString(pname);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return GL20.glGetAttribLocation(program, name);
    }

    // ===================== MEMORY STACK OPERATIONS =====================

    @Override
    public MemoryStack stackPush() {
        return new LWJGL2MemoryStack(com.gtnewhorizon.gtnhlib.bytebuf.MemoryStack.stackPush());
    }

    // ===================== NATIVE MEMORY OPERATIONS =====================

    @Override
    public long nmemAlloc(long size) {
        return MemoryUtilities.nmemAlloc(size);
    }

    @Override
    public long nmemCalloc(long count, long size) {
        return MemoryUtilities.nmemCalloc(count, size);
    }

    @Override
    public long nmemAlignedAlloc(long alignment, long size) {
        // Ported from FalsePattern's lwjgl2-celeritas LegacyMemoryAdapter
        int required = 8;
        alignment = Math.max(alignment, 8);
        long prefixLength = Math.max(alignment, required) + required;
        long capacity = size + prefixLength;
        long addr = MemoryUtilities.nmemAlloc(capacity);
        if (addr == 0) return 0;
        long shiftBy = alignment - (addr % alignment);
        if (shiftBy < required) {
            shiftBy += alignment;
        }
        long finalAddr = addr + shiftBy;
        MemoryUtilities.memPutLong(finalAddr - 8, addr);
        return finalAddr;
    }

    @Override
    public long nmemRealloc(long ptr, long size) {
        return MemoryUtilities.nmemRealloc(ptr, size);
    }

    @Override
    public void nmemFree(long ptr) {
        MemoryUtilities.nmemFree(ptr);
    }

    @Override
    public void nmemAlignedFree(long ptr) {
        if (ptr == 0) return;
        long realAddr = MemoryUtilities.memGetLong(ptr - 8);
        MemoryUtilities.nmemFree(realAddr);
    }

    @Override
    public ByteBuffer memAlloc(int size) {
        return MemoryUtilities.memAlloc(size);
    }

    @Override
    public ByteBuffer memCalloc(int size) {
        return MemoryUtilities.memCalloc(size);
    }

    @Override
    public ByteBuffer memRealloc(ByteBuffer buffer, int size) {
        return MemoryUtilities.memRealloc(buffer, size);
    }

    @Override
    public void memFree(Buffer buffer) {
        MemoryUtilities.memFree(buffer);
    }

    @Override
    public ByteBuffer memByteBuffer(long address, int capacity) {
        return MemoryUtilities.memByteBuffer(address, capacity);
    }

    @Override
    public long memAddress(Buffer buffer) {
        return MemoryUtilities.memAddress(buffer);
    }

    @Override
    public long memAddress(Buffer buffer, int position) {
        if (buffer == null) {
            return position;
        }
        return MemoryUtilities.memAddress0(buffer) + position;
    }

    @Override
    public void memSet(long address, int value, long bytes) {
        MemoryUtilities.memSet(address, value, bytes);
    }

    @Override
    public void memCopy(long src, long dst, long bytes) {
        MemoryUtilities.memCopy(src, dst, bytes);
    }

    @Override
    public void memPutByte(long address, byte value) {
        MemoryUtilities.memPutByte(address, value);
    }

    @Override
    public void memPutShort(long address, short value) {
        MemoryUtilities.memPutShort(address, value);
    }

    @Override
    public void memPutInt(long address, int value) {
        MemoryUtilities.memPutInt(address, value);
    }

    @Override
    public void memPutFloat(long address, float value) {
        MemoryUtilities.memPutFloat(address, value);
    }

    @Override
    public void memPutLong(long address, long value) {
        MemoryUtilities.memPutLong(address, value);
    }

    @Override
    public void memPutAddress(long address, long value) {
        MemoryUtilities.memPutAddress(address, value);
    }

    @Override
    public byte memGetByte(long address) {
        return MemoryUtilities.memGetByte(address);
    }

    @Override
    public short memGetShort(long address) {
        return MemoryUtilities.memGetShort(address);
    }

    @Override
    public int memGetInt(long address) {
        return MemoryUtilities.memGetInt(address);
    }

    @Override
    public float memGetFloat(long address) {
        return MemoryUtilities.memGetFloat(address);
    }

    @Override
    public long memGetLong(long address) {
        return MemoryUtilities.memGetLong(address);
    }

    @Override
    public long memGetAddress(long address) {
        return MemoryUtilities.memGetAddress(address);
    }

    @Override
    public ByteBuffer memSlice(ByteBuffer buffer, int offset, int capacity) {
        long address = MemoryUtilities.memAddress(buffer) + offset;
        return MemoryUtilities.memByteBuffer(address, capacity);
    }
}
