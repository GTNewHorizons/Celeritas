package com.mitchej123.glsm.impl;

import com.mitchej123.glsm.RenderSystemService;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

public abstract class PassThroughRenderSystem implements RenderSystemService {
    // Profile detection - initialized during IrisRenderSystem.initRenderer()
    private static boolean isCompatibilityProfile = false;
    private static boolean profileInitialized = false;
    
    /**
     * Initialize profile detection. Called during IrisRenderSystem.initRenderer()
     * when OpenGL context is guaranteed to be available.
     */
    public static void initializeProfileDetection() {
        if (profileInitialized) {
            return; // Already initialized
        }
        
        // Use LWJGL capabilities to determine profile, similar to IrisRenderSystem
        var caps = GL.getCapabilities();
        
        if (caps.OpenGL32 || caps.OpenGL31) {
            // OpenGL 3.1+ - check for ARB_compatibility extension
            // If we have the compatibility extension, we're in compatibility mode
            isCompatibilityProfile = caps.GL_ARB_compatibility;
        } else {
            // OpenGL 3.0 and below - inherently compatibility profile
            // (OpenGL 3.0+ introduced the concept of profiles, older versions are always compatibility)
            isCompatibilityProfile = true;
        }
        
        profileInitialized = true;
    }
    
    /**
     * Returns whether the current OpenGL context is in compatibility profile.
     * This affects whether deprecated matrix functions are available.
     * Must be called after initializeProfileDetection().
     */
    public static boolean isCompatibilityProfile() {
        if (!profileInitialized) {
            throw new IllegalStateException("Profile detection not initialized. Call initializeProfileDetection() first.");
        }
        return isCompatibilityProfile;
    }
    
    @Override
    public void glActiveTexture(int texture) {
        GL13.glActiveTexture(texture);
    }

    @Override
    public void enableCullFace() {
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    @Override
    public void disableCullFace() {
        GL11.glDisable(GL11.GL_CULL_FACE);
    }

    @Override
    public void enableBlend() {
        GL11.glEnable(GL11.GL_BLEND);
    }

    @Override
    public void disableBlend() {
        GL11.glDisable(GL11.GL_BLEND);
    }

    @Override
    public void setUnknownBlendState() {
        // This is a Mojang-specific method that resets blend state to unknown
        // In a pass-through implementation, we don't need to do anything special
        // as the actual OpenGL state is managed directly
    }

    @Override
    public void enableDepthTest() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void disableDepthTest() {
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    @Override
    public void depthFunc(int depthFunc) {
        GL11.glDepthFunc(depthFunc);
    }

    @Override
    public void depthMask(boolean flag) {
        GL11.glDepthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }

    @Override
    public void bindTexture(int texture) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @Override
    public void glUniform1i(int location, int value) {
        GL20.glUniform1i(location, value);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix3fv(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        GL20.glUniformMatrix4fv(location, transpose, value);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        GL11.glClear(mask);
        if (checkError) {
            int error = GL11.glGetError();
            if (error != 0) {
                throw new RuntimeException("OpenGL Error: " + error);
            }
        }
    }

    @Override
    public void assertOnRenderThread() {
        // Noop
    }

    @Override
    public void assertOnRenderThreadOrInit() {
        // Noop
    }

    private final int[] shaderTextures = new int[16]; // Support up to 16 texture units
    
    @Override
    public void setShaderTexture(int shaderTexture, int textureId) {
        if (shaderTexture >= 0 && shaderTexture < shaderTextures.length) {
            shaderTextures[shaderTexture] = textureId;
        }
    }

    @Override
    public int getShaderTexture(int shaderTexture) {
        if (shaderTexture >= 0 && shaderTexture < shaderTextures.length) {
            return shaderTextures[shaderTexture];
        }
        return 0;
    }

    private final float[] shaderColor = new float[4]; // RGBA
    
    @Override
    public void setShaderColor(float red, float green, float blue, float alpha) {
        shaderColor[0] = red;
        shaderColor[1] = green;
        shaderColor[2] = blue;
        shaderColor[3] = alpha;
    }

    @Override
    public float[] getShaderColor() {
        return shaderColor;
    }

    private final float[] shaderFogColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f}; // RGBA
    private float shaderFogStart = 0.0f;
    private float shaderFogEnd = 0.0f;
    private int fogShape = 0;
    
    @Override
    public void setShaderFogColor(float red, float green, float blue, float alpha) {
        shaderFogColor[0] = red;
        shaderFogColor[1] = green;
        shaderFogColor[2] = blue;
        shaderFogColor[3] = alpha;
    }
    
    @Override
    public void setShaderFogStart(float start) {
        this.shaderFogStart = start;
    }
    
    @Override
    public void setShaderFogEnd(float end) {
        this.shaderFogEnd = end;
    }
    
    @Override
    public void setFogShape(int shape) {
        this.fogShape = shape;
    }
    
    @Override
    public float[] getShaderFogColor() {
        return shaderFogColor;
    }

    @Override
    public float getShaderFogStart() {
        return shaderFogStart;
    }

    @Override
    public float getShaderFogEnd() {
        return shaderFogEnd;
    }

    @Override
    public int getFogShape() {
        return fogShape;
    }

    private float shaderLineWidth = 1.0f;

    @Override
    public void setShaderLineWidth(float lineWidth) {
        this.shaderLineWidth = lineWidth;
    }

    @Override
    public float getShaderLineWidth() {
        return shaderLineWidth;
    }

    private final FloatBuffer PROJECTION_MATRIX_BUFFER = org.lwjgl.BufferUtils.createFloatBuffer(16);
    private final Matrix4f PROJECTION_MATRIX = new Matrix4f();
    @Override
    public Matrix4f getProjectionMatrix() {
        return new Matrix4f(PROJECTION_MATRIX);
    }

    @Override
    public void setProjectionMatrixOrth(Matrix4f projectionMatrix) {
        PROJECTION_MATRIX.set(projectionMatrix);
        
        if (isCompatibilityProfile()) {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            projectionMatrix.get(0, PROJECTION_MATRIX_BUFFER);
            GL11.glLoadMatrixf(PROJECTION_MATRIX_BUFFER);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    @Override
    public void setProjectionMatrixOrigin(Matrix4f projectionMatrix) {
        PROJECTION_MATRIX.set(projectionMatrix);
        
        if (isCompatibilityProfile()) {
            GL11.glMatrixMode(GL11.GL_PROJECTION);
            projectionMatrix.get(0, PROJECTION_MATRIX_BUFFER);
            GL11.glLoadMatrixf(PROJECTION_MATRIX_BUFFER);
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
        }
    }

    @Override
    public void defaultBlendFunc() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    @Override
    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        GL20.glBlendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
}
