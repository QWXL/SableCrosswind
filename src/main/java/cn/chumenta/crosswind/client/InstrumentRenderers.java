package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.WindInstruments;
import cn.chumenta.crosswind.registry.CrosswindRegistry;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Crosswind.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class InstrumentRenderers {
    private InstrumentRenderers() {
    }

    private static final float VANE_MAX_DEGREES_PER_TICK = 22.5F;

    private static float cupSpinSign() {
        final float base = -1.0F;
        return cn.chumenta.crosswind.CrosswindClientConfig.CUP_SPIN_REVERSED.get()
                ? -base
                : base;
    }

    @SubscribeEvent
    public static void onRegisterAdditional(final ModelEvent.RegisterAdditional event) {
        event.register(RotorModels.standalone(RotorModels.WIND_VANE_ID));

        event.register(RotorModels.standalone(Crosswind.id("block/anemometer_hub")));
        event.register(RotorModels.standalone(Crosswind.id("block/anemometer_arm")));

        event.register(RotorModels.standalone(RotorModels.WEATHER_STATION_VANE_ID));
        event.register(RotorModels.standalone(
                Crosswind.id("block/weather_station_anemometer_hub")));
        event.register(RotorModels.standalone(
                Crosswind.id("block/weather_station_anemometer_arm")));
    }

    @SubscribeEvent
    public static void onRegisterRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CrosswindRegistry.WIND_VANE_BE.get(), WindVaneRenderer::new);
        event.registerBlockEntityRenderer(CrosswindRegistry.ANEMOMETER_BE.get(), AnemometerRenderer::new);
        event.registerBlockEntityRenderer(CrosswindRegistry.WEATHER_STATION_BE.get(),
                WeatherStationRenderer::new);
    }

    public static final class WindVaneRenderer
            extends AbstractRotorRenderer<WindInstruments.WindVaneBlockEntity> {
        public WindVaneRenderer(final net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected dev.engine_room.flywheel.lib.model.baked.PartialModel rotorPartial() {
            return RotorModels.WIND_VANE;
        }

        @Override
        protected float currentAngle(final WindInstruments.WindVaneBlockEntity be,
                                     final float partialTick) {
            return be.vaneAngle().advanceTowards(be.readout().fromDegrees(),
                    VANE_MAX_DEGREES_PER_TICK, partialTick);
        }
    }

    public static final class AnemometerRenderer
            extends AbstractRotorRenderer<WindInstruments.AnemometerBlockEntity> {
        public AnemometerRenderer(final net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        protected dev.engine_room.flywheel.lib.model.baked.PartialModel rotorPartial() {
            return RotorModels.ANEMOMETER_HUB;
        }

        @Override
        public void render(final WindInstruments.AnemometerBlockEntity be,
                           final float partialTick,
                           final com.mojang.blaze3d.vertex.PoseStack poseStack,
                           final net.minecraft.client.renderer.MultiBufferSource buffers,
                           final int packedLight, final int packedOverlay) {
            final float degrees = this.rotationSign() * this.currentAngle(be, partialTick);
            final float[] tint = this.warnTint(this.warnIntensity(be, partialTick));

            AbstractRotorRenderer.drawRotor(RotorModels.ANEMOMETER_HUB,
                    be.getBlockState(), degrees, tint, poseStack, buffers, packedLight);

            for (int k = 0; k < 3; k++) {
                AbstractRotorRenderer.drawRotor(RotorModels.ANEMOMETER_ARM,
                        be.getBlockState(), degrees + k * 120.0F, tint,
                        poseStack, buffers, packedLight);
            }
        }

        @Override
        protected float rotationSign() {
            return cupSpinSign();
        }

        @Override
        protected float currentAngle(final WindInstruments.AnemometerBlockEntity be,
                                     final float partialTick) {
            final float dps = cn.chumenta.crosswind.instrument.WindInstrumentAngles
                    .anemometerDegreesPerSecond(be.readout().cupSpeedMps());

            return be.cupAngle().spinBy(dps, partialTick, AbstractRotorRenderer.renderTicks(be));
        }

        @Override
        protected float warnIntensity(final WindInstruments.AnemometerBlockEntity be,
                                      final float partialTick) {
            return cn.chumenta.crosswind.instrument.WindInstrumentAngles
                    .anemometerWarnIntensity(be.readout().cupSpeedMps());
        }

        @Override
        protected float[] warnTint(final float intensity) {
            return cn.chumenta.crosswind.instrument.WindInstrumentAngles.warnTint(intensity);
        }
    }

    public static final class WeatherStationRenderer
            implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<
                    WindInstruments.WeatherStationBlockEntity> {
        public WeatherStationRenderer(
                final net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx) {
        }

        @Override
        public void render(final WindInstruments.WeatherStationBlockEntity be,
                           final float partialTick,
                           final com.mojang.blaze3d.vertex.PoseStack poseStack,
                           final net.minecraft.client.renderer.MultiBufferSource buffers,
                           final int packedLight, final int packedOverlay) {
            final var readout = be.readout();

            final long gameTime = AbstractRotorRenderer.renderTicks(be);

            final float vaneDegrees = -be.vaneAngle()
                    .advanceTowards(readout.fromDegrees(), VANE_MAX_DEGREES_PER_TICK, partialTick);
            AbstractRotorRenderer.drawRotor(RotorModels.WEATHER_STATION_VANE,
                    be.getBlockState(), vaneDegrees, new float[]{1.0F, 1.0F, 1.0F},
                    poseStack, buffers, packedLight);

            final float dps = cn.chumenta.crosswind.instrument.WindInstrumentAngles
                    .anemometerDegreesPerSecond(readout.cupSpeedMps());

            final float cupDegrees = cupSpinSign()
                    * be.cupAngle().spinBy(dps, partialTick, gameTime);

            final float[] tint = new float[]{1.0F, 1.0F, 1.0F};

            AbstractRotorRenderer.drawRotor(RotorModels.WEATHER_STATION_ANEMOMETER_HUB,
                    be.getBlockState(), cupDegrees, tint, poseStack, buffers, packedLight);
            for (int k = 0; k < 3; k++) {
                AbstractRotorRenderer.drawRotor(RotorModels.WEATHER_STATION_ANEMOMETER_ARM,
                        be.getBlockState(), cupDegrees + k * 120.0F, tint,
                        poseStack, buffers, packedLight);
            }
        }
    }
}
