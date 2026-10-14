package cn.chumenta.crosswind.client.ponder;

import cn.chumenta.crosswind.Crosswind;
import net.createmod.ponder.foundation.PonderIndex;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Crosswind.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CrosswindPonder {
    private CrosswindPonder() {
    }

    @SubscribeEvent
    public static void onClientSetup(final FMLClientSetupEvent event) {
        PonderIndex.addPlugin(new CrosswindPonderPlugin());
    }
}
