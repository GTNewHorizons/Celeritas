package com.mitchej123.glsm;

import org.joml.Matrix4f;

import java.nio.FloatBuffer;
import java.util.ServiceLoader;

public interface RenderSystemService {
    RenderSystemService RENDER_SYSTEM = ServiceLoader.load(RenderSystemService.class).findFirst().orElseThrow();

    void glActiveTexture(int texture);

    void enableCullFace();
    void disableCullFace();

    void enableBlend();
    void disableBlend();
    void setUnknownBlendState(); // Mojang Addition

    void enableDepthTest();
    void disableDepthTest();
    void depthFunc(int depthFunc);
    void depthMask(boolean flag);

    void glViewport(int x, int y, int width, int height);

    void bindTexture(int texture); // Non-standard

    void glUniform1i(int location, int value);
    void glUniformMatrix3(int location, boolean transpose, FloatBuffer value);
    void glUniformMatrix4(int location, boolean transpose, FloatBuffer value);

    void glClearColor(float red, float green, float blue, float alpha);

    // Mojang Implementations
    void clear(int mask, boolean checkError); // Non-standard

    void assertOnRenderThread();
    void assertOnRenderThreadOrInit();

    void setShaderTexture(int shaderTexture, int textureId);
    int getShaderTexture(int shaderTexture);

    void setShaderColor(float red, float green, float blue, float alpha);

    float[] getShaderColor();

    void setShaderFogColor(float red, float green, float blue, float alpha);
    void setShaderFogStart(float start);
    void setShaderFogEnd(float end);
    void setFogShape(int shape);
    
    float[] getShaderFogColor();
    float getShaderFogStart();
    float getShaderFogEnd();
    int getFogShape();

    void setShaderLineWidth(float lineWidth);
    float getShaderLineWidth();

    Matrix4f getProjectionMatrix();

    default void setPositionShader() {};

    void setProjectionMatrixOrth(Matrix4f projectionMatrix);

    void setProjectionMatrixOrigin(Matrix4f projectionMatrix);

    // Blending methods
    void defaultBlendFunc();
    
    void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);
}
