package cn.chumenta.crosswind.wind;

import cn.chumenta.crosswind.Crosswind;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@EventBusSubscriber(modid = Crosswind.MOD_ID)
public final class WindSeedRotation {
    private WindSeedRotation() {
    }

    private static final long DAY_LENGTH = 24000L;

    private static final Map<ResourceKey<Level>, Long> LAST_DAY = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (!cn.chumenta.crosswind.CrosswindConfig.SEED_AUTO_ROTATE.get()) {
            return;
        }
        final MinecraftServer server = event.getServer();
        for (final ServerLevel level : server.getAllLevels()) {
            tickLevel(level);
        }
    }

    private static void tickLevel(final ServerLevel level) {
        final long day = level.getDayTime() / DAY_LENGTH;
        final Long lastDay = LAST_DAY.put(level.dimension(), day);

        if (lastDay == null || day <= lastDay) {
            return;
        }

        final WeatherSeedState state = WeatherSeedState.get(level);
        final long oldSeed = state.seed();

        final long newSeed = new Random().nextLong();
        state.setSeed(newSeed, level.getGameTime(), transitionTicks());
        Crosswind.LOGGER.info(
                "[crosswind] 刷新 {} 的风场种子（过渡 {} tick）",
                level.dimension().location(), transitionTicks());
        if (Crosswind.LOGGER.isDebugEnabled()) {
            Crosswind.LOGGER.debug("[crosswind] 风场种子 {} → {}", oldSeed, newSeed);
        }
    }

    private static int transitionTicks() {
        return Math.max(0, cn.chumenta.crosswind.CrosswindConfig.SEED_TRANSITION_TICKS.get());
    }
}
