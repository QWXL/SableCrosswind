package cn.chumenta.crosswind.registry;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.displaysource.AbstractInstrumentDisplaySource;
import cn.chumenta.crosswind.instrument.displaysource.AviationWeatherDisplaySource;
import cn.chumenta.crosswind.instrument.displaysource.TerminalDirectionDisplaySource;
import cn.chumenta.crosswind.instrument.displaysource.TerminalSpeedDisplaySource;
import cn.chumenta.crosswind.instrument.displaysource.TerminalWindDisplaySource;
import com.simibubi.create.api.registry.CreateRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = Crosswind.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class CrosswindDisplaySources {
    private CrosswindDisplaySources() {
    }

    private static TerminalSpeedDisplaySource terminalSpeed;
    private static TerminalDirectionDisplaySource terminalDirection;

    private static TerminalWindDisplaySource terminalWind;

    private static AviationWeatherDisplaySource aviationWeather;

    public static TerminalSpeedDisplaySource terminalSpeed() {
        return terminalSpeed;
    }

    public static TerminalDirectionDisplaySource terminalDirection() {
        return terminalDirection;
    }

    public static TerminalWindDisplaySource terminalWind() {
        return terminalWind;
    }

    public static AviationWeatherDisplaySource aviationWeather() {
        return aviationWeather;
    }

    @SubscribeEvent
    public static void onRegister(final RegisterEvent event) {
        if (!event.getRegistryKey().equals(CreateRegistries.DISPLAY_SOURCE)) {
            return;
        }

        terminalSpeed = register(event, "terminal_speed", new TerminalSpeedDisplaySource());
        terminalDirection = register(event, "terminal_direction", new TerminalDirectionDisplaySource());
        terminalWind = register(event, "terminal_wind", new TerminalWindDisplaySource());
        aviationWeather = register(event, "aviation_weather", new AviationWeatherDisplaySource());
    }

    private static <T extends AbstractInstrumentDisplaySource> T register(
            final RegisterEvent event, final String path, final T source) {
        event.register(CreateRegistries.DISPLAY_SOURCE, Crosswind.id(path), () -> source);
        return source;
    }

    @SubscribeEvent
    public static void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            if (terminalSpeed == null || terminalDirection == null
                    || terminalWind == null || aviationWeather == null) {
                Crosswind.LOGGER.warn("显示源未全部注册成功，跳过与方块实体的关联");
                return;
            }
            try {
                terminalSpeed.registerFor(CrosswindRegistry.ANEMOMETER_BE.get());
                terminalDirection.registerFor(CrosswindRegistry.WIND_VANE_BE.get());

                terminalWind.registerFor(CrosswindRegistry.WEATHER_STATION_BE.get());
                aviationWeather.registerFor(CrosswindRegistry.WEATHER_STATION_BE.get());
            } catch (final Throwable t) {
                Crosswind.LOGGER.warn("显示源关联方块实体失败", t);
            }
        });
    }
}
