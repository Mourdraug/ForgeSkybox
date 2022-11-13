package dev.mord.forgeskybox;

import dev.mord.forgeskybox.resource.SkyboxResourceListener;

import dev.mord.forgeskybox.skyboxes.AbstractSkybox;
import dev.mord.forgeskybox.skyboxes.LegacyDeserializer;
import dev.mord.forgeskybox.skyboxes.SkyboxType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.NewRegistryEvent;
import net.minecraftforge.registries.RegistryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("forgeskybox")
public class FabricSkyBoxesClient {
    public static final String MODID = "forgeskybox";
    private static Logger LOGGER;

    public static Logger getLogger() {
        if (LOGGER == null) {
            LOGGER = LogManager.getLogger("ForgeSkybox");
        }
        return LOGGER;
    }
}
