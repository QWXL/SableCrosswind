package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.network.WindMotionClientHandler;
import cn.chumenta.crosswind.registry.CrosswindRegistry;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.TooltipModifier;
import net.createmod.catnip.lang.FontHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Crosswind.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrosswindClient {
    private CrosswindClient() {
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(
                cn.chumenta.crosswind.registry.CrosswindRegistry.WIND_STREAK.get(),
                WindStreakParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        WindMotionClientHandler.register();

        RotorModels.bootstrap();

        WindStreakSpawnerTick.register();

        TooltipModifier.REGISTRY.register(CrosswindRegistry.RAT_ITEM.get(),
                new ItemDescription.Modifier(CrosswindRegistry.RAT_ITEM.get(),
                        FontHelper.Palette.STANDARD_CREATE));
    }
}
