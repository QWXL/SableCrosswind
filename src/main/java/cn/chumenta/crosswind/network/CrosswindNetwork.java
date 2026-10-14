package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.Crosswind;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = Crosswind.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CrosswindNetwork {
    private CrosswindNetwork() {
    }

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1");

        registrar.playToClient(
                WindMotionPayload.TYPE,
                WindMotionPayload.CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist.isClient()) {
                        WindMotionClientHandler.handle(payload, context);
                    }
                });

        registrar.playToClient(
                WindInstrumentPayload.TYPE,
                WindInstrumentPayload.CODEC,
                (payload, context) -> {
                    if (FMLEnvironment.dist.isClient()) {
                        WindInstrumentClientHandler.handle(payload);
                    }
                });

        Crosswind.LOGGER.info("已注册网络包 crosswind:wind_motion, crosswind:instrument_readout");
    }
}
