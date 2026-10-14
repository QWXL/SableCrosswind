package cn.chumenta.crosswind.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@OnlyIn(Dist.CLIENT)
final class WindStreakSpawnerTick {
    private WindStreakSpawnerTick() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> WindStreakSpawner.tick());
    }
}
