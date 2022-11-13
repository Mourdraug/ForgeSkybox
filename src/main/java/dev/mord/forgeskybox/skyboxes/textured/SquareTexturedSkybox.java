package dev.mord.forgeskybox.skyboxes.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.mord.forgeskybox.util.object.*;
import dev.mord.forgeskybox.mixin.skybox.WorldRendererAccess;
import dev.mord.forgeskybox.skyboxes.AbstractSkybox;
import dev.mord.forgeskybox.skyboxes.SkyboxType;
import net.minecraft.client.Camera;

public class SquareTexturedSkybox extends TexturedSkybox {
    public static Codec<SquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            DefaultProperties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getDefaultProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.NO_CONDITIONS).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
            Textures.CODEC.fieldOf("textures").forGetter(s -> s.textures)
    ).apply(instance, SquareTexturedSkybox::new));
    public Textures textures;

    public SquareTexturedSkybox() {
    }

    public SquareTexturedSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations, Blend blend, Textures textures) {
        super(properties, conditions, decorations, blend);
        this.textures = textures;
    }

    @Override
    public SkyboxType<? extends AbstractSkybox> getType() {
        return SkyboxType.SQUARE_TEXTURED_SKYBOX;
    }

    @Override
    public void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, Camera camera, boolean thickFog) {
        Tesselator tessellator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuilder();

        for (int i = 0; i < 6; ++i) {
            // 0 = bottom
            // 1 = north
            // 2 = south
            // 3 = top
            // 4 = east
            // 5 = west
            Texture tex = this.textures.byId(i);
            matrices.pushPose();

            RenderSystem.setShaderTexture(0, tex.getTextureId());

            if (i == 1) {
                matrices.mulPose(Vector3f.XP.rotationDegrees(90.0F));
            } else if (i == 2) {
                matrices.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
                matrices.mulPose(Vector3f.YP.rotationDegrees(180.0F));
            } else if (i == 3) {
                matrices.mulPose(Vector3f.XP.rotationDegrees(180.0F));
            } else if (i == 4) {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
                matrices.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
            } else if (i == 5) {
                matrices.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
                matrices.mulPose(Vector3f.YP.rotationDegrees(90.0F));
            }

            Matrix4f matrix4f = matrices.last().pose();
            bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(tex.getMinU(), tex.getMinV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(tex.getMinU(), tex.getMaxV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(tex.getMaxU(), tex.getMaxV()).color(1f, 1f, 1f, alpha).endVertex();
            bufferBuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(tex.getMaxU(), tex.getMinV()).color(1f, 1f, 1f, alpha).endVertex();
            tessellator.end();
            matrices.popPose();
        }
    }
}
