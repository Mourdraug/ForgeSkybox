package dev.mord.forgeskybox.skyboxes.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.mord.forgeskybox.util.object.*;
import dev.mord.forgeskybox.mixin.skybox.WorldRendererAccess;
import dev.mord.forgeskybox.skyboxes.AbstractSkybox;
import dev.mord.forgeskybox.skyboxes.RotatableSkybox;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements RotatableSkybox {
    public Rotation rotation;
    public Blend blend;

    protected TexturedSkybox() {
    }

    protected TexturedSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, Blend blend) {
        super(properties, conditions, decorations);
        this.blend = blend;
        this.rotation = properties.getRotation();
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param worldRendererAccess Access to the worldRenderer as skyboxes often require it.
     * @param matrices            The current MatrixStack.
     * @param tickDelta           The current tick delta.
     */
    @Override
    public final void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f matrix4f, float tickDelta, Camera camera, boolean thickFog) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        blend.applyBlendFunc(this.alpha);

        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);

        Vector3f rotationStatic = this.rotation.getStatic();

        matrices.pushPose();
        float timeRotation = this.shouldRotate ? ((float) world.getDayTime() / 24000) * 360 : 0;
        this.applyTimeRotation(matrices, timeRotation);
        matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
        this.renderSkybox(worldRendererAccess, matrices, tickDelta, camera, thickFog);
        matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
        matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
        matrices.popPose();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();

        this.renderDecorations(worldRendererAccess, matrices, matrix4f, tickDelta, bufferBuilder, this.alpha);

        RenderSystem.depthMask(true);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, Camera camera, boolean thickFog);

    private void applyTimeRotation(PoseStack matrices, float timeRotation) {
        // Very ugly, find a better way to do this
        Vector3f timeRotationAxis = this.rotation.getAxis();
        matrices.mulPose(Vector3f.XP.rotationDegrees(timeRotationAxis.x()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Vector3f.ZP.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Vector3f.YP.rotationDegrees(timeRotation * rotation.getRotationSpeed()));
        matrices.mulPose(Vector3f.ZN.rotationDegrees(timeRotationAxis.z()));
        matrices.mulPose(Vector3f.YN.rotationDegrees(timeRotationAxis.y()));
        matrices.mulPose(Vector3f.XN.rotationDegrees(timeRotationAxis.x()));
    }

    public Blend getBlend() {
        return this.blend;
    }

    public Rotation getRotation() {
        return this.rotation;
    }
}
