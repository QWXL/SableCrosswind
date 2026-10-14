package cn.chumenta.crosswind.instrument;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import cn.chumenta.crosswind.wind.WindDirection;

import java.util.List;
import java.util.Locale;

public final class InstrumentGoggleInfo {
    private InstrumentGoggleInfo() {
    }

    public static boolean addTo(
            final List<Component> tooltip,
            final WindReadout readout,
            final Component label,
            final WindReadout.Fields fields) {
        if (!fields.any()) {
            return false;
        }

        tooltip.add(Component.literal("     ")
                .append(label.copy())
                .withStyle(ChatFormatting.WHITE));

        if (fields.speed()) {
            if (fields.gust()) {
                tooltip.add(line("gui.crosswind.goggle.second_speed",
                        mps(readout.cupSpeedMps()), knots(readout.cupSpeedKnots())));
                tooltip.add(line("gui.crosswind.goggle.speed",
                        mps(readout.peakSpeedMps()), knots(readout.peakSpeedKnots())));
            } else {
                tooltip.add(line("gui.crosswind.goggle.cup_speed",
                        mps(readout.cupSpeedMps()), knots(readout.cupSpeedKnots())));
            }
        }

        if (!readout.directionAvailable()
                && (fields.fromDirection() || fields.toDirection())) {
            tooltip.add(fields.gust()
                    ? line("gui.crosswind.goggle.wind",
                            Component.translatable("gui.crosswind.calm"), null)
                    : Component.translatable("gui.crosswind.calm")
                            .withStyle(ChatFormatting.GRAY));

            appendClimate(tooltip, readout, fields);
            return true;
        }

        if (fields.fromDirection() && fields.toDirection()) {
            final MutableComponent dir = Component.translatable("gui.crosswind.goggle.wind_from_to",
                    cardinal(readout.fromDegrees()),
                    cardinal(readout.toDegrees()));
            if (fields.gust()) {
                tooltip.add(Component.translatable("gui.crosswind.goggle.wind")
                        .withStyle(ChatFormatting.GRAY)
                        .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                        .append(dir.withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(" (" + bearingPair(
                                        readout.fromDegrees(), readout.toDegrees()) + ")")
                                .withStyle(ChatFormatting.DARK_AQUA)));
            } else {
                tooltip.add(dir.withStyle(ChatFormatting.GRAY));
            }
        } else if (fields.fromDirection()) {
            tooltip.add(line("gui.crosswind.goggle.wind_from",
                    cardinal(readout.fromDegrees()),
                    String.format(Locale.ROOT, "%.0f°", readout.fromDegrees())));
        } else if (fields.toDirection()) {
            tooltip.add(line("gui.crosswind.goggle.wind_to",
                    cardinal(readout.toDegrees()),
                    String.format(Locale.ROOT, "%.0f°", readout.toDegrees())));
        }

        appendClimate(tooltip, readout, fields);
        return true;
    }

    private static void appendClimate(final List<Component> tooltip,
                                      final WindReadout readout,
                                      final WindReadout.Fields fields) {
        if (fields.temperature()) {
            tooltip.add(line("gui.crosswind.goggle.temperature",
                    readout.climateAvailable()
                            ? String.format(Locale.ROOT, "%.1f °C", readout.temperatureC())
                            : Component.translatable("gui.crosswind.unknown"),
                    null));
        }

        if (fields.precipitation()) {
            tooltip.add(line("gui.crosswind.goggle.precipitation",
                    readout.climateAvailable()
                            ? String.format(Locale.ROOT, "%.0f%%", readout.precipitation() * 100.0F)

                            : Component.translatable("gui.crosswind.unknown"),
                    null));
        }
    }

    private static String mps(final double v) {
        return String.format(Locale.ROOT, "%.1f m/s", v);
    }

    private static String knots(final double v) {
        return String.format(Locale.ROOT, "%.1f kt", v);
    }

    private static String bearingPair(final float fromDegrees, final float toDegrees) {
        return String.format(Locale.ROOT, "%.0f° → %.0f°", fromDegrees, toDegrees);
    }

    private static MutableComponent cardinal(final float degrees) {
        return Component.translatable(WindDirection.cardinalKey(degrees))
                .withStyle(ChatFormatting.AQUA);
    }

    private static MutableComponent line(final String key, final Object value,
                                         final String secondary) {
        final MutableComponent c = Component.translatable(key)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" ").withStyle(ChatFormatting.GRAY))
                .append(value instanceof final Component comp
                        ? comp.copy().withStyle(ChatFormatting.AQUA)
                        : Component.literal(String.valueOf(value)).withStyle(ChatFormatting.AQUA));
        if (secondary != null) {
            return c.append(Component.literal(" (" + secondary + ")")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        return c;
    }
}
