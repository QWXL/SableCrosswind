package cn.chumenta.crosswind.wind;

public final class PressureMath {
    private PressureMath() {
    }

    public static double forceScale(final double pressure, final double reference, final double exponent) {
        if (!(pressure > 0.0D) || !(reference > 0.0D)) {
            return 1.0D;
        }
        if (exponent == 1.0D) {
            return pressure / reference;
        }
        return Math.exp(exponent * Math.log(pressure / reference));
    }

    public static double clampedForceScale(final double pressure, final double reference,
                                           final double exponent, final double min, final double max) {
        return WindMath.clamp(forceScale(pressure, reference, exponent), min, max);
    }
}
