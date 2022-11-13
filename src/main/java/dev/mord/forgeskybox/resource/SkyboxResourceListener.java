package dev.mord.forgeskybox.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.mord.forgeskybox.FabricSkyBoxesClient;
import dev.mord.forgeskybox.SkyboxManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

@Mod.EventBusSubscriber(modid = FabricSkyBoxesClient.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SkyboxResourceListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();

    @SubscribeEvent
    public static void onResourceReload(AddReloadListenerEvent event){
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        SkyboxManager skyboxManager = SkyboxManager.getInstance();

        // clear registered skyboxes on reload
        skyboxManager.clearSkyboxes();

        // load new skyboxes
        Collection<ResourceLocation> resources = manager.listResources("sky", string -> string.endsWith(".json"));

        for (ResourceLocation id : resources) {
            try {
                Resource resource = manager.getResource(id);
                JsonObject json = GSON.fromJson(new InputStreamReader(resource.getInputStream()), JsonObject.class);
                skyboxManager.addSkybox(id, json);
            } catch (IOException e) {
                FabricSkyBoxesClient.getLogger().error("Error reading skybox " + id.toString());
                e.printStackTrace();
            }
        }
    }
}
