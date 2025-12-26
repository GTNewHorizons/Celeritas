package com.mitchej123.glsm;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;

/**
 * Abstraction for RenderSystem operations across different Minecraft versions.
 */
public interface RenderSystemService {

    /** Higher priority services are preferred. */
    default int getPriority() { return 0; }

    // ===================== TEXTURE OPERATIONS =====================

    void glActiveTexture(int texture);

    /** Non-standard texture bind (assumes GL_TEXTURE_2D). */
    void bindTexture(int texture);

    // ===================== STATE OPERATIONS =====================

    void enableCullFace();
    void disableCullFace();

    void enableBlend();
    void disableBlend();
    void defaultBlendFunc();
    void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    /** Mojang-specific: resets blend state tracking to unknown. */
    void setUnknownBlendState();

    void enableDepthTest();
    void disableDepthTest();
    void depthFunc(int depthFunc);
    void depthMask(boolean flag);

    void glViewport(int x, int y, int width, int height);
    void glClearColor(float red, float green, float blue, float alpha);

    /** Non-standard clear with optional error checking. */
    void clear(int mask, boolean checkError);

    // ===================== UNIFORM OPERATIONS =====================

    void glUniform1i(int location, int value);
    void glUniformMatrix3(int location, boolean transpose, FloatBuffer value);
    void glUniformMatrix4(int location, boolean transpose, FloatBuffer value);

    // ===================== THREAD ASSERTIONS =====================

    void assertOnRenderThread();
    void assertOnRenderThreadOrInit();

    // ===================== SHADER STATE =====================

    void setShaderTexture(int shaderTexture, int textureId);
    int getShaderTexture(int shaderTexture);

    void setShaderColor(float red, float green, float blue, float alpha);
    float[] getShaderColor();

    void setShaderLineWidth(float lineWidth);
    float getShaderLineWidth();

    // ===================== FOG STATE =====================

    void setShaderFogColor(float red, float green, float blue, float alpha);
    float[] getShaderFogColor();

    void setShaderFogStart(float start);
    float getShaderFogStart();

    void setShaderFogEnd(float end);
    float getShaderFogEnd();

    void setFogShape(int shape);
    int getFogShape();

    // ===================== PROJECTION MATRIX =====================

    Matrix4f getProjectionMatrix();
    void setProjectionMatrixOrth(Matrix4f projectionMatrix);
    void setProjectionMatrixOrigin(Matrix4f projectionMatrix);

    default void setPositionShader() {}
}
