package cn.chumenta.crosswind.advancement;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.WindReadout;
import cn.chumenta.crosswind.item.NonSlipBootsItem;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CrosswindTriggers {
    private CrosswindTriggers() {
    }

    public static final double WITNESS_RADIUS = 32.0D;

    private static final double WITNESS_RADIUS_SQR = WITNESS_RADIUS * WITNESS_RADIUS;

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, Crosswind.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, WindSpeedTrigger> WIND_SPEED =
            TRIGGER_TYPES.register("wind_speed", WindSpeedTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, InstrumentDestroyedTrigger> INSTRUMENT_DESTROYED =
            TRIGGER_TYPES.register("instrument_destroyed", InstrumentDestroyedTrigger::new);

    public static final DeferredHolder<CriterionTrigger<?>, EquipBootsTrigger> EQUIP_BOOTS =
            TRIGGER_TYPES.register("equip_boots", EquipBootsTrigger::new);

    public static void register(final IEventBus modBus) {
        TRIGGER_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(CrosswindTriggers::onEquipmentChange);
    }

    public static void onReadoutSynced(final ServerLevel level,
                                       final double x, final double y, final double z,
                                       final WindReadout readout,
                                       final WindReadout.Fields provides) {
        if (!provides.speed()) {
            return;
        }

        final double witnessed = Math.max(readout.cupSpeedMps(), readout.peakSpeedMps());
        for (final ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= WITNESS_RADIUS_SQR) {
                WIND_SPEED.get().trigger(player, witnessed);
            }
        }
    }

    public static void onAnemometerDestroyed(final ServerLevel level,
                                             final double x, final double y, final double z) {
        for (final ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) <= WITNESS_RADIUS_SQR) {
                INSTRUMENT_DESTROYED.get().trigger(player);
            }
        }
    }

    private static void onEquipmentChange(final LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.FEET) {
            return;
        }
        if (!(event.getEntity() instanceof final ServerPlayer player)) {
            return;
        }
        if (!(event.getTo().getItem() instanceof NonSlipBootsItem)) {
            return;
        }
        EQUIP_BOOTS.get().trigger(player);
    }
}
