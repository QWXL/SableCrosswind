package cn.chumenta.crosswind.compat.sable;

import cn.chumenta.crosswind.Crosswind;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class SubLevelTracker {
    private SubLevelTracker() {
    }

    @Nullable
    private static volatile UUID tracked;

    private static long lastLoggedTick = Long.MIN_VALUE;


    public static void track(final UUID id) {
        tracked = id;
        lastLoggedTick = Long.MIN_VALUE;
        Crosswind.LOGGER.info("开始追踪子世界 {}", id);
    }

    public static void untrack() {
        if (tracked != null) {
            Crosswind.LOGGER.info("停止追踪子世界 {}", tracked);
        }
        tracked = null;
    }

    @Nullable
    public static UUID tracked() {
        return tracked;
    }


    public static boolean shouldLog(final UUID id, final long gameTime) {
        final UUID t = tracked;
        if (t == null || !t.equals(id)) {
            return false;
        }
        if (gameTime == lastLoggedTick) {
            return false;
        }
        lastLoggedTick = gameTime;
        return true;
    }
}
