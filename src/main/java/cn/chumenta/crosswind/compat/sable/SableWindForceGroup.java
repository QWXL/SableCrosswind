package cn.chumenta.crosswind.compat.sable;

import cn.chumenta.crosswind.Crosswind;
import dev.ryanhcode.sable.api.physics.force.ForceGroup;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class SableWindForceGroup {
    private SableWindForceGroup() {
    }

    private static final ResourceLocation WIND_ID = Crosswind.id("wind");

    private static ForceGroup windGroup;

    private static boolean registered;

    private static String failureReason;

    public static void register(final net.neoforged.bus.api.IEventBus modBus) {
        try {
            final ForceGroup existing = ForceGroups.REGISTRY.get(WIND_ID);
            if (existing != null) {
                windGroup = existing;
                registered = true;
                Crosswind.LOGGER.info("复用力组 crosswind:wind");
                return;
            }

            final var key = net.minecraft.resources.ResourceKey.create(
                    ForceGroups.REGISTRY_KEY, WIND_ID);
            final ForceGroup created = new ForceGroup(
                    Component.translatable("force_group.crosswind.wind"),
                    null,
                    0x7FD4C1,
                    true);

            ForceGroup result;
            try {
                result = net.minecraft.core.Registry.register(ForceGroups.REGISTRY, key, created);
            } catch (final UnsupportedOperationException e) {
                result = null;
                failureReason = "注册表已冻结（" + e.getMessage() + "）";
            }

            if (result == null) {
                registered = false;
                Crosswind.LOGGER.warn("无法注册自定义力组 crosswind:wind（{}）——"
                        + "风力将记入内置力组 sable:lift", failureReason);
                return;
            }

            windGroup = result;
            registered = true;
            Crosswind.LOGGER.info("已注册 Sable 力组 crosswind:wind");
        } catch (final Throwable t) {
            registered = false;
            failureReason = t.getClass().getSimpleName() + ": " + t.getMessage();
            Crosswind.LOGGER.warn("无法注册自定义力组 crosswind:wind（{}）——"
                    + "风力将记入内置力组 sable:lift", failureReason);
        }
    }

    public static ForceGroup group() {
        final ForceGroup g = windGroup;
        return g != null ? g : ForceGroups.LIFT.get();
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static String failureReason() {
        return failureReason;
    }
}
