package org.embeddedt.embeddium.impl.gl;

import com.mitchej123.glsm.impl.PassThroughRenderSystem;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.irisshaders.iris.gl.BooleanStateExtended;
import net.irisshaders.iris.mixin.GlStateManagerAccessor;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

import java.nio.FloatBuffer;

public class ModernRenderSystem extends PassThroughRenderSystem {
    @Override
    public void glActiveTexture(int texture) {
        RenderSystem.activeTexture(texture);
    }

    @Override
    public void enableCullFace() {
        RenderSystem.enableCull();
    }

    @Override
    public void disableCullFace() {
        RenderSystem.disableCull();
    }

    @Override
    public void enableBlend() {
        RenderSystem.enableBlend();
    }

    @Override
    public void disableBlend() {
        RenderSystem.disableBlend();
    }

    @Override
    public void setUnknownBlendState() {
        ((BooleanStateExtended) GlStateManagerAccessor.getBLEND().mode).setUnknownState();
    }

    @Override
    public void enableDepthTest() {
        RenderSystem.enableDepthTest();
    }

    @Override
    public void disableDepthTest() {
        RenderSystem.disableDepthTest();
    }

    @Override
    public void depthFunc(int depthFunc) {
        RenderSystem.depthFunc(depthFunc);
    }

    @Override
    public void depthMask(boolean flag) {
        RenderSystem.depthMask(flag);
    }

    @Override
    public void glViewport(int x, int y, int width, int height) {
        RenderSystem.viewport(x, y, width, height);
    }

    @Override
    public void bindTexture(int texture) {
        RenderSystem.bindTexture(texture);
    }

    @Override
    public void glUniform1i(int location, int value) {
        RenderSystem.glUniform1i(location, value);
    }

    @Override
    public void glUniformMatrix3(int location, boolean transpose, FloatBuffer value) {
        RenderSystem.glUniformMatrix3(location, transpose, value);
    }

    @Override
    public void glUniformMatrix4(int location, boolean transpose, FloatBuffer value) {
        RenderSystem.glUniformMatrix4(location, transpose, value);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        RenderSystem.clearColor(red, green, blue, alpha);
    }

    @Override
    public void clear(int mask, boolean checkError) {
        RenderSystem.clear(mask, checkError);
    }

    @Override
    public void assertOnRenderThread() {
        RenderSystem.assertOnRenderThread();
    }

    @Override
    public void assertOnRenderThreadOrInit() {
        RenderSystem.assertOnRenderThreadOrInit();
    }

    @Override
    public void setShaderTexture(int shaderTexture, int textureId) {
        RenderSystem.setShaderTexture(shaderTexture, textureId);
    }

    @Override
    public int getShaderTexture(int shaderTexture) {
        return RenderSystem.getShaderTexture(shaderTexture);
    }

    @Override
    public void setShaderColor(float red, float green, float blue, float alpha) {
        super.setShaderColor(red, green, blue, alpha);
        RenderSystem.setShaderColor(red, green, blue, alpha);
    }

    @Override
    public float[] getShaderColor() {
        return RenderSystem.getShaderColor();
    }

    @Override
    public float[] getShaderFogColor() {
        return RenderSystem.getShaderFogColor();
    }

    @Override
    public float getShaderFogStart() {
        return RenderSystem.getShaderFogStart();
    }

    @Override
    public float getShaderFogEnd() {
        return RenderSystem.getShaderFogEnd();
    }

    @Override
    public int getFogShape() {
        return RenderSystem.getShaderFogShape() == FogShape.CYLINDER ? 1 : 0;
    }

    @Override
    public void setShaderLineWidth(float lineWidth) {
        RenderSystem.lineWidth(lineWidth);
    }

    @Override
    public float getShaderLineWidth() {
        return RenderSystem.getShaderLineWidth();
    }

    @Override
    public Matrix4f getProjectionMatrix() {
        return RenderSystem.getProjectionMatrix();
    }

    @Override
    public void setProjectionMatrixOrth(Matrix4f projectionMatrix) {
        RenderSystem.setProjectionMatrix(projectionMatrix/*? if >=1.20 {*/, com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z/*?}*/);
    }

    @Override
    public void setProjectionMatrixOrigin(Matrix4f projectionMatrix) {
        RenderSystem.setProjectionMatrix(projectionMatrix/*? if >=1.20 {*/, com.mojang.blaze3d.vertex.VertexSorting.DISTANCE_TO_ORIGIN/*?}*/);
    }

    @Override
    public void setPositionShader() {
        RenderSystem.setShader(GameRenderer::getPositionShader);
    }

    @Override
    public void defaultBlendFunc() {
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha) {
        RenderSystem.blendFuncSeparate(srcRGB, dstRGB, srcAlpha, dstAlpha);
    }
}
