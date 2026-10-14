package cn.chumenta.crosswind.instrument.displaysource;

import cn.chumenta.crosswind.instrument.WindReadout;
import cn.chumenta.crosswind.instrument.WindTelemetry;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class AviationWeatherDisplaySource extends AbstractInstrumentDisplaySource {
    @Override
    public String getTranslationKey() {
        return "aviation_weather";
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context,
                                           final DisplayTargetStats stats) {
        final WindReadout r = readoutOf(context);
        if (r == null) {
            return EMPTY_LINE;
        }
        return Component.literal(WindTelemetry.windCondition(
                r.speedMps(),
                r.peakSpeedMps(),
                r.fromDegrees(),
                r.toDegrees(),
                r.temperatureC(),
                r.precipitation(),
                r.climateAvailable()));
    }
}
