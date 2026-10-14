package cn.chumenta.crosswind.instrument;

import cn.chumenta.crosswind.wind.WindMath;

public final class WindInstrumentAngles {
    private WindInstrumentAngles() {
    }

    public static float approach(final float current, final float target, final float maxStep) {
        final float delta = (float) WindMath.deltaDegrees(current, target);
        if (maxStep <= 0.0F || Math.abs(delta) <= maxStep) {
            return (float) WindMath.wrapDegrees(target);
        }
        return (float) WindMath.wrapDegrees(current + Math.signum(delta) * maxStep);
    }

    public static float approachForRender(final float current, final float target,
                                          final float maxStepPerTick, final float partialTick) {
        final float step = maxStepPerTick * Math.max(1.0F, 1.0F + partialTick);
        return approach(current, target, step);
    }

    public static float shortestDistance(final float a, final float b) {
        return (float) Math.abs(WindMath.deltaDegrees(a, b));
    }

    public static final double ANEMOMETER_DPS_COEFFICIENT = 8.0D;

    public static final double ANEMOMETER_DPS_EXPONENT = 1.5D;

    public static final float ANEMOMETER_MAX_DPS = 2400.0F;

    public static float anemometerDegreesPerSecond(final float windSpeedMps) {
        if (windSpeedMps <= 0.0F) {
            return 0.0F;
        }
        final double dps = ANEMOMETER_DPS_COEFFICIENT
                * Math.pow(windSpeedMps, ANEMOMETER_DPS_EXPONENT);
        return (float) Math.min(dps, ANEMOMETER_MAX_DPS);
    }

    public static final float ANEMOMETER_WARN_START_MPS = 14.0F;

    public static final float ANEMOMETER_WARN_FULL_MPS = 25.0F;

    public static float anemometerWarnIntensity(final float windSpeedMps) {
        if (windSpeedMps <= ANEMOMETER_WARN_START_MPS) {
            return 0.0F;
        }
        if (windSpeedMps >= ANEMOMETER_WARN_FULL_MPS) {
            return 1.0F;
        }
        return (windSpeedMps - ANEMOMETER_WARN_START_MPS)
                / (ANEMOMETER_WARN_FULL_MPS - ANEMOMETER_WARN_START_MPS);
    }

    public static float[] warnTint(final float intensity) {
        final float t = Float.isNaN(intensity)
                ? 0.0F
                : Math.max(0.0F, Math.min(1.0F, intensity));
        return new float[]{1.0F, 1.0F - t, 1.0F - t};
    }
}
