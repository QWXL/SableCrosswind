package cn.chumenta.crosswind.compat.sable;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.wind.WindFieldService;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Crosswind.MOD_ID)
public final class SablePhysicsHook {
    private SablePhysicsHook() {
    }

    private static final Map<java.util.UUID, SubLevelWindForces.Scratch> SCRATCH = new ConcurrentHashMap<>();

    private static long lastStatsTick;
    private static int subLevelsWithWind;
    private static int clampedThisWindow;
    private static double lastMaxAccelG;
    private static double lastMaxForce;
    private static double lastMaxRelSpeed;

    private static final Map<java.util.UUID, CachedWind> WIND_CACHE = new ConcurrentHashMap<>();

    private record CachedWind(long tick, WindSample sample) {
    }
    private static double diagRawForce;
    private static double diagMass;
    private static double diagBodySpeed;
    private static double diagArea;
    private static double maxSailForce;
    private static int sailCount;

    private static double diagWindX;
    private static double diagWindY;
    private static double diagWindZ;

    private static double diagForceX;
    private static double diagForceY;
    private static double diagForceZ;

    private static double maxAbsForceY;
    private static double yForceX;
    private static double yForceY;
    private static double yForceZ;
    private static double yMass;
    private static double yArea;
    private static double yWindX;
    private static double yWindY;
    private static double yWindZ;
    private static double yBodySpeed;
    private static double ySailForce;
    private static int ySailCount;

    @SubscribeEvent
    public static void onPrePhysicsTick(final ForgeSablePrePhysicsTickEvent event) {
        final var physicsSystem = event.getPhysicsSystem();
        final double timeStep = event.getTimeStep();

        final WindSettings settings = WindSettings.fromConfig();
        if (!settings.enabled()) {
            return;
        }

        final ServerLevel level = resolveLevel(physicsSystem);
        if (level == null) {
            return;
        }

        final var container = SubLevelContainer.getContainer(level);
        if (container == null || container.getAllSubLevels().isEmpty()) {
            return;
        }

        final long t0 = System.nanoTime();

        final WindFieldService windService = Crosswind.windService();
        final ForceGroup group = SableWindForceGroup.group();
        int withWind = 0;
        int clamped = 0;
        double maxAccelG = 0.0D;
        double maxForce = 0.0D;
        double maxRelSpeed = 0.0D;
        int totalSails = 0;
        long sampleNanos = 0L;
        long subLevelCount = 0;

        for (final ServerSubLevel subLevel : container.getAllSubLevels()) {
            subLevelCount++;

            if (isLocked(level, subLevel)) {
                continue;
            }

            final var pose = subLevel.logicalPose();
            final var center = pose.position();

            final var massTrackerForCheck = subLevel.getMassTracker();
            if (massTrackerForCheck != null
                    && SubmersionCheck.subLevelSubmerged(level, pose,
                            massTrackerForCheck.getCenterOfMass(), scratchCom)) {
                submergedSkipped++;
                continue;
            }
            final long gameTime = level.getGameTime();
            final java.util.UUID subId = subLevel.getUniqueId();
            final long tSample = System.nanoTime();
            WindSample wind = null;
            final CachedWind cached = WIND_CACHE.get(subId);
            if (cached != null && cached.tick() == gameTime) {
                wind = cached.sample();
                perfCacheHits++;
            }
            if (wind == null) {
                wind = windService.sample(level, center.x(), center.y(), center.z(), settings);
                WIND_CACHE.put(subId, new CachedWind(gameTime, wind));
            }
            sampleNanos += System.nanoTime() - tSample;
            if (wind.isCalm()) {
                continue;
            }

            final var handle = physicsSystem.getPhysicsHandle(subLevel);
            final SubLevelWindForces.Scratch scratch =
                    SCRATCH.computeIfAbsent(subLevel.getUniqueId(), k -> new SubLevelWindForces.Scratch());

            final double result = SubLevelWindForces.apply(
                    level, subLevel, handle, timeStep, wind, settings, group, scratch);

            if (SubLevelTracker.shouldLog(subLevel.getUniqueId(), level.getGameTime())) {
                logTracked(level, subLevel, scratch, wind, result, stepMass(subLevel));

                scratch.diagTickForce.set(0.0D, 0.0D, 0.0D);
                scratch.diagTickImpulse.set(0.0D, 0.0D, 0.0D);
                scratch.diagTickSamples = 0;
            }

            if (result == 0.0D) {
                continue;
            }

            final double forceMag = Math.abs(result);
            final boolean wasClamped = result < 0.0D;

            withWind++;
            totalSails += scratch.diagSailCount;
            maxForce = Math.max(maxForce, forceMag);
            maxAccelG = Math.max(maxAccelG,
                    SubLevelWindForces.accelerationG(forceMag, subLevel.getMassTracker().getMass()));

            if (scratch.observedRelSpeed > maxRelSpeed) {
                maxRelSpeed = scratch.observedRelSpeed;

                diagRawForce = scratch.diagRawForce;
                diagMass = scratch.diagMass;
                diagBodySpeed = scratch.diagBodySpeed;
                diagArea = scratch.diagArea;
            }

            if (scratch.diagSailForce > maxSailForce) {
                maxSailForce = scratch.diagSailForce;
                sailCount = scratch.diagSailCount;
            }

            diagWindX = wind.velocity().x;
            diagWindY = wind.velocity().y;
            diagWindZ = wind.velocity().z;
            diagForceX = scratch.diagForceX;
            diagForceY = scratch.diagForceY;
            diagForceZ = scratch.diagForceZ;

            final double fy = Math.abs(scratch.diagForceY);
            if (fy > maxAbsForceY) {
                maxAbsForceY = fy;
                yForceX = scratch.diagForceX;
                yForceY = scratch.diagForceY;
                yForceZ = scratch.diagForceZ;
                yMass = scratch.diagMass;
                yArea = scratch.diagArea;
                yWindX = wind.velocity().x;
                yWindY = wind.velocity().y;
                yWindZ = wind.velocity().z;
                yBodySpeed = scratch.diagBodySpeed;
                ySailForce = scratch.diagSailForce;
                ySailCount = scratch.diagSailCount;
            }
            if (wasClamped) {
                clamped++;
            }
        }

        if (settings.sableLogging()) {
            recordStats(level.getGameTime(), withWind, clamped, maxForce, maxAccelG, maxRelSpeed,
                    diagRawForce, diagMass, diagBodySpeed, diagArea, maxSailForce, sailCount);

            perfSubLevels += subLevelCount;
            perfSails += totalSails;
            perfSampleNanos += sampleNanos;
            perfTotalNanos += System.nanoTime() - t0;
            perfSteps++;
        }
    }

    private static long perfSubLevels;
    private static long perfSails;
    private static long perfSampleNanos;
    private static long perfTotalNanos;
    private static int perfSteps;
    private static long perfCacheHits;

    private static final org.joml.Vector3d scratchCom = new org.joml.Vector3d();

    private static int submergedSkipped;

    private static double stepMass(final ServerSubLevel subLevel) {
        try {
            return subLevel.getMassTracker().getMass();
        } catch (final Throwable t) {
            return 0.0D;
        }
    }

    private static void logTracked(final ServerLevel level, final ServerSubLevel subLevel,
                                   final SubLevelWindForces.Scratch s, final WindSample wind,
                                   final double result, final double mass) {
        final String name = subLevel.getName() == null ? "?" : subLevel.getName();
        Crosswind.LOGGER.info(
                "[追踪 {}] t={} 位置=({}, {}, {}) 速度={} m/s 角速度={} rad/s",
                name, level.getGameTime(),
                fmt(s.diagPos.x), fmt(s.diagPos.y), fmt(s.diagPos.z),
                fmt(s.diagVel.length()), fmt(s.diagAngVel.length()));
        Crosswind.LOGGER.info(
                "[追踪 {}]   风=({}, {}, {}) |v|={} 气压乘数={}",
                name,
                fmt(wind.velocity().x), fmt(wind.velocity().y), fmt(wind.velocity().z),
                fmt(wind.speed()), fmt(s.diagPressureScale));        Crosswind.LOGGER.info(
                "[追踪 {}]   质量={} 面积={} m² 帆={} 片({} N) 粗粒度={} N 相对风速={} 钳制:加速度={} 接近={}",
                name, fmt(s.diagMass), fmt(s.diagArea),
                s.diagSailCount, fmt(s.diagSailForce), fmt(s.diagCoarseForce),
                fmt(s.observedRelSpeed), s.diagAccelClamped, s.approachClamped);

        Crosswind.LOGGER.info(
                "[追踪 {}]   合力(冲量·N·s)=({}, {}, {}) |J|={} → 实际 |J|={}（{} g）返回={}",
                name,
                fmt(s.diagRawForceVec.x), fmt(s.diagRawForceVec.y), fmt(s.diagRawForceVec.z),
                fmt(s.diagRawForce),
                fmt(Math.abs(result)),
                fmt(SubLevelWindForces.accelerationG(Math.abs(result), mass)),
                fmt(result));

        if (s.diagTickForce.lengthSquared() > 1.0E-18D) {
            final double dot = s.diagTickForce.x * wind.velocity().x
                    + s.diagTickForce.y * wind.velocity().y
                    + s.diagTickForce.z * wind.velocity().z;
            final double cos = dot / (s.diagTickForce.length() * Math.max(1.0E-9D, wind.speed()));
            final double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, cos))));
            Crosswind.LOGGER.info(
                    "[追踪 {}]   合计力=({}, {}, {})（{} 子步）与世界风夹角={} 度",
                    name,
                    fmt(s.diagTickForce.x), fmt(s.diagTickForce.y), fmt(s.diagTickForce.z),
                    s.diagTickSamples, String.format("%.1f", angleDeg));
        }

        if (s.diagTickSamples > 0 && mass > 0.0D) {
            final double expectedDv = s.diagTickImpulse.length() / mass;
            final double measuredDv = s.diagVel.sub(s.diagTickStartVel, s.tmpDbg).length();
            Crosswind.LOGGER.info(
                    "[追踪 {}]   速度变化：Δv_预期={} vs Δv_实测={} m/s（冲量合计 {}，{} 子步）{}",
                    name,
                    fmt(expectedDv), fmt(measuredDv),
                    fmt(s.diagTickImpulse.length()), s.diagTickSamples,
                    measuredDv > expectedDv * 2.0D + 0.5D ? "  ← 速度增量主要来自别处" : "");
        }
    }

    private static ServerLevel resolveLevel(final dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem system) {
        final var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        for (final ServerLevel level : server.getAllLevels()) {
            if (dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.get(level) == system) {
                return level;
            }
        }
        return null;
    }

    private static boolean isLocked(final ServerLevel level, final ServerSubLevel subLevel) {
        try {
            return PhysicsStaffServerHandler.get(level).isLocked(subLevel);
        } catch (final Throwable t) {
            if (!warnedAboutLock) {
                warnedAboutLock = true;
                Crosswind.LOGGER.warn("无法判定子世界锁定状态（{}）——"
                        + "将不跳过任何子世界", t.toString());
            }
            return false;
        }
    }

    private static volatile boolean warnedAboutLock;

    private static void recordStats(final long gameTime, final int withWind, final int clamped,
                                    final double maxForce, final double maxAccelG,
                                    final double maxRelSpeed,
                                    final double rawForce, final double mass,
                                    final double bodySpeed, final double area,
                                    final double sailForce, final int sailCountIn) {
        subLevelsWithWind = withWind;
        clampedThisWindow += clamped;
        lastMaxForce = Math.max(lastMaxForce, maxForce);
        lastMaxAccelG = Math.max(lastMaxAccelG, maxAccelG);
        lastMaxRelSpeed = Math.max(lastMaxRelSpeed, maxRelSpeed);

        if (gameTime - lastStatsTick < 100L) {
            return;
        }
        lastStatsTick = gameTime;

        if (subLevelsWithWind > 0 || clampedThisWindow > 0) {
            Crosswind.LOGGER.info(
                    "风力：{} 子世界 力 {} N（{} g）｜质量 {} 面积 {} m²｜刚体速度 {} m/s 相对风速 {} m/s｜钳制 {} 次",
                    subLevelsWithWind,
                    fmt(maxForce), fmt(maxAccelG),
                    fmt(mass), fmt(area),
                    fmt(bodySpeed), fmt(maxRelSpeed),
                    clampedThisWindow);
            Crosswind.LOGGER.info("  明细：钳制前合力 {} N；帆 {} 片贡献 {} N",
                    fmt(rawForce), sailCountIn, fmt(sailForce));
            Crosswind.LOGGER.info("  矢量：世界风 ({}, {}, {})  施加力 ({}, {}, {})  刚体速度 {} m/s",
                    fmt(diagWindX), fmt(diagWindY), fmt(diagWindZ),
                    fmt(diagForceX), fmt(diagForceY), fmt(diagForceZ),
                    fmt(maxRelSpeed));

            if (perfSteps > 0) {
                final double totalMs = perfTotalNanos / 1.0E6D;
                final double sampleMs = perfSampleNanos / 1.0E6D;
                final double pct = totalMs > 0.0D ? (sampleMs / totalMs * 100.0D) : 0.0D;

                Crosswind.LOGGER.info(
                        "  └ 性能：窗口内 {} 子步 / {} 子世界次（浸没跳过 {}）/ {} 帆次 ｜"
                                + "本模组耗时 {} ms（风场采样 {} ms，占 {}%）"
                                + " ｜ 每子步 {} 子世界 / {} 帆 ｜ 采样缓存命中 {}",
                        perfSteps, perfSubLevels, submergedSkipped, perfSails,
                        String.format("%.2f", totalMs),
                        String.format("%.2f", sampleMs),
                        String.format("%.0f", pct),
                        String.format("%.1f", perfSubLevels / (double) perfSteps),
                        String.format("%.1f", perfSails / (double) perfSteps),
                        perfCacheHits);
            }

            if (maxAbsForceY > 1.0E-6D || Math.abs(yWindY) > 1.0E-6D) {
                Crosswind.LOGGER.info(
                        "  竖直：世界风 Y={} 施加力 Y={}（力矢量 ({}, {}, {})）｜质量 {} 面积 {}｜帆 {} 片贡献 {} N｜刚体速度 {}",
                        fmt(yWindY), fmt(yForceY),
                        fmt(yForceX), fmt(yForceY), fmt(yForceZ),
                        fmt(yMass), fmt(yArea), ySailCount, fmt(ySailForce), fmt(yBodySpeed));
            }
        }
        clampedThisWindow = 0;
        lastMaxForce = 0.0D;
        lastMaxAccelG = 0.0D;
        lastMaxRelSpeed = 0.0D;
        diagRawForce = 0.0D;
        diagMass = 0.0D;
        diagBodySpeed = 0.0D;
        diagArea = 0.0D;
        maxSailForce = 0.0D;
        sailCount = 0;
        maxAbsForceY = 0.0D;
        yForceX = 0.0D;
        yForceY = 0.0D;
        yForceZ = 0.0D;
        yMass = 0.0D;
        yArea = 0.0D;
        yWindX = 0.0D;
        yWindY = 0.0D;
        yWindZ = 0.0D;
        yBodySpeed = 0.0D;
        ySailForce = 0.0D;
        ySailCount = 0;

        perfSubLevels = 0L;
        perfSails = 0L;
        perfSampleNanos = 0L;
        perfTotalNanos = 0L;
        perfSteps = 0;
        perfCacheHits = 0L;
        submergedSkipped = 0;
    }
    private static String fmt(final double v) {
        return String.format("%.2f", v);
    }
}
