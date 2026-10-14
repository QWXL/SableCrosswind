package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.registry.CrosswindRegistry;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WindStreakSpawner {
    private WindStreakSpawner() {
    }

    private static double barLengthBlocks() {
        final double base = cn.chumenta.crosswind.CrosswindClientConfig
                .WIND_PARTICLE_LENGTH_BLOCKS.get();

        return base * (0.75D + 0.5D * java.util.concurrent.ThreadLocalRandom.current().nextDouble());
    }

    private static final int MAX_DOTS_PER_BAR = 48;

    private static double dotSpacing() {
        return cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_DOT_SPACING.get();
    }

    private static final double JITTER = 0.06D;

    private static double minDistance() {
        return cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_MIN_DISTANCE.get();
    }

    private static double maxDistance() {
        return Math.max(minDistance() + 1.0D,
                cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_MAX_DISTANCE.get());
    }

    private static final double HALF_HEIGHT = 7.0D;

    private static double spreadDegrees() {
        return cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_SPREAD_DEG.get();
    }

    private static final int MAX_SWEEP_DOTS_PER_TICK = 6;

    private static final java.util.ArrayDeque<double[]> PENDING =
            new java.util.ArrayDeque<>();
    private static final int PENDING_LIMIT = 512;

    private static final double FULL_RATE_SPEED = 30.0D;

    private static double maxBarsPerTick() {
        return cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_DENSITY.get();
    }

    private static double pending;

    private static int consecutiveFailures;

    private static boolean particlesDisabledByFailure;


    public static void tick() {
        final Minecraft mc = Minecraft.getInstance();
        final LocalPlayer player = mc.player;
        final ClientLevel level = mc.level;
        if (player == null || level == null || mc.isPaused()) {
            return;
        }
        if (particlesDisabledByFailure
                || !cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLES_ENABLED.get()) {
            return;
        }

        if (!flushPending(level)) {
            return;
        }

        final double threshold = cn.chumenta.crosswind.CrosswindClientConfig.WIND_PARTICLE_THRESHOLD.get();
        final WindSample sample = cn.chumenta.crosswind.Crosswind
                .windService().sample(level, player.getX(), player.getY(), player.getZ(),
                        WindSettings.fromConfig());
        final double speed = sample.speed();
        if (speed <= threshold) {
            pending = 0.0D;
            return;
        }

        final double span = Math.max(1.0E-6D, FULL_RATE_SPEED - threshold);
        final double normalized = Mth.clamp((speed - threshold) / span, 0.0D, 1.0D);
        final double rate = normalized * maxBarsPerTick();
        pending += rate;
        int bars = (int) Math.floor(pending);
        pending -= bars;
        if (bars <= 0) {
            return;
        }

        final Vec3 wind = sample.velocity();

        final double hLen = Math.hypot(wind.x, wind.z);
        final double wx;
        final double wz;
        if (hLen < 1.0E-6D) {
            final Vec3 look0 = player.getLookAngle();
            final double look0Len = Math.hypot(look0.x, look0.z);
            if (look0Len < 1.0E-6D) {
                wx = 1.0D;
                wz = 0.0D;
            } else {
                wx = look0.x / look0Len;
                wz = look0.z / look0Len;
            }
        } else {
            wx = wind.x / hLen;
            wz = wind.z / hLen;
        }

        final Vec3 look = player.getLookAngle();
        final double lookLen = Math.hypot(look.x, look.z);
        final double lookAngle = lookLen < 1.0E-6D
                ? Math.atan2(wz, wx)
                : Math.atan2(look.z, look.x);

        while (bars-- > 0) {
            if (!enqueueBar(level, player, wx, wz, lookAngle)) {
                return;
            }
        }
    }

    private static boolean enqueueBar(final ClientLevel level, final LocalPlayer player,
                                    final double wx, final double wz, final double lookAngle) {
        final double spread = Math.toRadians(spreadDegrees());
        final double angle = lookAngle + (level.random.nextDouble() - 0.5D) * 2.0D * spread;
        final double dirX = Math.cos(angle);
        final double dirZ = Math.sin(angle);
        final double minD = minDistance();
        final double distance = minD + (maxDistance() - minD) * level.random.nextDouble();

        final double spacing = dotSpacing();

        final double length = barLengthBlocks();
        final int dots = Math.max(2, Math.min(MAX_DOTS_PER_BAR,
                (int) Math.round(length / spacing) + 1));
        final double halfSpan = spacing * (dots - 1) / 2.0D;
        final double baseX = player.getX() + dirX * distance - wx * halfSpan;
        final double baseZ = player.getZ() + dirZ * distance - wz * halfSpan;
        final double baseY = player.getY()
                + (level.random.nextDouble() - 0.5D) * 2.0D * HALF_HEIGHT;

        final double barY = baseY + (level.random.nextDouble() - 0.5D) * JITTER;

        for (int i = 0; i < dots; i++) {
            final double along = i * spacing;

            final double jx = (level.random.nextDouble() - 0.5D) * 2.0D * JITTER;
            final double jz = (level.random.nextDouble() - 0.5D) * 2.0D * JITTER;
            final double x = baseX + wx * along + jx;
            final double z = baseZ + wz * along + jz;
            final double y = barY;

            if (PENDING.size() >= PENDING_LIMIT) {
                PENDING.pollFirst();
            }
            PENDING.addLast(new double[]{x, y, z});
        }
        return true;
    }

    private static boolean flushPending(final ClientLevel level) {
        if (PENDING.isEmpty()) {
            return true;
        }

        final int sweepTicks = Math.max(1, cn.chumenta.crosswind.CrosswindClientConfig
                .WIND_PARTICLE_SWEEP_TICKS.get());
        final int perTick = Math.max(1, Math.min(MAX_SWEEP_DOTS_PER_TICK,
                (PENDING.size() + sweepTicks - 1) / sweepTicks));

        for (int i = 0; i < perTick && !PENDING.isEmpty(); i++) {
            final double[] p = PENDING.pollFirst();
            if (p == null) {
                break;
            }
            if (!spawnOne(level, p[0], p[1], p[2])) {
                return false;
            }
        }
        return true;
    }

    private static boolean spawnOne(final ClientLevel level,
                                    final double x, final double y, final double z) {
        try {
            level.addParticle(CrosswindRegistry.WIND_STREAK.get(), x, y, z, 0.0D, 0.0D, 0.0D);
            consecutiveFailures = 0;
            return true;
        } catch (final Throwable t) {
            final int n = ++consecutiveFailures;
            if (n <= 3) {
                cn.chumenta.crosswind.Crosswind.LOGGER.warn(
                        "[crosswind] 风纹粒子生成失败", t);
            }
            if (n > 20) {
                particlesDisabledByFailure = true;
                cn.chumenta.crosswind.Crosswind.LOGGER.warn(
                        "[crosswind] 风纹粒子连续失败 20 次以上，本次游戏内已关闭该效果", n);
            }
            return false;
        }
    }
}
