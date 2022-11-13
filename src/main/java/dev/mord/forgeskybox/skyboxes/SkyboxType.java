package dev.mord.forgeskybox.skyboxes;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import dev.mord.forgeskybox.FabricSkyBoxesClient;
import dev.mord.forgeskybox.skyboxes.textured.AnimatedSquareTexturedSkybox;
import dev.mord.forgeskybox.skyboxes.textured.SingleSpriteAnimatedSquareTexturedSkybox;
import dev.mord.forgeskybox.skyboxes.textured.SingleSpriteSquareTexturedSkybox;
import dev.mord.forgeskybox.skyboxes.textured.SquareTexturedSkybox;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = FabricSkyBoxesClient.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SkyboxType<T extends AbstractSkybox> extends ForgeRegistryEntry<SkyboxType<? extends AbstractSkybox>> {
    public static Supplier<IForgeRegistry<SkyboxType<? extends AbstractSkybox>>> REGISTRY_SUPPLIER;
    public static SkyboxType<MonoColorSkybox> MONO_COLOR_SKYBOX;
    public static SkyboxType<SquareTexturedSkybox> SQUARE_TEXTURED_SKYBOX;
    public static SkyboxType<SingleSpriteSquareTexturedSkybox> SINGLE_SPRITE_SQUARE_TEXTURED_SKYBOX;
    public static SkyboxType<AnimatedSquareTexturedSkybox> ANIMATED_SQUARE_TEXTURED_SKYBOX;
    public static SkyboxType<SingleSpriteAnimatedSquareTexturedSkybox> SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED_SKYBOX;
    public static final Codec<ResourceLocation> SKYBOX_ID_CODEC;


    private final BiMap<Integer, Codec<T>> codecBiMap;
    private final boolean legacySupported;
    private final String name;
    @Nullable
    private final Supplier<T> factory;
    @Nullable
    private final LegacyDeserializer<T> deserializer;

    private SkyboxType(BiMap<Integer, Codec<T>> codecBiMap, boolean legacySupported, String name, @Nullable Supplier<T> factory, @Nullable LegacyDeserializer<T> deserializer) {
        this.codecBiMap = codecBiMap;
        this.legacySupported = legacySupported;
        this.name = name;
        this.factory = factory;
        this.deserializer = deserializer;
    }

    public String getName() {
        return this.name;
    }

    public boolean isLegacySupported() {
        return this.legacySupported;
    }

    @NotNull
    public T instantiate() {
        return Objects.requireNonNull(Objects.requireNonNull(this.factory, "Can't instantiate from a null factory").get());
    }

    @Nullable
    public LegacyDeserializer<T> getDeserializer() {
        return this.deserializer;
    }

    public ResourceLocation createId(String namespace) {
        return this.createIdFactory().apply(namespace);
    }

    public Function<String, ResourceLocation> createIdFactory() {
        return (ns) -> new ResourceLocation(ns, this.getName().replace('-', '_'));
    }

    public Codec<T> getCodec(int schemaVersion) {
        return Objects.requireNonNull(this.codecBiMap.get(schemaVersion), String.format("Unsupported schema version '%d' for skybox type %s", schemaVersion, this.name));
    }

    private static <T extends AbstractSkybox> SkyboxType<T> register(SkyboxType<T> type) {
        type.setRegistryName(type.createId(FabricSkyBoxesClient.MODID));
        REGISTRY_SUPPLIER.get().register(type);
        return type;
    }

    static {
        SKYBOX_ID_CODEC = Codec.STRING.xmap((s) -> {
            if (!s.contains(":")) {
                return new ResourceLocation(FabricSkyBoxesClient.MODID, s.replace('-', '_'));
            }
            return new ResourceLocation(s.replace('-', '_'));
        }, (id) -> {
            if (id.getNamespace().equals(FabricSkyBoxesClient.MODID)) {
                return id.getPath().replace('_', '-');
            }
            return id.toString().replace('_', '-');
        });
    }

    private static <T> Class<T> c(Class<?> cls) {
        return (Class<T>) cls;
    }

    @SubscribeEvent
    public static void onNewRegistry(NewRegistryEvent event) {
        RegistryBuilder<SkyboxType<? extends AbstractSkybox>> builder = new RegistryBuilder<>();
        builder.setName(new ResourceLocation(FabricSkyBoxesClient.MODID, "skybox_type"));
        builder.setType(c(SkyboxType.class));
        REGISTRY_SUPPLIER = event.create(builder);
    }

    @SubscribeEvent
    public static void registerSkyboxes(RegistryEvent.Register<SkyboxType<? extends  AbstractSkybox>> event) {
        MONO_COLOR_SKYBOX = register(SkyboxType.Builder.create(MonoColorSkybox.class, "monocolor").legacySupported().deserializer(LegacyDeserializer.MONO_COLOR_SKYBOX_DESERIALIZER).factory(MonoColorSkybox::new).add(2, MonoColorSkybox.CODEC).build());
        SQUARE_TEXTURED_SKYBOX = register(SkyboxType.Builder.create(SquareTexturedSkybox.class, "square-textured").deserializer(LegacyDeserializer.SQUARE_TEXTURED_SKYBOX_DESERIALIZER).legacySupported().factory(SquareTexturedSkybox::new).add(2, SquareTexturedSkybox.CODEC).build());
        SINGLE_SPRITE_SQUARE_TEXTURED_SKYBOX = register(SkyboxType.Builder.create(SingleSpriteSquareTexturedSkybox.class, "single-sprite-square-textured").add(2, SingleSpriteSquareTexturedSkybox.CODEC).build());
        ANIMATED_SQUARE_TEXTURED_SKYBOX = register(SkyboxType.Builder.create(AnimatedSquareTexturedSkybox.class, "animated-square-textured").add(2, AnimatedSquareTexturedSkybox.CODEC).build());
        SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED_SKYBOX = register(SkyboxType.Builder.create(SingleSpriteAnimatedSquareTexturedSkybox.class, "single-sprite-animated-square-textured").add(2, SingleSpriteAnimatedSquareTexturedSkybox.CODEC).build());
    }

    public static class Builder<T extends AbstractSkybox> {
        private String name;
        private final ImmutableBiMap.Builder<Integer, Codec<T>> builder = ImmutableBiMap.builder();
        private boolean legacySupported = false;
        private Supplier<T> factory;
        private LegacyDeserializer<T> deserializer;

        private Builder() {
        }

        public static <S extends AbstractSkybox> Builder<S> create(@SuppressWarnings("unused") Class<S> clazz, String name) {
            Builder<S> builder = new Builder<>();
            builder.name = name;
            return builder;
        }

        public static <S extends AbstractSkybox> Builder<S> create(String name) {
            Builder<S> builder = new Builder<>();
            builder.name = name;
            return builder;
        }

        protected Builder<T> legacySupported() {
            this.legacySupported = true;
            return this;
        }

        protected Builder<T> factory(Supplier<T> factory) {
            this.factory = factory;
            return this;
        }

        protected Builder<T> deserializer(LegacyDeserializer<T> deserializer) {
            this.deserializer = deserializer;
            return this;
        }

        public Builder<T> add(int schemaVersion, Codec<T> codec) {
            Preconditions.checkArgument(schemaVersion >= 2, "schema version was lesser than 2");
            Preconditions.checkNotNull(codec, "codec was null");
            this.builder.put(schemaVersion, codec);
            return this;
        }

        public SkyboxType<T> build() {
            if (this.legacySupported) {
                Preconditions.checkNotNull(this.factory, "factory was null");
                Preconditions.checkNotNull(this.deserializer, "deserializer was null");
            }
            return new SkyboxType<>(this.builder.build(), this.legacySupported, this.name, this.factory, this.deserializer);
        }

        public SkyboxType<T> buildAndRegister(String namespace) {
            SkyboxType<T> type = build();
            type.setRegistryName(new ResourceLocation(namespace, this.name.replace('-', '_')));
            SkyboxType.REGISTRY_SUPPLIER.get().register(type);
            return type;
        }
    }
}
