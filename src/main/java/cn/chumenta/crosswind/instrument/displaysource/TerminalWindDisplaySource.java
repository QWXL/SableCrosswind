package cn.chumenta.crosswind.instrument.displaysource;

import cn.chumenta.crosswind.instrument.WindReadout;
import cn.chumenta.crosswind.wind.WindDirection;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Locale;

public class TerminalWindDisplaySource extends AbstractInstrumentDisplaySource {
    public static final String CONFIG_KEY_MODE = "Mode";

    public static final int FIELD_TERMINAL_SPEED = 0;
    public static final int FIELD_TERMINAL_DIRECTION = 1;
    public static final int FIELD_WIND_FROM = 2;
    public static final int FIELD_WIND_TO = 3;
    public static final int FIELD_TEMPERATURE = 4;
    public static final int FIELD_PRECIPITATION = 5;

    private static final String LANG_PREFIX = "crosswind.display_source.terminal_wind.";

    private static final String[] OPTIONS = {
            "terminal_speed", "terminal_direction",
            "wind_from", "wind_to", "temperature", "precipitation"
    };

    @Override
    public String getTranslationKey() {
        return "terminal_wind";
    }

    @Override
    protected MutableComponent provideLine(final DisplayLinkContext context,
                                           final DisplayTargetStats stats) {
        final WindReadout r = readoutOf(context);
        if (r == null) {
            return EMPTY_LINE;
        }
        final int field = context.sourceConfig().getInt(CONFIG_KEY_MODE);
        return switch (field) {
            case FIELD_TERMINAL_SPEED -> Component.literal(
                    String.format(Locale.ROOT, "%.1f m/s", r.cupSpeedMps()));

            case FIELD_TERMINAL_DIRECTION -> r.directionAvailable()
                    ? Component.translatable("gui.crosswind.goggle.wind_from_to",
                            Component.translatable(WindDirection.cardinalKey(r.fromDegrees())),
                            Component.translatable(WindDirection.cardinalKey(r.toDegrees())))
                    : Component.translatable("gui.crosswind.calm");
            case FIELD_WIND_FROM -> r.directionAvailable()
                    ? Component.translatable(WindDirection.cardinalKey(r.fromDegrees()))
                    : Component.translatable("gui.crosswind.calm");
            case FIELD_WIND_TO -> r.directionAvailable()
                    ? Component.translatable(WindDirection.cardinalKey(r.toDegrees()))
                    : Component.translatable("gui.crosswind.calm");
            case FIELD_TEMPERATURE -> r.climateAvailable()
                    ? Component.literal(String.format(Locale.ROOT, "%.1f °C", r.temperatureC()))

                    : Component.translatable("gui.crosswind.unknown");
            case FIELD_PRECIPITATION -> Component.literal(
                    String.format(Locale.ROOT, "%.0f%%", r.precipitation() * 100.0F));
            default -> EMPTY_LINE;
        };
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initConfigurationWidgets(final DisplayLinkContext context,
                                         final ModularGuiLineBuilder builder,
                                         final boolean isFirstLine) {
        super.initConfigurationWidgets(context, builder, isFirstLine);
        if (isFirstLine) {
            return;
        }

        final java.util.List<Component> options = new java.util.ArrayList<>(OPTIONS.length);
        for (final String option : OPTIONS) {
            options.add(Component.translatable(LANG_PREFIX + option));
        }
        builder.addSelectionScrollInput(0, 137, (selectionScrollInput, label) ->
                        selectionScrollInput
                                .forOptions(options)
                                .titled(Component.translatable(LANG_PREFIX + "title")),
                CONFIG_KEY_MODE);
    }
}
