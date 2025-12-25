package com.mitchej123.glsm.impl;

import com.mitchej123.glsm.GLStateManagerService;
import org.lwjgl.opengl.GL11;

import static com.mitchej123.lwjgl.LWJGLServiceProvider.LWJGL;
import org.lwjgl.opengl.GL13;

public class PassThroughGLStateManager implements GLStateManagerService {

    @Override
    public int glGetInteger(int pname) {
        return LWJGL.glGetInteger(pname);
    }

    @Override
    public String glGetString(int pname) {
        return LWJGL.glGetString(pname);
    }

    @Override
    public void glBindFramebuffer(int target, int framebuffer) {
        LWJGL.glBindFramebuffer(target, framebuffer);
    }

    @Override
    public int glCheckFramebufferStatus(int target) {
        return LWJGL.glCheckFramebufferStatus(target);
    }

    @Override
    public void glDeleteFramebuffers(int framebuffer) {
        LWJGL.glDeleteFramebuffers(framebuffer);
    }

    @Override
    public int glGenFramebuffers() {
        return LWJGL.glGenFramebuffers();
    }

    @Override
    public int glGetProgrami(int program, int pname) {
        return LWJGL.glGetProgrami(program, pname);
    }

    @Override
    public void glAttachShader(int program, int shader) {
        LWJGL.glAttachShader(program, shader);
    }

    @Override
    public void glDeleteShader(int shader) {
        LWJGL.glDeleteShader(shader);
    }

    @Override
    public int glCreateShader(int type) {
        return LWJGL.glCreateShader(type);
    }

    @Override
    public void glCompileShader(int shader) {
        LWJGL.glCompileShader(shader);
    }

    @Override
    public int glGetShaderi(int shader, int pname) {
        return LWJGL.glGetShaderi(shader, pname);
    }

    @Override
    public String glGetShaderInfoLog(int shader, int maxLength) {
        return LWJGL.glGetShaderInfoLog(shader, maxLength);
    }

    @Override
    public void glUseProgram(int program) {
        LWJGL.glUseProgram(program);
    }

    @Override
    public int glCreateProgram() {
        return LWJGL.glCreateProgram();
    }

    @Override
    public void glDeleteProgram(int program) {
        LWJGL.glDeleteProgram(program);
    }

    @Override
    public void glLinkProgram(int program) {
        LWJGL.glLinkProgram(program);
    }

    @Override
    public int glGetUniformLocation(int program, CharSequence name) {
        return LWJGL.glGetUniformLocation(program, name);
    }

    @Override
    public void glUniform1i(int location, int value) {
        LWJGL.glUniform1i(location, value);
    }

    @Override
    public int glGetAttribLocation(int program, CharSequence name) {
        return LWJGL.glGetAttribLocation(program, name);
    }

    @Override
    public void glBindAttribLocation(int program, int index, CharSequence name) {
        LWJGL.glBindAttribLocation(program, index, name);
    }

    @Override
    public int glGenVertexArrays() {
        return LWJGL.glGenVertexArrays();
    }

    @Override
    public void glBindVertexArray(int array) {
        LWJGL.glBindVertexArray(array);
    }

    @Override
    public void glCopyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height) {
        LWJGL.glCopyTexSubImage2D(target, level, xoffset, yoffset, x, y, width, height);
    }

    @Override
    public void enableCullFace() {
        LWJGL.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableCullFace() {
        LWJGL.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void enableBlend() {
        LWJGL.glEnable(GL11.GL_BLEND);
    }

    @Override
    public void disableBlend() {
        LWJGL.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void glBlendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        LWJGL.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }

    @Override
    public void enableDepthTest() {
        LWJGL.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void disableDepthTest() {
        LWJGL.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void glDepthFunc(int func) {
        LWJGL.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        LWJGL.glDepthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        LWJGL.glViewport(x, y, width, height);
    }

    @Override
    public void glColorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        LWJGL.glColorMask(red, green, blue, alpha);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        LWJGL.glClearColor(red, green, blue, alpha);
    }

    @Override
    public int glGetTexLevelParameteri(int target, int level, int pname) {
        return LWJGL.glGetTexLevelParameteri(target, level, pname);
    }

    @Override
    public int glGetTexLevelParameter(int target, int level, int pname) {
        return LWJGL.glGetTexLevelParameteri(target, level, pname);
    }

    @Override
    public void glFramebufferTexture2D(int target, int attachment, int textarget, int texture, int level) {
        LWJGL.glFramebufferTexture2D(target, attachment, textarget, texture, level);
    }

    @Override
    public int glGenTextures() {
        return LWJGL.glGenTextures();
    }

    @Override
    public void glGenTextures(int[] textures) {
        LWJGL.glGenTextures(textures);
    }

    @Override
    public void glDeleteTextures(int texture) {
        LWJGL.glDeleteTextures(texture);
    }

    @Override
    public void glDeleteTextures(int[] textures) {
        LWJGL.glDeleteTextures(textures);
    }

    @Override
    public void glActiveTexture(int texture) {
        LWJGL.glActiveTexture(texture);
    }

    @Override
    public int glGenBuffers() {
        return LWJGL.glGenBuffers();
    }

    @Override
    public void glBindBuffer(int target, int buffer) {
        LWJGL.glBindBuffer(target, buffer);
    }

    @Override
    public void glDeleteBuffers(int buffer) {
        LWJGL.glDeleteBuffers(buffer);
    }

    @Override
    public void glDeleteVertexArrays(int array) {
        LWJGL.glDeleteVertexArrays(array);
    }

    @Override
    public void glPixelStorei(int pname, int param) {
        LWJGL.glPixelStorei(pname, param);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        LWJGL.glClear(mask);
    }

    @Override
    public void glClear(int mask) {
        LWJGL.glClear(mask);
    }

    @Override
    public void bindTexture(int texture) {
        LWJGL.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @Override
    public int getActiveTexture() {
        return LWJGL.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
    }

    @Override
    public int getActiveTextureAccessor() {
        return LWJGL.glGetInteger(GL13.GL_ACTIVE_TEXTURE) - GL13.GL_TEXTURE0;
    }

    @Override
    public int getBoundTexture(int internalUnit) {
        return LWJGL.glGetInteger(internalUnit);
    }

    @Override
    public int getActiveBoundTexture() {
        return LWJGL.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    }

    @Override
    public int getViewportWidth() {
        int[] viewport = new int[4];
        LWJGL.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        return viewport[2]; // viewport[2] is width
    }

    @Override
    public int getViewportHeight() {
        int[] viewport = new int[4];
        LWJGL.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        return viewport[3]; // viewport[3] is height
    }

    @Override
    public boolean getDepthStateMask() {
        return LWJGL.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    }

    @Override
    public boolean isBlendEnabled() {
        return LWJGL.glGetBoolean(GL11.GL_BLEND);
    }

    @Override
    public void setBoundTexture(int unit, int texture) {
        LWJGL.glBindTexture(GL13.GL_TEXTURE0 + unit, texture);
    }

    @Override
    public int getTextureBinding(int unit) {
        int currentActive = LWJGL.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        LWJGL.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        int binding = LWJGL.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        LWJGL.glActiveTexture(currentActive);
        return binding;
    }
}
