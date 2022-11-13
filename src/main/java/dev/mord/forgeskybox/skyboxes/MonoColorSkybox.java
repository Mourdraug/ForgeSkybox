package dev.mord.forgeskybox.skyboxes;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mord.forgeskybox.mixin.skybox.WorldRendererAccess;
import dev.mord.forgeskybox.util.object.RGBA;
import dev.mord.forgeskybox.util.object.Conditions;
import dev.mord.forgeskybox.util.object.Decorations;
import dev.mord.forgeskybox.util.object.DefaultProperties;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;

import java.util.Objects;

public class MonoColorSkybox extends AbstractSkybox {
    public static Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultProperties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getDefaultProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.NO_CONDITIONS).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            RGBA.CODEC.optionalFieldOf("color", RGBA.ZERO).forGetter(MonoColorSkybox::getColor)
    ).apply(instance, MonoColorSkybox::new));
    public RGBA color;

    public MonoColorSkybox() {
    }

    public MonoColorSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, RGBA color) {
        super(properties, conditions, decorations);
        this.color = color;
    }

    @Override
    public SkyboxType<? extends AbstractSkybox> getType() {
        return SkyboxType.MONO_COLOR_SKYBOX;
    }

    @Override
    public void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f matrix4f, float tickDelta, Camera camera, boolean thickFog) {
        if (this.alpha > 0) {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            Minecraft client = Minecraft.getInstance();
            ClientLevel world = Objects.requireNonNull(client.level);
            RenderSystem.disableTexture();
            FogRenderer.levelFogColor();
            BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
            RenderSystem.depthMask(false);
            RenderSystem.setShaderColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), 1.0F);
            worldRendererAccess.getLightSkyBuffer().bind();
            worldRendererAccess.getLightSkyBuffer().draw();
            VertexBuffer.unbind();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float[] skyColor = DimensionSpecialEffects.forType(world.dimensionType()).getSunriseColor(world.getTimeOfDay(tickDelta), tickDelta);
            float skySide;
            float skyColorGreen;
            float o;
            float p;
            float q;
            if (skyColor != null) {
                RenderSystem.disableTexture();
                matrices.pushPose();
                matrices.mulPose(Vector3f.XP.rotationDegrees(90.0F));
                skySide = Mth.sin(world.getSunAngle(tickDelta)) < 0.0F ? 180.0F : 0.0F;
                matrices.mulPose(Vector3f.ZP.rotationDegrees(skySide));
                matrices.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
                float skyColorRed = skyColor[0];
                skyColorGreen = skyColor[1];
                float skyColorBlue = skyColor[2];
                Matrix4f matrix4f2 = matrices.last().pose();
                bufferBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
                bufferBuilder.vertex(matrix4f2, 0.0F, 100.0F, 0.0F).color(skyColorRed, skyColorGreen, skyColorBlue, skyColor[3]).endVertex();

                for (int n = 0; n <= 16; ++n) {
                    o = (float) n * 6.2831855F / 16.0F;
                    p = Mth.sin(o);
                    q = Mth.cos(o);
                    bufferBuilder.vertex(matrix4f2, p * 120.0F, q * 120.0F, -q * 40.0F * skyColor[3]).color(skyColor[0], skyColor[1], skyColor[2], 0.0F).endVertex();
                }

                bufferBuilder.end();
                BufferUploader.end(bufferBuilder);
                matrices.popPose();
            }

            this.renderDecorations(worldRendererAccess, matrices, matrix4f, tickDelta, bufferBuilder, this.alpha);

            RenderSystem.disableTexture();
            RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
            //noinspection ConstantConditions
            double d = client.player.getEyePosition(tickDelta).y - world.getLevelData().getHorizonHeight(world);
            if (d < 0.0D) {
                matrices.pushPose();
                matrices.translate(0.0D, 12.0D, 0.0D);
                matrices.popPose();
            }

            if (world.effects().hasGround()) {
                RenderSystem.setShaderColor(this.color.getRed() * 0.2F + 0.04F, this.color.getBlue() * 0.2F + 0.04F, this.color.getGreen() * 0.6F + 0.1F, 1.0F);
            } else {
                RenderSystem.setShaderColor(this.color.getRed(), this.color.getBlue(), this.color.getGreen(), 1.0F);
            }

            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
        }
    }

    public RGBA getColor() {
        return this.color;
    }
}
