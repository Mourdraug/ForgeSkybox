package dev.mord.forgeskybox.mixin.skybox;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface WorldRendererAccess {
    @Accessor("skyBuffer")
    VertexBuffer getLightSkyBuffer();

    @Accessor("starBuffer")
    VertexBuffer getStarsBuffer();

    @Deprecated
    @Accessor("SUN_LOCATION")
    static ResourceLocation getSun() {
        throw new AssertionError();
    }

    @Deprecated
    @Accessor("MOON_LOCATION")
    static ResourceLocation getMoonPhases(){
        throw new AssertionError();
    }
}
