package cn.chumenta.crosswind.client.ponder;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.registry.CrosswindRegistry;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class CrosswindPonderTags {
    public static final ResourceLocation INSTRUMENTS = Crosswind.id("instruments");

    public static final ResourceLocation CREATE_DISPLAY_SOURCES =
            ResourceLocation.fromNamespaceAndPath("create", "display_sources");

    private CrosswindPonderTags() {
    }

    public static void register(final PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(INSTRUMENTS)

                .item(CrosswindRegistry.WIND_VANE.get())
                .title("The Air That Brushes Past")
                .description("Instruments that read the local wind field")

                .addToIndex()

                .register();

        helper.addToTag(INSTRUMENTS)
                .add(Crosswind.id("wind_vane"))
                .add(Crosswind.id("anemometer"))
                .add(Crosswind.id("weather_station"))
                .add(Crosswind.id("instrument_rod"));

        helper.addToTag(CREATE_DISPLAY_SOURCES)
                .add(Crosswind.id("wind_vane"))
                .add(Crosswind.id("anemometer"))
                .add(Crosswind.id("weather_station"));
    }
}
