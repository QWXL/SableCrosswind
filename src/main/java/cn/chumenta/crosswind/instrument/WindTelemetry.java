package cn.chumenta.crosswind.instrument;

import cn.chumenta.crosswind.wind.WindSample;

import java.util.Locale;

public final class WindTelemetry {
    private WindTelemetry() {
    }

    public static final String KEY_SPEED = "S";

    public static final String KEY_PEAK = "G";

    public static final String KEY_FROM = "F";

    public static final String KEY_TO = "T";

    public static final String KEY_TEMPERATURE = "C";

    public static final String KEY_PRECIPITATION = "P";

    public static final String UNKNOWN = "?";

    public static String windCondition(final double speedMps,
                                       final double peakSpeedMps,
                                       final double fromDegrees,
                                       final double toDegrees,
                                       final double temperatureC,
                                       final double precipitation,
                                       final boolean climateAvailable) {
        final boolean hasDirection = speedMps >= WindSample.DIRECTION_FLOOR_MPS;

        final StringBuilder sb = new StringBuilder(48);
        append(sb, KEY_SPEED, oneDecimal(speedMps));
        append(sb, KEY_PEAK, oneDecimal(peakSpeedMps));
        append(sb, KEY_FROM, hasDirection ? degrees(fromDegrees) : UNKNOWN);
        append(sb, KEY_TO, hasDirection ? degrees(toDegrees) : UNKNOWN);

        append(sb, KEY_TEMPERATURE, climateAvailable ? oneDecimal(temperatureC) : UNKNOWN);
        append(sb, KEY_PRECIPITATION, climateAvailable ? twoDecimals(precipitation) : UNKNOWN);
        return sb.toString();
    }

    private static void append(final StringBuilder sb, final String key, final String value) {
        if (sb.length() > 0) {
            sb.append(' ');
        }
        sb.append(key).append(':').append(value);
    }

    private static String oneDecimal(final double v) {
        return Double.isFinite(v) ? String.format(Locale.ROOT, "%.1f", v) : UNKNOWN;
    }

    private static String twoDecimals(final double v) {
        return Double.isFinite(v) ? String.format(Locale.ROOT, "%.2f", v) : UNKNOWN;
    }

    private static String degrees(final double v) {
        if (!Double.isFinite(v)) {
            return UNKNOWN;
        }
        int d = (int) Math.round(v) % 360;
        if (d < 0) {
            d += 360;
        }
        return String.format(Locale.ROOT, "%03d", d);
    }
}
