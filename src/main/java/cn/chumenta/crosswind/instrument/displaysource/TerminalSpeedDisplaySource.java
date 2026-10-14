package cn.chumenta.crosswind.instrument.displaysource;

import cn.chumenta.crosswind.instrument.WindReadout;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Locale;

public class TerminalSpeedDisplaySource extends AbstractInstrumentDisplaySource {
    @Override
    public String getTranslationKey() {
        return "terminal_speed";
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context,
                                           final DisplayTargetStats stats) {
        final WindReadout r = readoutOf(context);
        if (r == null) {
            return EMPTY_LINE;
        }

        return Component.literal(String.format(Locale.ROOT, "%.1f m/s", r.cupSpeedMps()));
    }
}
