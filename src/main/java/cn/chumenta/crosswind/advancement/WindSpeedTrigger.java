package cn.chumenta.crosswind.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class WindSpeedTrigger extends SimpleCriterionTrigger<WindSpeedTrigger.TriggerInstance> {
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(final ServerPlayer player, final double speedMps) {
        this.trigger(player, instance -> instance.speed().matches(speedMps));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player,
                                  MinMaxBounds.Doubles speed)
            implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(

                                EntityPredicate.ADVANCEMENT_CODEC
                                        .optionalFieldOf("player")
                                        .forGetter(TriggerInstance::player),
                                MinMaxBounds.Doubles.CODEC
                                        .optionalFieldOf("speed", MinMaxBounds.Doubles.ANY)
                                        .forGetter(TriggerInstance::speed))
                        .apply(instance, TriggerInstance::new));
    }
}
