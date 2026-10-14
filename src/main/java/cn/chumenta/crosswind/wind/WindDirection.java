package cn.chumenta.crosswind.wind;

public final class WindDirection {
    private WindDirection() {
    }

    public static final String[] KEY_SUFFIXES = {
            "n", "nne", "ne", "ene", "e", "ese", "se", "sse",
            "s", "ssw", "sw", "wsw", "w", "wnw", "nw", "nnw",
    };

    public static final String[] ENGLISH_SHORT = {
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
    };

    public static int index(final double degrees) {
        final double d = WindMath.wrapDegrees(degrees);

        return (int) Math.round(d / 22.5D) & 15;
    }

    public static String cardinalKey(final double degrees) {
        return "gui.crosswind.dir." + KEY_SUFFIXES[index(degrees)];
    }

    public static String cardinalShort(final double degrees) {
        return ENGLISH_SHORT[index(degrees)];
    }
}
