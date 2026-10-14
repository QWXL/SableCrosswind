package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.Crosswind;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
public final class RotorModels {
    private RotorModels() {
    }

    public static final net.minecraft.resources.ResourceLocation WIND_VANE_ID =
            Crosswind.id("block/wind_vane_rotor");

    public static final PartialModel WIND_VANE =
            PartialModel.of(WIND_VANE_ID);

    public static final PartialModel ANEMOMETER_HUB =
            PartialModel.of(Crosswind.id("block/anemometer_hub"));

    public static final PartialModel ANEMOMETER_ARM =
            PartialModel.of(Crosswind.id("block/anemometer_arm"));

    public static final net.minecraft.resources.ResourceLocation WEATHER_STATION_VANE_ID =
            Crosswind.id("block/weather_station_vane_rotor");

    public static final PartialModel WEATHER_STATION_VANE =
            PartialModel.of(WEATHER_STATION_VANE_ID);

    public static final PartialModel WEATHER_STATION_ANEMOMETER_HUB =
            PartialModel.of(Crosswind.id("block/weather_station_anemometer_hub"));

    public static final PartialModel WEATHER_STATION_ANEMOMETER_ARM =
            PartialModel.of(Crosswind.id("block/weather_station_anemometer_arm"));

    public static void bootstrap() {
    }

    public static net.minecraft.client.resources.model.ModelResourceLocation standalone(
            final net.minecraft.resources.ResourceLocation id) {
        return net.minecraft.client.resources.model.ModelResourceLocation.standalone(id);
    }
}
