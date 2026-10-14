package cn.chumenta.crosswind;

import cn.chumenta.crosswind.wind.WindFieldService;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Crosswind.MOD_ID)
public final class Crosswind {
    public static final String MOD_ID = "crosswind";

    public static net.minecraft.resources.ResourceLocation id(final String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final Logger LOGGER = LoggerFactory.getLogger("Crosswind");

    private static final WindFieldService WIND_SERVICE = new WindFieldService();

    public static WindFieldService windService() {
        return WIND_SERVICE;
    }

    public Crosswind(final IEventBus modBus, final ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, CrosswindConfig.SPEC);

        container.registerConfig(ModConfig.Type.CLIENT, cn.chumenta.crosswind.CrosswindClientConfig.SPEC);

        cn.chumenta.crosswind.registry.CrosswindRegistry.register(modBus);
        cn.chumenta.crosswind.registry.CrosswindRegistry.registerParticles(modBus);

        cn.chumenta.crosswind.compat.sable.SableWindForceGroup.register(modBus);

        cn.chumenta.crosswind.compat.sable.SableWindProvider.register();

        cn.chumenta.crosswind.entity.EntityWindHandler.register();

        cn.chumenta.crosswind.advancement.CrosswindTriggers.register(modBus);

        LOGGER.info("Sable: Crosswind Loaded");
    }
}
