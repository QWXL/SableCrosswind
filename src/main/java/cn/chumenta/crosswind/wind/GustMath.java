package cn.chumenta.crosswind.wind;

public final class GustMath {
    private GustMath() {
    }

    public static double peakSpeed(final double sustainedSpeedMps, final double gustComponentMps) {
        final double sustained = Math.max(0.0D, sustainedSpeedMps);
        return sustained + Math.max(0.0D, gustComponentMps);
    }

    public static double componentFromPeak(final double sustainedSpeedMps, final double peakSpeedMps) {
        final double sustained = Math.max(0.0D, sustainedSpeedMps);
        return Math.max(0.0D, peakSpeedMps - sustained);
    }
}
