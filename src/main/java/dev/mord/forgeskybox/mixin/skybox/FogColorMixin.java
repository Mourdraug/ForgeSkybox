package dev.mord.forgeskybox.mixin.skybox;

import dev.mord.forgeskybox.SkyboxManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogColorMixin {

    @Shadow
    private static float fogRed;

    @Shadow
    private static float fogBlue;

    @Shadow
    private static float fogGreen;

    /**
     * Checks if we should change the fog color to whatever the skybox set it to, and sets it.
     */
    //@Inject(method = "setupColor", at = @At(value = "FIELD", target = "biomeC", ordinal = 5))

    @Inject(method = "setupColor", at = @At("HEAD"), cancellable = true)
    private static void modifyColors(Camera pActiveRenderInfo, float pPartialTicks, ClientLevel pLevel, int pRenderDistanceChunks, float pBossColorModifier, CallbackInfo ci) {
        if (SkyboxManager.shouldChangeFog)
        {
            fogRed = SkyboxManager.fogRed;
            fogBlue = SkyboxManager.fogBlue;
            fogGreen = SkyboxManager.fogGreen;
            SkyboxManager.shouldChangeFog = false;
            ci.cancel();
        }
    }
}
