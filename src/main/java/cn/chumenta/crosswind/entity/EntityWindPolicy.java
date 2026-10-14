package cn.chumenta.crosswind.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

public final class EntityWindPolicy {
    private EntityWindPolicy() {
    }

    public enum Policy {
        NORMAL,

        IGNORE,

        PHYSICS_MANAGED
    }

    public static final String[] EXEMPT_MOD_NAMESPACES = {
            "create",

            "aeronautics",
            "simulated",
    };

    public static boolean isExemptMod(final Entity entity) {
        for (final String ns : EXEMPT_MOD_NAMESPACES) {
            if (isFromMod(entity, ns)) {
                return true;
            }
        }
        return false;
    }

    private static final java.util.Map<Integer, Boolean> WAS_ON_SUB_LEVEL = new java.util.HashMap<>();

    public static boolean shouldApplyWind(final Entity entity, final boolean affectPlayers) {
        if (policyFor(entity, affectPlayers) != Policy.NORMAL) {
            return false;
        }
        if (!cn.chumenta.crosswind.CrosswindConfig.ENTITY_SABLE_GRACE.get()) {
            return true;
        }

        final Boolean wasOn = WAS_ON_SUB_LEVEL.get(entity.getId());
        if (Boolean.TRUE.equals(wasOn)) {
            return entity.onGround() || entity.verticalCollision
                    || cn.chumenta.crosswind.compat.sable.SubmersionCheck.entitySubmerged(entity);
        }
        return true;
    }

    public static Policy policyFor(final Entity entity, final boolean affectPlayers) {
        if (entity.isRemoved() || !entity.isAlive() && entity instanceof net.minecraft.world.entity.LivingEntity) {
            return Policy.IGNORE;
        }

        if (entity.isPassenger()) {
            return Policy.IGNORE;
        }

        if (entity.isNoGravity()) {
            return Policy.IGNORE;
        }

        if (entity instanceof Player player && player.isCreative() && player.getAbilities().flying) {
            return Policy.IGNORE;
        }

        if (cn.chumenta.crosswind.compat.sable.SubmersionCheck.entitySubmerged(entity)) {
            return Policy.IGNORE;
        }

        final boolean onSubLevel = isInsideSubLevel(entity);

        WAS_ON_SUB_LEVEL.put(entity.getId(), onSubLevel);
        if (onSubLevel) {
            return Policy.PHYSICS_MANAGED;
        }

        if (entity instanceof Player) {
            return affectPlayers ? Policy.NORMAL : Policy.IGNORE;
        }

        if (entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return Policy.NORMAL;
        }

        if (isExemptMod(entity)) {
            return Policy.IGNORE;
        }

        return Policy.NORMAL;
    }

    public static boolean isInsideSubLevel(final Entity entity) {
        try {
            final var level = entity.level();
            final double cx = entity.getX();
            final double feetY = entity.getY();
            final double cz = entity.getZ();

            final var probe = new dev.ryanhcode.sable.companion.math.BoundingBox3d(
                    cx, feetY, cz, cx, feetY, cz);

            for (final var subLevel : dev.ryanhcode.sable.Sable.HELPER.getAllIntersecting(level, probe)) {
                if (subLevel == null || subLevel.isRemoved()) {
                    continue;
                }
                final var box = subLevel.boundingBox();
                if (box == null) {
                    continue;
                }

                if (box.contains(cx, feetY, cz)) {
                    return true;
                }

                if (box.contains(cx, entity.getEyeY(), cz)) {
                    return true;
                }
            }
            return false;
        } catch (final Throwable t) {
            return false;
        }
    }

    private static boolean isFromMod(final Entity entity, final String modId) {
        final var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null && modId.equals(key.getNamespace());
    }
}
