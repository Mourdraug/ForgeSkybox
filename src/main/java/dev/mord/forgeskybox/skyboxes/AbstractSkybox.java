package dev.mord.forgeskybox.skyboxes;

import com.google.common.collect.Lists;
import com.google.common.collect.Range;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import dev.mord.forgeskybox.SkyboxManager;
import dev.mord.forgeskybox.mixin.skybox.WorldRendererAccess;
import dev.mord.forgeskybox.util.Utils;
import dev.mord.forgeskybox.util.object.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * All classes that implement {@link AbstractSkybox} should
 * have a default constructor as it is required when checking
 * the type of the skybox.
 */
public abstract class AbstractSkybox {
    /**
     * The current alpha for the skybox. Expects all skyboxes extending this to accommodate this.
     * This variable is responsible for fading in/out skyboxes.
     */
    public transient float alpha;

    // ! These are the options variables.  Do not mess with these.
    protected Fade fade = Fade.ZERO;
    protected float maxAlpha = 1f;
    protected float transitionSpeed = 1;
    protected boolean changeFog = false;
    protected RGBA fogColors = RGBA.ZERO;
    protected boolean renderSunSkyColorTint = true;
    protected boolean shouldRotate = false;
    protected List<String> weather = new ArrayList<>();
    protected List<ResourceLocation> biomes = new ArrayList<>();
    protected Decorations decorations = Decorations.DEFAULT;
    /**
     * Stores identifiers of <b>worlds</b>, not dimension types.
     */
    protected List<ResourceLocation> worlds = new ArrayList<>();
    protected List<MinMaxEntry> yRanges = Lists.newArrayList();
    protected List<MinMaxEntry> zRanges = Lists.newArrayList();
    protected List<MinMaxEntry> xRanges = Lists.newArrayList();

    /**
     * The main render method for a skybox.
     * Override this if you are creating a skybox from this one.
     *
     * @param worldRendererAccess Access to the worldRenderer as skyboxes often require it.
     * @param matrices            The current MatrixStack.
     * @param tickDelta           The current tick delta.
     * @param camera              The player camera.
     * @param thickFog            Is using thick fog.
     */
    public abstract void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f matrix4f, float tickDelta, Camera camera, boolean thickFog);

    protected AbstractSkybox() {
    }

    protected AbstractSkybox(DefaultProperties properties, Conditions conditions, Decorations decorations) {
        this.fade = properties.getFade();
        this.maxAlpha = properties.getMaxAlpha();
        this.transitionSpeed = properties.getTransitionSpeed();
        this.changeFog = properties.isChangeFog();
        this.fogColors = properties.getFogColors();
        this.renderSunSkyColorTint = properties.isRenderSunSkyTint();
        this.shouldRotate = properties.isShouldRotate();
        this.weather = conditions.getWeathers().stream().map(Weather::toString).distinct().collect(Collectors.toList());
        this.biomes = conditions.getBiomes();
        this.worlds = conditions.getWorlds();
        this.yRanges = conditions.getYRanges();
        this.zRanges = conditions.getZRanges();
        this.xRanges = conditions.getXRanges();
        this.decorations = decorations;
    }

    /**
     * Calculates the alpha value for the current time and conditions and returns it.
     *
     * @return The new alpha value.
     */
    public final float updateAlpha() {
        if (!fade.isAlwaysOn()) {
            int currentTime = (int) (Objects.requireNonNull(Minecraft.getInstance().level).getDayTime() % 24000); // modulo so that it's bound to 24000
            int durationIn = Utils.getTicksBetween(this.fade.getStartFadeIn(), this.fade.getEndFadeIn());
            int durationOut = Utils.getTicksBetween(this.fade.getStartFadeOut(), this.fade.getEndFadeOut());

            int startFadeIn = this.fade.getStartFadeIn() % 24000;
            int endFadeIn = this.fade.getEndFadeIn() % 24000;

            if (endFadeIn < startFadeIn) {
                endFadeIn += 24000;
            }

            int startFadeOut = this.fade.getStartFadeOut() % 24000;
            int endFadeOut = this.fade.getEndFadeOut() % 24000;

            if (startFadeOut < endFadeIn) {
                startFadeOut += 24000;
            }

            if (endFadeOut < startFadeOut) {
                endFadeOut += 24000;
            }

            int tempInTime = currentTime;

            if (tempInTime < startFadeIn) {
                tempInTime += 24000;
            }

            int tempFullTime = currentTime;

            if (tempFullTime < endFadeIn) {
                tempFullTime += 24000;
            }

            int tempOutTime = currentTime;

            if (tempOutTime < startFadeOut) {
                tempOutTime += 24000;
            }

            float maxPossibleAlpha;

            if (startFadeIn < tempInTime && endFadeIn >= tempInTime) {
                maxPossibleAlpha = 1f - (((float) (endFadeIn - tempInTime)) / durationIn); // fading in

            } else if (endFadeIn < tempFullTime && startFadeOut >= tempFullTime) {
                maxPossibleAlpha = 1f; // fully faded in

            } else if (startFadeOut < tempOutTime && endFadeOut >= tempOutTime) {
                maxPossibleAlpha = (float) (endFadeOut - tempOutTime) / durationOut; // fading out

            } else {
                maxPossibleAlpha = 0f; // default not showing
            }

            maxPossibleAlpha *= maxAlpha;
            if (checkBiomes() && checkXRanges() && checkYRanges() && checkZRanges() && checkWeather() && checkEffect()) { // check if environment is invalid
                if (alpha >= maxPossibleAlpha) {
                    alpha = maxPossibleAlpha;
                } else {
                    alpha += transitionSpeed;
                    if (alpha > maxPossibleAlpha) alpha = maxPossibleAlpha;
                }
            } else {
                if (alpha > 0f) {
                    alpha -= transitionSpeed;
                    if (alpha < 0f) alpha = 0f;
                } else {
                    alpha = 0f;
                }
            }
        } else {
            alpha = 1f;
        }

        if (alpha > SkyboxManager.MINIMUM_ALPHA) {
            if (changeFog) {
                SkyboxManager.shouldChangeFog = true;
                SkyboxManager.fogRed = this.fogColors.getRed();
                SkyboxManager.fogBlue = this.fogColors.getBlue();
                SkyboxManager.fogGreen = this.fogColors.getGreen();
            }
            if (!renderSunSkyColorTint) {
                SkyboxManager.renderSunriseAndSet = false;
            }
        }

        // sanity checks
        if (alpha < 0f) alpha = 0f;
        if (alpha > 1f) alpha = 1f;

        return alpha;
    }

    /**
     * @return Whether the current biomes and dimensions are valid for this skybox.
     */
    protected boolean checkBiomes() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);
        Objects.requireNonNull(client.player);
        if (worlds.isEmpty()|| worlds.contains(client.level.dimension().location())) {
            return biomes.isEmpty()|| biomes.contains(client.level.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY).getKey(client.level.getBiome(client.player.blockPosition()).value()));
        }
        return false;
    }

    /*
		Check if an effect that should prevent skybox from showing
     */
    protected boolean checkEffect() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);

        Camera camera = client.gameRenderer.getMainCamera();
        boolean thickFog = DimensionSpecialEffects.forType(client.level.dimensionType()).isFoggyAt(Mth.floor(camera.getPosition().x()), Mth.floor(camera.getPosition().y())) || client.gui.getBossOverlay().shouldCreateWorldFog();
        if (thickFog)
            return false;

        FogType cameraSubmersionType = camera.getFluidInCamera();
        if (cameraSubmersionType == FogType.POWDER_SNOW.POWDER_SNOW || cameraSubmersionType == FogType.LAVA)
            return false;

        if (camera.getEntity() instanceof LivingEntity livingEntity && livingEntity.hasEffect(MobEffects.BLINDNESS))
            return false;

        return true;
    }

    /**
     * @return Whether the current x values are valid for this skybox.
     */
    protected boolean checkXRanges() {
        double playerX = Objects.requireNonNull(Minecraft.getInstance().player).getX();
        return checkCoordRanges(playerX, this.xRanges);
    }

    /**
     * @return Whether the current y values are valid for this skybox.
     */
    protected boolean checkYRanges() {
        double playerY = Objects.requireNonNull(Minecraft.getInstance().player).getY();
        return checkCoordRanges(playerY, this.yRanges);
    }

    /**
     * @return Whether the current z values are valid for this skybox.
     */
    protected boolean checkZRanges() {
        double playerZ = Objects.requireNonNull(Minecraft.getInstance().player).getZ();
        return checkCoordRanges(playerZ, this.zRanges);
    }

    /**
     * @return Whether the coordValue is within any of the minMaxEntries.
     */
    private static boolean checkCoordRanges(double coordValue, List<MinMaxEntry> minMaxEntries) {
        return minMaxEntries.isEmpty() || minMaxEntries.stream()
            .anyMatch(minMaxEntry -> Range.closedOpen(minMaxEntry.getMin(), minMaxEntry.getMax())
                .contains((float) coordValue));
    }

    /**
     * @return Whether the current weather is valid for this skybox.
     */
    protected boolean checkWeather() {
        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        Biome.Precipitation precipitation = world.getBiome(player.blockPosition()).value().getPrecipitation();
        if (weather.size() > 0) {
            if (weather.contains("thunder") && world.isThundering()) {
                return true;
            } else if (weather.contains("snow") && world.isRaining() && precipitation == Biome.Precipitation.SNOW) {
                return true;
            } else if (weather.contains("rain") && world.isRaining() && !world.isThundering()) {
                return true;
            } else return weather.contains("clear") && !world.isRaining();
        } else {
            return true;
        }
    }

    public abstract SkyboxType<? extends AbstractSkybox> getType();

    public void renderDecorations(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f matrix4f, float tickDelta, BufferBuilder bufferBuilder, float alpha) {
        if (!SkyboxManager.getInstance().hasRenderedDecorations()) {
            Vector3f rotationStatic = decorations.getRotation().getStatic();
            Vector3f rotationAxis = decorations.getRotation().getAxis();

            RenderSystem.enableTexture();
            matrices.pushPose();
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
            ClientLevel world = Minecraft.getInstance().level;
            assert world != null;
            RenderSystem.enableTexture();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationAxis.x()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationAxis.y()));
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationAxis.z()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
            matrices.mulPose(Vector3f.XP.rotationDegrees(world.getTimeOfDay(tickDelta) * 360.0F * decorations.getRotation().getRotationSpeed()));
            matrices.mulPose(Vector3f.ZN.rotationDegrees(rotationAxis.z()));
            matrices.mulPose(Vector3f.YN.rotationDegrees(rotationAxis.y()));
            matrices.mulPose(Vector3f.XN.rotationDegrees(rotationAxis.x()));
            // sun
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
            Matrix4f matrix4f2 = matrices.last().pose();
            float s = 30.0F;
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            if (decorations.isSunEnabled()) {
                RenderSystem.setShaderTexture(0, this.decorations.getSunTexture());
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix4f2, -s, 100.0F, -s).uv(0.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, s, 100.0F, -s).uv(1.0F, 0.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, s, 100.0F, s).uv(1.0F, 1.0F).endVertex();
                bufferBuilder.vertex(matrix4f2, -s, 100.0F, s).uv(0.0F, 1.0F).endVertex();
                bufferBuilder.end();
                BufferUploader.end(bufferBuilder);
            }
            // moon
            s = 20.0F;
            if (decorations.isMoonEnabled()) {
                RenderSystem.setShaderTexture(0, this.decorations.getMoonTexture());
                int u = world.getMoonPhase();
                int v = u % 4;
                int w = u / 4 % 2;
                float x = v / 4.0F;
                float p = w / 2.0F;
                float q = (v + 1) / 4.0F;
                float r = (w + 1) / 2.0F;
                bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                bufferBuilder.vertex(matrix4f2, -s, -100.0F, s).uv(q, r).endVertex();
                bufferBuilder.vertex(matrix4f2, s, -100.0F, s).uv(x, r).endVertex();
                bufferBuilder.vertex(matrix4f2, s, -100.0F, -s).uv(x, p).endVertex();
                bufferBuilder.vertex(matrix4f2, -s, -100.0F, -s).uv(q, p).endVertex();
                bufferBuilder.end();
                BufferUploader.end(bufferBuilder);
            }
            // stars
            if (decorations.isStarsEnabled()) {
                RenderSystem.disableTexture();
                float ab = world.getStarBrightness(tickDelta) * s;
                if (ab > 0.0F) {
                    RenderSystem.setShaderColor(ab, ab, ab, ab);
                    worldRendererAccess.getStarsBuffer().drawWithShader(matrices.last().pose(), matrix4f, GameRenderer.getPositionShader());
                }
            }
            matrices.mulPose(Vector3f.ZP.rotationDegrees(rotationStatic.z()));
            matrices.mulPose(Vector3f.YP.rotationDegrees(rotationStatic.y()));
            matrices.mulPose(Vector3f.XP.rotationDegrees(rotationStatic.x()));
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
            matrices.popPose();
        }
    }

    public Fade getFade() {
        return this.fade;
    }

    public float getMaxAlpha() {
        return this.maxAlpha;
    }

    public float getTransitionSpeed() {
        return this.transitionSpeed;
    }

    public boolean isChangeFog() {
        return this.changeFog;
    }

    public RGBA getFogColors() {
        return this.fogColors;
    }

    public boolean isRenderSunSkyColorTint() {
        return this.renderSunSkyColorTint;
    }

    public boolean isShouldRotate() {
        return this.shouldRotate;
    }

    public Decorations getDecorations() {
        return this.decorations;
    }

    public List<String> getWeather() {
        return this.weather;
    }

    public List<ResourceLocation> getBiomes() {
        return this.biomes;
    }

    public List<ResourceLocation> getWorlds() {
        return this.worlds;
    }

    public DefaultProperties getDefaultProperties() {
        return DefaultProperties.ofSkybox(this);
    }

    public Conditions getConditions() {
        return Conditions.ofSkybox(this);
    }

    public List<MinMaxEntry> getXRanges() {
        return this.xRanges;
    }

    public List<MinMaxEntry> getYRanges() {
        return this.yRanges;
    }

    public List<MinMaxEntry> getZRanges() {
        return this.zRanges;
    }
}
