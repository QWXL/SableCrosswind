package cn.chumenta.crosswind.wind;

public final class DensityMath {
    private DensityMath() {
    }

    public static final double REFERENCE_TEMPERATURE_K = 288.15D;

    public static final double MIN_TEMPERATURE_RATIO = 0.5D;

    public static final double MAX_TEMPERATURE_RATIO = 2.0D;

    public static double temperatureRatio(final double temperatureC) {
        final double kelvin = temperatureC + 273.15D;
        if (!(kelvin > 1.0D)) {
            return 1.0D;
        }
        return WindMath.clamp(REFERENCE_TEMPERATURE_K / kelvin,
                MIN_TEMPERATURE_RATIO, MAX_TEMPERATURE_RATIO);
    }

    public static double densityRatio(final double pressure, final double reference,
                                      final double temperatureC) {
        if (!(pressure > 0.0D) || !(reference > 0.0D)) {
            return 1.0D;
        }
        return (pressure / reference) * temperatureRatio(temperatureC);
    }
}
