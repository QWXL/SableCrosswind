package cn.chumenta.crosswind.entity;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.CrosswindConfig;
import cn.chumenta.crosswind.wind.WindFieldService;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.joml.Vector3d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public final class EntityWindHandler {
    private EntityWindHandler() {
    }

    private static boolean registered;

    private static final Vector3d DRIVE = new Vector3d();
    private static final Vector3d ACCEL = new Vector3d();



    public static synchronized void register() {
        if (registered) {
            return;
        }
        NeoForge.EVENT_BUS.addListener(EntityWindHandler::onEntityTick);
        registered = true;
        Crosswind.LOGGER.info("实体受风已启用");
    }

    private static void onEntityTick(final EntityTickEvent.Post event) {
        final Entity entity = event.getEntity();

        if (!(entity.level() instanceof final net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        try {
            applyWind(entity, level);
        } catch (final Throwable t) {
            Crosswind.LOGGER.debug("实体受风失败：{}", entity.getType(), t);
        }

        flushMotionPayload(level);
    }

    private static double groundFrictionFor(final Entity entity) {
        try {
            if (!(entity.level() instanceof final net.minecraft.server.level.ServerLevel level)) {
                return 1.0D;
            }
            if (!entity.onGround()) {
                return 0.98D;
            }
            final var below = entity.getBlockPosBelowThatAffectsMyMovement();
            final float blockFriction = level.getBlockState(below)
                    .getFriction(level, below, entity);
            final double base = Math.min(1.0D, blockFriction * 0.98D);

            if (cn.chumenta.crosswind.item.NonSlipBootsItem.isWornBy(entity)) {
                return base * cn.chumenta.crosswind.CrosswindConfig
                        .NON_SLIP_BOOTS_SLIDE_FACTOR.get();
            }
            return base;
        } catch (final Throwable t) {
            return 1.0D;
        }
    }

    private static final Map<java.util.UUID, double[]> PREV_PLAYER_VEL = new HashMap<>();

    private static void measurePlayerDelta(final Entity entity,
                                           final net.minecraft.server.level.ServerLevel level) {
        final Vec3 now = entity.getDeltaMovement();
        final double[] prev = PREV_PLAYER_VEL.put(entity.getUUID(),
                new double[] { now.x, now.y, now.z });
        if (prev == null) {
            return;
        }


    }


    private static final List<cn.chumenta.crosswind.network.WindMotionPayload.Entry> PENDING_DELTAS =
            new ArrayList<>();

    private static long lastFlushTick = Long.MIN_VALUE;

    private static void flushMotionPayload(final net.minecraft.server.level.ServerLevel level) {
        if (PENDING_DELTAS.isEmpty()) {
            return;
        }
        final long tick = level.getGameTime();
        if (tick == lastFlushTick) {
            return;
        }

        final long interval = Math.max(1, CrosswindConfig.ENTITY_SYNC_INTERVAL.get());
        final boolean due = deltaChanged
                || lastSentTick == Long.MIN_VALUE
                || tick - lastSentTick >= interval;
        if (!due) {
            PENDING_DELTAS.clear();
            PENDING_MAP.clear();
            lastFlushTick = tick;
            return;
        }
        lastSentTick = tick;
        lastFlushTick = tick;
        deltaChanged = false;

        try {
            final List<cn.chumenta.crosswind.network.WindMotionPayload.Entry> outgoing =
                    new ArrayList<>(PENDING_DELTAS.size() + PENDING_CLEAR.size()
                            + LAST_SENT_DELTA.size());
            for (final int id : PENDING_CLEAR) {
                outgoing.add(new cn.chumenta.crosswind.network.WindMotionPayload.Entry(
                        id, CLEAR_SENTINEL, CLEAR_SENTINEL, CLEAR_SENTINEL));
            }

            for (final int id : LAST_SENT_DELTA.keySet()) {
                if (!PENDING_MAP.containsKey(id) && CLEARED.add(id)) {
                    outgoing.add(new cn.chumenta.crosswind.network.WindMotionPayload.Entry(
                            id, CLEAR_SENTINEL, CLEAR_SENTINEL, CLEAR_SENTINEL));
                }
            }

            for (final int id : PENDING_MAP.keySet()) {
                CLEARED.remove(id);
            }

            outgoing.addAll(PENDING_DELTAS);

            if (outgoing.isEmpty()) {
                LAST_SENT_DELTA.clear();
                return;
            }

            final var payload = new cn.chumenta.crosswind.network.WindMotionPayload(
                    tick, List.copyOf(outgoing));
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);

            LAST_SENT_DELTA.clear();
            LAST_SENT_DELTA.putAll(PENDING_MAP);
        } catch (final Throwable t) {
            Crosswind.LOGGER.debug("发送风力增量包失败", t);
        } finally {
            PENDING_DELTAS.clear();
            PENDING_MAP.clear();
            PENDING_CLEAR.clear();
        }
    }

    private static final java.util.Set<Integer> CLEARED = new java.util.HashSet<>();

    private static long lastSentTick = Long.MIN_VALUE;

    private static final Map<Integer, float[]> LAST_SENT_DELTA = new HashMap<>();

    private static final double DELTA_CHANGE_THRESHOLD = 0.005D;

    private static boolean deltaChanged;

    private static final Map<Integer, float[]> PENDING_MAP = new HashMap<>();

    private static final Map<Integer, Boolean> LAST_ON_GROUND = new HashMap<>();

    private static final List<Integer> PENDING_CLEAR = new ArrayList<>();

    public static final float CLEAR_SENTINEL = Float.NaN;


    private static void applyWind(final Entity entity,
                                  final net.minecraft.server.level.ServerLevel level) {
        if (entity instanceof net.minecraft.world.entity.player.Player) {
            measurePlayerDelta(entity, level);
        }

        final WindSettings settings = WindSettings.fromConfig();
        if (!settings.enabled() || !CrosswindConfig.ENTITY_ENABLED.get()) {
            return;
        }

        if (!EntityWindPolicy.shouldApplyWind(entity, CrosswindConfig.ENTITY_AFFECT_PLAYERS.get())) {
            return;
        }

        final AABB box = entity.getBoundingBox();
        final double sizeX = box.getXsize();
        final double sizeY = box.getYsize();
        final double sizeZ = box.getZsize();

        final WindFieldService service = Crosswind.windService();
        final WindSample wind = service.sample(level, entity.getX(), entity.getY(), entity.getZ(), settings);
        if (wind.isCalm()) {
            return;
        }
        final Vec3 windVec = wind.velocity();
        final double windX = windVec.x;
        final double windZ = windVec.z;

        final double area = EntityAerodynamics.projectedArea(sizeX, sizeY, sizeZ, windX, windZ);
        if (area <= 0.0D) {
            return;
        }

        final Vec3 vel = entity.getDeltaMovement();
        EntityAerodynamics.horizontalDrive(windX, windZ, vel.x, vel.z, DRIVE);
        if (DRIVE.lengthSquared() < 1.0E-8D) {
            return;
        }

        final double volume = sizeX * sizeY * sizeZ;

        final double effMass = EntityAerodynamics.effectiveMassWithResistance(
                volume,
                CrosswindConfig.ENTITY_MASS_COEFFICIENT.get(),
                CrosswindConfig.ENTITY_MIN_VOLUME.get(),
                CrosswindConfig.ENTITY_REFERENCE_VOLUME.get(),
                CrosswindConfig.ENTITY_RESISTANCE_EXPONENT.get());

        double multiplier = 1.0D;
        if (CrosswindConfig.ENTITY_USE_FORCE_MULTIPLIER.get()) {
            multiplier = settings.sableForceMultiplier();
        }

        final double densityRatio = CrosswindConfig.PRESSURE_AFFECTS_FORCE.get()
                ? cn.chumenta.crosswind.wind.DensityMath.densityRatio(
                        wind.pressure(), settings.pressureReference(), wind.temperatureC())
                : 1.0D;

        EntityAerodynamics.acceleration(DRIVE, area, effMass,
                settings.airDensityReference() * densityRatio,
                settings.windDragCoefficient(),
                CrosswindConfig.ENTITY_MAX_ACCELERATION.get(),
                ACCEL);
        if (multiplier != 1.0D) {
            ACCEL.mul(multiplier);
        }
        if (ACCEL.lengthSquared() < 1.0E-12D) {
            return;
        }

        final Vector3d newVel = new Vector3d(
                vel.x + ACCEL.x * EntityAerodynamics.TICK_SECONDS,
                vel.y,
                vel.z + ACCEL.z * EntityAerodynamics.TICK_SECONDS);

        final double friction = groundFrictionFor(entity);
        if (friction > 0.0D && friction < 1.0D) {
            newVel.x *= friction;
            newVel.z *= friction;
        }


        entity.setDeltaMovement(new Vec3(newVel.x, newVel.y, newVel.z));

        PENDING_DELTAS.add(new cn.chumenta.crosswind.network.WindMotionPayload.Entry(
                entity.getId(),
                (float) (newVel.x - vel.x),
                (float) (newVel.y - vel.y),
                (float) (newVel.z - vel.z)));

        final float[] now = { (float) (newVel.x - vel.x), (float) (newVel.y - vel.y),
                (float) (newVel.z - vel.z) };
        PENDING_MAP.put(entity.getId(), now);
        final float[] prev = LAST_SENT_DELTA.get(entity.getId());
        if (prev == null) {
            deltaChanged = true;
        } else if (Math.abs(now[0] - prev[0]) > DELTA_CHANGE_THRESHOLD
                || Math.abs(now[1] - prev[1]) > DELTA_CHANGE_THRESHOLD
                || Math.abs(now[2] - prev[2]) > DELTA_CHANGE_THRESHOLD) {
            deltaChanged = true;
        }

        final boolean onGroundNow = entity.onGround();
        final Boolean onGroundPrev = LAST_ON_GROUND.put(entity.getId(), onGroundNow);
        if (onGroundPrev != null && onGroundPrev != onGroundNow) {
            PENDING_CLEAR.add(entity.getId());
            LAST_SENT_DELTA.remove(entity.getId());
            deltaChanged = true;
        }




    }
}
