package cn.chumenta.crosswind.instrument.displaysource;

import cn.chumenta.crosswind.instrument.WindReadout;
import cn.chumenta.crosswind.wind.WindDirection;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class TerminalDirectionDisplaySource extends AbstractInstrumentDisplaySource {
    @Override
    public String getTranslationKey() {
        return "terminal_direction";
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context,
                                           final DisplayTargetStats stats) {
        final WindReadout r = readoutOf(context);
        if (r == null) {
            return EMPTY_LINE;
        }

        if (!r.directionAvailable()) {
            return Component.translatable("gui.crosswind.calm");
        }
        return Component.translatable("gui.crosswind.goggle.wind_from_to",
                Component.translatable(WindDirection.cardinalKey(r.fromDegrees())),
                Component.translatable(WindDirection.cardinalKey(r.toDegrees())));
    }
}
