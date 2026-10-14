package cn.chumenta.crosswind.wind;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public final class WindFieldService {
    private final TerrainCache terrainCache = new TerrainCache();

    private final Vector3d tmpBase = new Vector3d();
    private final Vector3d tmpMod = new Vector3d();

    public TerrainCache terrainCache() {
        return this.terrainCache;
    }

    public WindSample sample(final Level level, final double x, final double y, final double z) {
        return sample(level, x, y, z, WindSettings.fromConfig());
    }

    public WindSample sample(final Level level, final Vec3 pos) {
        return sample(level, pos.x, pos.y, pos.z);
    }

    public WindSample sample(final Level level, final Vec3 pos, final WindSettings settings) {
        return sample(level, pos.x, pos.y, pos.z, settings);
    }

    public WindSample sample(final Level level, final BlockPos pos) {
        return sample(level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
    }

    public WindSample sample(final Level level, final double x, final double y, final double z,
                             final WindSettings settings) {
        if (!settings.enabled()) {
            return WindSample.CALM;
        }
        return compute(level, x, y, z, settings).toSample();
    }

    public WindStages sampleDetailed(final Level level, final double x, final double y, final double z,
                                     final WindSettings settings) {
        if (!settings.enabled()) {
            return new WindStages(new Vector3d(), 1.0D, false,

                    0.0D,
                    0.0D,
                    new Vector3d(),
                    0.0D, 0.0D, 0.0D, 0.0D,
                    0.0D, 0.0D,
                    0.0D, 1.0D,

                    "", 0.0D, false, false);
        }
        return compute(level, x, y, z, settings);
    }

    private WindStages compute(final Level level, final double x, final double y, final double z,
                               final WindSettings settings) {
        final long gameTime = level.getGameTime();
        final int dimSalt = dimensionSalt(level);

        final WeatherSeedState state = WeatherSeedState.get(level);
        final double ratio = state.transitionRatio(gameTime);

        final boolean blending = ratio < 1.0D;

        final long seed = state.seed();
        final long prevSeed = state.previousSeed();

        final TerrainSignature sig = settings.terrainActive()
                ? this.terrainCache.get(level, new ChunkPos((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4))
                : TerrainSignature.empty(0L);

        final WindStages stageSeed = computeForSeed(
                level, x, y, z, settings, seed, dimSalt, gameTime, sig);
        if (!blending) {
            return stageSeed;
        }

        final WindStages stagePrev = computeForSeed(
                level, x, y, z, settings, prevSeed, dimSalt, gameTime, sig);

        final double t = WindMath.fade(ratio);

        final Vector3d mixedBase = new Vector3d(stagePrev.baseVelocity()).lerp(stageSeed.baseVelocity(), t);
        final Vector3d mixedFinal = new Vector3d(stagePrev.finalVelocity()).lerp(stageSeed.finalVelocity(), t);

        return new WindStages(
                mixedBase,
                WindMath.lerp(t, stagePrev.terrainFactor(), stageSeed.terrainFactor()),
                stageSeed.terrainApplied(),

                stageSeed.terrainReferenceHeight(),
                WindMath.lerp(t, stagePrev.gustSpeed(), stageSeed.gustSpeed()),
                mixedFinal,
                stageSeed.temperatureC(),
                stageSeed.temperatureBiomeBaseC(),
                stageSeed.temperatureDiurnalC(),
                stageSeed.temperatureFluctuationC(),
                stageSeed.precipitation(),
                stageSeed.precipitationBase(),

                stageSeed.pressure(),
                stageSeed.pressureForceScale(),
                stageSeed.biomeId(),
                stageSeed.rawBiomeTemperature(),
                stageSeed.isOcean(),

                stageSeed.climateAvailable());
    }

    private WindStages computeForSeed(final Level level, final double x, final double y, final double z,
                                      final WindSettings settings, final long seed, final int dimSalt,
                                      final long gameTime, final TerrainSignature sig) {
        final long transitionTicks = Math.max(1L, settings.transitionTicks());
        final long bucket = Math.floorDiv(gameTime, transitionTicks);
        final double frac = (gameTime - bucket * transitionTicks) / (double) transitionTicks;

        WindModifiers.baseWind(seed, dimSalt, gameTime, transitionTicks,
                settings.baseSpeed(), settings.maxSpeed(), settings.gridSize(), x, z, this.tmpBase);

        final double baseSpeed = this.tmpBase.length();
        this.tmpMod.set(this.tmpBase);

        double terrainFactor = 1.0D;
        boolean terrainApplied = false;

        double terrainWeight = 0.0D;
        if (settings.terrainActive() && !sig.isEmpty()) {
            final double heightAboveTerrain = y - sig.meanHeight();
            terrainWeight = WindModifiers.terrainAltitudeWeight(heightAboveTerrain);

            if (terrainWeight > 1.0E-6D) {
                terrainApplied = true;
                final double speedBefore = this.tmpMod.length();
                final TerrainSignature leeSig = settings.leeDecel() > 0.0D
                        ? this.upwindSignature(level, x, z, this.tmpMod)
                        : null;
                WindModifiers.applyTerrain(this.tmpMod, sig, leeSig,
                        settings.ridgeAccel(), settings.leeDecel(),
                        settings.leeRange(), settings.valleyDeflectionDeg(),
                        settings.maxSpeed());
                final double speedAfter = this.tmpMod.length();

                final double rawFactor = speedBefore < 1.0E-9D ? 1.0D : speedAfter / speedBefore;

                terrainFactor = WindModifiers.fadeTerrainFactor(rawFactor, terrainWeight);
                if (rawFactor > 1.0E-9D && terrainFactor != rawFactor) {
                    this.tmpMod.mul(terrainFactor / rawFactor);
                }
            }
        }


        double gustSpeed = 0.0D;
        if (settings.gustActive()) {
            final double currentSpeed = this.tmpMod.length();
            final double gustDelta = WindModifiers.gustStrength(seed, bucket, frac, x, y, z,
                    settings.gridSize(), baseSpeed, settings.gustStrength(), true);
            if (currentSpeed > 1.0E-9D && gustDelta > 0.0D) {
                this.tmpMod.mul((currentSpeed + gustDelta) / currentSpeed);

                final double cappedPeak = WindModifiers.compressStrongWind(currentSpeed + gustDelta);
                final double cappedSustained = WindModifiers.compressStrongWind(currentSpeed);
                gustSpeed = Math.max(0.0D, cappedPeak - cappedSustained);
            } else if (currentSpeed <= 1.0E-9D) {
                gustSpeed = 0.0D;
            }
        }

        if (settings.pressureAffectsWind()) {
            final double pressureForWind = WindPressure.sample(level, x, y, z);
            final double ratio = settings.pressureReference() > 0.0D
                    ? pressureForWind / settings.pressureReference() : 1.0D;
            final double windScale = WindModifiers.pressureWindMultiplier(ratio);
            if (windScale != 1.0D) {
                if (this.tmpMod.lengthSquared() > 1.0E-18D) {
                    this.tmpMod.mul(windScale);
                }

                gustSpeed *= windScale;
            }
        }

        final double rawFinal = this.tmpMod.length();
        final double cappedFinal = WindModifiers.compressStrongWind(rawFinal);
        if (rawFinal > 1.0E-9D && cappedFinal < rawFinal) {
            this.tmpMod.mul(cappedFinal / rawFinal);
        }

        final double temperatureC;
        final double temperatureBiomeBaseC;
        final double temperatureDiurnalC;
        final double temperatureFluctuationC;
        final String biomeId;
        final double biomeRawTemp;
        final boolean isOcean;
        if (settings.useBiomeTemperature()) {
            final ClimateSample climate = biomeTemperatureC(level, x, y, z);

            temperatureBiomeBaseC = climate.convertedC();
            temperatureDiurnalC = WindModifiers.diurnalOffset(
                    level.getDayTime(),
                    settings.temperatureDayOffset(), settings.temperatureNightOffset(),
                    climate.isOcean());
            temperatureFluctuationC = WindModifiers.temperatureFluctuation(
                    seed, gameTime, x, y, z, settings.gridSize(), settings.temperatureFluctuation());

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

        final double rain = WindMath.clamp(level.getRainLevel(1.0F), 0.0D, 1.0D);
        final double thunder = WindMath.clamp(level.getThunderLevel(1.0F), 0.0D, 1.0D);

        final double precipitationBase = precipitationFrom(rain, thunder);
        double precipitation = precipitationBase;

        if (precipitation > 0.0D && settings.precipitationFluctuation() > 0.0D) {
            final double pf = WindModifiers.precipitationFluctuation(
                    seed, gameTime, x, z, settings.gridSize(), settings.precipitationFluctuation());

            final double deviation = pf * precipitation;
            precipitation = WindMath.clamp(precipitation + deviation, 0.0D, 1.0D);
        }

        final double pressure = settings.pressureAffectsForce()
                ? WindPressure.sample(level, x, y, z)
                : 0.0D;
        final double pressureForceScale = settings.pressureAffectsForce()
                ? PressureMath.clampedForceScale(pressure,
                        settings.pressureReference(), settings.pressureExponent(),
                        settings.pressureMinFactor(), settings.pressureMaxFactor())
                : 1.0D;

        return new WindStages(
                new Vector3d(this.tmpBase),
                terrainFactor,
                terrainApplied,

                sig.isEmpty() ? 0.0D : sig.meanHeight(),
                gustSpeed,
                new Vector3d(this.tmpMod),
                temperatureC,
                temperatureBiomeBaseC,
                temperatureDiurnalC,
                temperatureFluctuationC,
                precipitation,
                precipitationBase,
                pressure,
                pressureForceScale,
                biomeId,
                biomeRawTemp,
                isOcean,

                !biomeId.isEmpty());
    }

    private TerrainSignature upwindSignature(final Level level, final double x, final double z,
                                             final Vector3d wind) {
        final double speed = Math.sqrt(wind.x * wind.x + wind.z * wind.z);
        if (speed < 1.0E-6D) {
            return null;
        }

        final double upX = x - (wind.x / speed) * 48.0D;
        final double upZ = z - (wind.z / speed) * 48.0D;
        final TerrainSignature lee = this.terrainCache.get(level, new ChunkPos(
                (int) Math.floor(upX) >> 4, (int) Math.floor(upZ) >> 4));
        return lee.isEmpty() ? null : lee;
    }

    private static ClimateSample biomeTemperatureC(final Level level, final double x, final double y,
                                                   final double z) {
        final LevelChunk chunk = level.getChunkSource().getChunkNow(
                ((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
        if (chunk == null) {
            return new ClimateSample("", 0.0D, 0.0D, false);
        }

        final int qx = ((int) Math.floor(x)) >> 2;
        final int qy = ((int) Math.floor(y)) >> 2;
        final int qz = ((int) Math.floor(z)) >> 2;
        final Holder<Biome> biome = chunk.getNoiseBiome(qx & 3, qy & 3, qz & 3);
        final float raw = biome.value().getBaseTemperature();

        final String id = biome.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("(inline)");

        final boolean ocean = biome.is(BiomeTags.IS_OCEAN);

        return new ClimateSample(id, raw, biomeToCelsius(raw), ocean);
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
        return WindMath.clamp(
                rain * RAIN_PRECIPITATION + thunder * THUNDER_PRECIPITATION, 0.0D, 1.0D);
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

    public record ClimateSample(String biomeId, double rawBiomeTemperature, double convertedC,
                                boolean isOcean) {
    }

    private static int dimensionSalt(final Level level) {
        return level.dimension().location().toString().hashCode();
    }
}
