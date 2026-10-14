package cn.chumenta.crosswind.compat.sable;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.wind.WindFieldService;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import dev.ryanhcode.sable.api.SubLevelHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SableWindProvider {
    private SableWindProvider() {
    }

    private static boolean registered;

    private static WindSettings cachedSettings;
    private static long cachedTick = Long.MIN_VALUE;

    private static final Vector3d OUT = new Vector3d();

    private static final Vector3d ZERO = new Vector3d();

    public static synchronized void register() {
        if (registered) {
            return;
        }
        try {
            SubLevelHelper.registerWindProvider(SableWindProvider::windAt);
            registered = true;
            Crosswind.LOGGER.info(
                    "已向 Sable 注册风场 provider");
        } catch (final Throwable t) {
            Crosswind.LOGGER.warn("向 Sable 注册风场 provider 失败，将只使用独立施力", t);
        }
    }

    private static Vector3dc windAt(final Vector3dc position, final Level level) {
        try {
            if (!(level instanceof final ServerLevel serverLevel)) {
                return ZERO;
            }

            final long tick = serverLevel.getGameTime();
            WindSettings settings = cachedSettings;
            if (settings == null || cachedTick != tick) {
                settings = WindSettings.fromConfig();
                cachedSettings = settings;
                cachedTick = tick;
            }

            if (!settings.enabled() || !settings.sableEnabled()) {
                return ZERO;
            }

            final WindFieldService service = Crosswind.windService();
            final WindSample sample = service.sample(serverLevel, position.x(), position.y(), position.z(), settings);
            if (sample.isCalm()) {
                return ZERO;
            }
            return OUT.set(sample.velocity().x, sample.velocity().y, sample.velocity().z);
        } catch (final Throwable t) {
            return ZERO;
        }
    }
}
