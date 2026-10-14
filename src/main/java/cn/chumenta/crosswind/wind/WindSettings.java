package cn.chumenta.crosswind.wind;

import cn.chumenta.crosswind.CrosswindConfig;

public record WindSettings(
        boolean enabled,

        double baseSpeed,
        double maxSpeed,
        long transitionTicks,

        int gridSize,
        int cacheLimit,

        boolean terrainEnabled,
        double ridgeAccel,
        double leeDecel,
        double leeRange,
        double valleyDeflectionDeg,

        boolean gustEnabled,
        double gustStrength,

        boolean useBiomeTemperature,
        double temperatureDayOffset,
        double temperatureNightOffset,
        double temperatureFluctuation,
        double precipitationFluctuation,

        boolean pressureAffectsForce,
        boolean pressureAffectsWind,
        double pressureReference,
        double pressureExponent,
        double pressureMinFactor,
        double pressureMaxFactor,

        boolean sableEnabled,
        double sableMaxForcePerSubstep,
        double sableMassExponent,
        double sableMaxAccelerationG,
        double sableForceExponent,
        double sableForceMultiplier,
        boolean sableCoarseWind,
        double windDragCoefficient,
        double airDensityReference,
        boolean sableLogging
) {
    public static final WindSettings DISABLED = new WindSettings(
            false,
            0.0D, 0.0D, 600L,
            16, 65536,
            false, 0.0D, 0.0D, 64.0D, 0.0D,
            false, 0.0D,
            false, 0.0D, 0.0D, 0.0D, 0.0D,
            false, false, 1.0D, 1.0D, 0.25D, 4.0D,

            false,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            0.0D,
            false,
            0.0D,
            0.0D,
            false);

    public static WindSettings fromConfig() {
        return new WindSettings(
                CrosswindConfig.WIND_ENABLED.get(),
                CrosswindConfig.BASE_SPEED.get(),
                CrosswindConfig.MAX_SPEED.get(),
                CrosswindConfig.TRANSITION_TICKS.get().longValue(),
                CrosswindConfig.GRID_SIZE.get(),
                CrosswindConfig.CACHE_LIMIT.get(),
                CrosswindConfig.TERRAIN_ENABLED.get(),
                CrosswindConfig.RIDGE_ACCEL.get(),
                CrosswindConfig.LEE_DECEL.get(),
                CrosswindConfig.LEE_RANGE.get(),
                CrosswindConfig.VALLEY_DEFLECTION_DEG.get(),
                CrosswindConfig.GUST_ENABLED.get(),
                CrosswindConfig.GUST_STRENGTH.get(),
                CrosswindConfig.USE_BIOME_TEMPERATURE.get(),
                CrosswindConfig.TEMPERATURE_DAY_OFFSET.get(),
                CrosswindConfig.TEMPERATURE_NIGHT_OFFSET.get(),
                CrosswindConfig.TEMPERATURE_FLUCTUATION.get(),
                CrosswindConfig.PRECIPITATION_FLUCTUATION.get(),
                CrosswindConfig.PRESSURE_AFFECTS_FORCE.get(),
                CrosswindConfig.PRESSURE_AFFECTS_WIND.get(),
                CrosswindConfig.PRESSURE_REFERENCE.get(),
                CrosswindConfig.PRESSURE_EXPONENT.get(),
                CrosswindConfig.PRESSURE_MIN_FACTOR.get(),
                CrosswindConfig.PRESSURE_MAX_FACTOR.get(),
                CrosswindConfig.SABLE_ENABLED.get(),
                CrosswindConfig.SABLE_MAX_FORCE_PER_SUBSTEP.get(),
                CrosswindConfig.SABLE_MASS_EXPONENT.get(),
                CrosswindConfig.SABLE_MAX_ACCEL_G.get(),
                CrosswindConfig.SABLE_FORCE_EXPONENT.get(),
                CrosswindConfig.SABLE_FORCE_MULTIPLIER.get(),
                CrosswindConfig.SABLE_COARSE_WIND.get(),
                CrosswindConfig.SABLE_DRAG_COEFFICIENT.get(),
                CrosswindConfig.SABLE_AIR_DENSITY.get(),
                CrosswindConfig.SABLE_LOGGING.get());
    }

    public boolean terrainActive() {
        return this.enabled && this.terrainEnabled;
    }

    public boolean gustActive() {
        return this.enabled && this.gustEnabled && this.gustStrength > 0.0D;
    }
}
