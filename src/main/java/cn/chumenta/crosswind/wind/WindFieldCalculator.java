package cn.chumenta.crosswind.wind;

import org.joml.Vector3d;

public final class WindFieldCalculator {
    private WindFieldCalculator() {
    }

    public static WindSample sample(final WindEnvironment environment, final WindDynamics dynamics) {
        if (!dynamics.settings().enabled()) {
            return WindSample.CALM;
        }
        return sampleDetailed(environment, dynamics).toSample();
    }

    public static WindStages sampleDetailed(final WindEnvironment environment,
                                            final WindDynamics dynamics) {
        if (!dynamics.settings().enabled()) {
            return new WindStages(new Vector3d(), 1.0D, false,
                    0.0D, 0.0D, new Vector3d(),
                    0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D,
                    0.0D, 1.0D, "", 0.0D, false, false);
        }
        final TerrainSignature sig = dynamics.settings().terrainActive()
                ? signature(environment, chunkKey(environment.x(), environment.z()))
                : TerrainSignature.empty(0L);
        final WindStages stageSeed = computeForSeed(environment, dynamics, dynamics.seed(), sig);
        if (!(dynamics.transitionRatio() < 1.0D)) {
            return stageSeed;
        }
        final WindStages stagePrev = computeForSeed(environment, dynamics, dynamics.previousSeed(), sig);
        final double t = WindMath.fade(dynamics.transitionRatio());
        final Vector3d mixedBase = new Vector3d(stagePrev.baseVelocity()).lerp(stageSeed.baseVelocity(), t);
        final Vector3d mixedFinal = new Vector3d(stagePrev.finalVelocity()).lerp(stageSeed.finalVelocity(), t);
        return new WindStages(mixedBase,
                WindMath.lerp(t, stagePrev.terrainFactor(), stageSeed.terrainFactor()),
                stageSeed.terrainApplied(), stageSeed.terrainReferenceHeight(),
                WindMath.lerp(t, stagePrev.gustSpeed(), stageSeed.gustSpeed()), mixedFinal,
                stageSeed.temperatureC(), stageSeed.temperatureBiomeBaseC(),
                stageSeed.temperatureDiurnalC(), stageSeed.temperatureFluctuationC(),
                stageSeed.precipitation(), stageSeed.precipitationBase(),
                stageSeed.pressure(), stageSeed.pressureForceScale(),
                stageSeed.biomeId(), stageSeed.rawBiomeTemperature(), stageSeed.isOcean(),
                stageSeed.climateAvailable());
    }

    private static WindStages computeForSeed(final WindEnvironment environment,
                                             final WindDynamics dynamics, final long seed,
                                             final TerrainSignature sig) {
        final WindSettings settings = dynamics.settings();
        final double x = environment.x();
        final double y = environment.y();
        final double z = environment.z();
        final long gameTime = dynamics.gameTime();
        final long transitionTicks = Math.max(1L, settings.transitionTicks());
        final long bucket = Math.floorDiv(gameTime, transitionTicks);
        final double frac = (gameTime - bucket * transitionTicks) / (double) transitionTicks;
        final Vector3d tmpBase = baseWind(environment, dynamics, seed);
        final double baseSpeed = tmpBase.length();
        final Vector3d tmpMod = new Vector3d(tmpBase);
        double terrainFactor = 1.0D;
        boolean terrainApplied = false;
        if (settings.terrainActive() && !sig.isEmpty()) {
            final double terrainWeight = WindModifiers.terrainAltitudeWeight(y - sig.meanHeight());
            if (terrainWeight > 1.0E-6D) {
                terrainApplied = true;
                final double speedBefore = tmpMod.length();
                final TerrainSignature leeSig = settings.leeDecel() > 0.0D
                        ? upwindSignature(environment, tmpMod) : null;
                WindModifiers.applyTerrain(tmpMod, sig, leeSig,
                        settings.ridgeAccel(), settings.leeDecel(),
                        settings.leeRange(), settings.valleyDeflectionDeg(), settings.maxSpeed());
                final double speedAfter = tmpMod.length();
                final double rawFactor = speedBefore < 1.0E-9D ? 1.0D : speedAfter / speedBefore;
                terrainFactor = WindModifiers.fadeTerrainFactor(rawFactor, terrainWeight);
                if (rawFactor > 1.0E-9D && terrainFactor != rawFactor) {
                    tmpMod.mul(terrainFactor / rawFactor);
                }
            }
        }
        double gustSpeed = 0.0D;
        if (settings.gustActive()) {
            final double currentSpeed = tmpMod.length();
            final double gustDelta = WindModifiers.gustStrength(seed, bucket, frac, x, y, z,
                    settings.gridSize(), baseSpeed, settings.gustStrength(), true);
            if (currentSpeed > 1.0E-9D && gustDelta > 0.0D) {
                tmpMod.mul((currentSpeed + gustDelta) / currentSpeed);
                final double cappedPeak = WindModifiers.compressStrongWind(currentSpeed + gustDelta);
                final double cappedSustained = WindModifiers.compressStrongWind(currentSpeed);
                gustSpeed = Math.max(0.0D, cappedPeak - cappedSustained);
            } else if (currentSpeed <= 1.0E-9D) {
                gustSpeed = 0.0D;
            }
        }
        if (settings.pressureAffectsWind()) {
            final double ratio = settings.pressureReference() > 0.0D
                    ? dynamics.pressure() / settings.pressureReference() : 1.0D;
            final double windScale = WindModifiers.pressureWindMultiplier(ratio);
            if (windScale != 1.0D) {
                if (tmpMod.lengthSquared() > 1.0E-18D) {
                    tmpMod.mul(windScale);
                }
                gustSpeed *= windScale;
            }
        }
        final double rawFinal = tmpMod.length();
        final double cappedFinal = WindModifiers.compressStrongWind(rawFinal);
        if (rawFinal > 1.0E-9D && cappedFinal < rawFinal) {
            tmpMod.mul(cappedFinal / rawFinal);
        }
        final double temperatureC;
        final double temperatureBiomeBaseC;
        final double temperatureDiurnalC;
        final double temperatureFluctuationC;
        final String biomeId;
        final double biomeRawTemp;
        final boolean isOcean;
        if (settings.useBiomeTemperature()) {
            final WindEnvironment.ClimateSample climate = environment.climate();
            temperatureBiomeBaseC = climate.convertedC();
            temperatureDiurnalC = WindModifiers.diurnalOffset(dynamics.dayTime(),
                    settings.temperatureDayOffset(), settings.temperatureNightOffset(), climate.isOcean());
            temperatureFluctuationC = WindModifiers.temperatureFluctuation(seed, gameTime, x, y, z,
                    settings.gridSize(), settings.temperatureFluctuation());
            temperatureC = WindMath.clamp(
                    temperatureBiomeBaseC + temperatureDiurnalC + temperatureFluctuationC,
                    MIN_TEMPERATURE_C, MAX_TEMPERATURE_C);
            biomeId = climate.biomeId();
            biomeRawTemp = climate.rawBiomeTemperature();
            isOcean = climate.isOcean();
        } else {
            temperatureC = 0.0D;
            temperatureBiomeBaseC = 0.0D;
            temperatureDiurnalC = 0.0D;
            temperatureFluctuationC = 0.0D;
            biomeId = "";
            biomeRawTemp = 0.0D;
            isOcean = false;
        }
        final double precipitationBase = precipitationFrom(dynamics.rainLevel(), dynamics.thunderLevel());
        double precipitation = precipitationBase;
        if (precipitation > 0.0D && settings.precipitationFluctuation() > 0.0D) {
            final double pf = WindModifiers.precipitationFluctuation(seed, gameTime, x, z,
                    settings.gridSize(), settings.precipitationFluctuation());
            final double deviation = pf * precipitation;
            precipitation = WindMath.clamp(precipitation + deviation, 0.0D, 1.0D);
        }
        final double pressure = settings.pressureAffectsForce() ? dynamics.pressure() : 0.0D;
        final double pressureForceScale = settings.pressureAffectsForce()
                ? PressureMath.clampedForceScale(pressure, settings.pressureReference(),
                        settings.pressureExponent(), settings.pressureMinFactor(), settings.pressureMaxFactor())
                : 1.0D;
        return new WindStages(tmpBase, terrainFactor, terrainApplied,
                sig.isEmpty() ? 0.0D : sig.meanHeight(), gustSpeed, tmpMod,
                temperatureC, temperatureBiomeBaseC, temperatureDiurnalC, temperatureFluctuationC,
                precipitation, precipitationBase, pressure, pressureForceScale,
                biomeId, biomeRawTemp, isOcean, !biomeId.isEmpty());
    }

    static Vector3d baseWind(final WindEnvironment environment, final WindDynamics dynamics,
                             final long seed) {
        final WindSettings settings = dynamics.settings();
        return WindModifiers.baseWind(seed, dynamics.dimensionSalt(), dynamics.gameTime(),
                Math.max(1L, settings.transitionTicks()), settings.baseSpeed(), settings.maxSpeed(),
                settings.gridSize(), environment.x(), environment.z(), new Vector3d());
    }

    static Long upwindKey(final double x, final double z, final Vector3d wind) {
        final double speed = Math.sqrt(wind.x * wind.x + wind.z * wind.z);
        if (speed < 1.0E-6D) {
            return null;
        }
        return chunkKey(x - (wind.x / speed) * 48.0D, z - (wind.z / speed) * 48.0D);
    }

    private static TerrainSignature upwindSignature(final WindEnvironment environment,
                                                    final Vector3d wind) {
        final Long key = upwindKey(environment.x(), environment.z(), wind);
        if (key == null) {
            return null;
        }
        final TerrainSignature sig = signature(environment, key);
        return sig.isEmpty() ? null : sig;
    }

    static long chunkKey(final double x, final double z) {
        final int chunkX = (int) Math.floor(x) >> 4;
        final int chunkZ = (int) Math.floor(z) >> 4;
        return (chunkX & 0xFFFFFFFFL) | ((chunkZ & 0xFFFFFFFFL) << 32);
    }

    private static TerrainSignature signature(final WindEnvironment environment, final long key) {
        return environment.terrainSignatures().getOrDefault(key, TerrainSignature.empty(0L));
    }

    static double biomeToCelsius(final double rawBiomeTemperature) {
        final double raw = WindMath.clamp(rawBiomeTemperature, -0.8D, 1.2D);
        final double celsius;
        if (raw >= BASE_BIOME_TEMPERATURE) {
            celsius = BASE_TEMPERATURE_C + (raw - BASE_BIOME_TEMPERATURE) * WARM_SCALE;
        } else {
            final double coldness = BASE_BIOME_TEMPERATURE - raw;
            celsius = BASE_TEMPERATURE_C - COLD_GAIN * Math.pow(coldness, COLD_POWER);
        }
        return WindMath.clamp(celsius, MIN_TEMPERATURE_C, MAX_TEMPERATURE_C);
    }

    static double precipitationFrom(final double rainLevel, final double thunderLevel) {
        final double rain = WindMath.clamp(rainLevel, 0.0D, 1.0D);
        final double thunder = WindMath.clamp(thunderLevel, 0.0D, 1.0D);
        return WindMath.clamp(rain * RAIN_PRECIPITATION + thunder * THUNDER_PRECIPITATION, 0.0D, 1.0D);
    }

    private static final double BASE_BIOME_TEMPERATURE = 0.5D;
    private static final double BASE_TEMPERATURE_C = 13.0D;
    private static final double MIN_TEMPERATURE_C = -25.0D;
    private static final double MAX_TEMPERATURE_C = 35.0D;
    private static final double WARM_SCALE = 12.5D;
    private static final double COLD_GAIN = 20.8D;
    private static final double COLD_POWER = 0.55D;
    private static final double RAIN_PRECIPITATION = 0.5D;
    private static final double THUNDER_PRECIPITATION = 0.5D;
}
