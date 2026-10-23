package cn.chumenta.crosswind.wind;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class WindFieldService {
    private final TerrainCache terrainCache = new TerrainCache();

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
        final WindDynamics dynamics = captureDynamics(level, x, y, z, settings);
        return WindFieldCalculator.sample(captureSamplingEnvironment(level, x, y, z, dynamics), dynamics);
    }

    public WindStages sampleDetailed(final Level level, final double x, final double y, final double z,
                                     final WindSettings settings) {
        if (!settings.enabled()) {
            return WindFieldCalculator.sampleDetailed(null, new WindDynamics(
                    0L, 0L, 0, 0L, 0L, 1.0D, 0.0D, 0.0D, 0.0D, settings));
        }
        final WindDynamics dynamics = captureDynamics(level, x, y, z, settings);
        return WindFieldCalculator.sampleDetailed(
                captureSamplingEnvironment(level, x, y, z, dynamics), dynamics);
    }

    /** 在世界所属主线程调用。 */
    public WindDynamics captureDynamics(final Level level, final double x, final double y,
                                         final double z) {
        return captureDynamics(level, x, y, z, WindSettings.fromConfig());
    }

    /** 在世界所属主线程调用。 */
    public WindDynamics captureDynamics(final Level level, final double x, final double y,
                                         final double z, final WindSettings settings) {
        final long gameTime = level.getGameTime();
        final WeatherSeedState state = WeatherSeedState.get(level);
        final double pressure = settings.enabled()
                && (settings.pressureAffectsWind() || settings.pressureAffectsForce())
                ? WindPressure.sample(level, x, y, z) : 0.0D;
        return new WindDynamics(gameTime, level.getDayTime(), dimensionSalt(level),
                state.seed(), state.previousSeed(), state.transitionRatio(gameTime),
                WindMath.clamp(level.getRainLevel(1.0F), 0.0D, 1.0D),
                WindMath.clamp(level.getThunderLevel(1.0F), 0.0D, 1.0D), pressure, settings);
    }

    /** 在世界所属主线程捕获站点及其周围 7×7 区块。 */
    public WindEnvironment captureEnvironment(final Level level, final double x, final double y,
                                               final double z) {
        final int chunkX = (int) Math.floor(x) >> 4;
        final int chunkZ = (int) Math.floor(z) >> 4;
        final Map<Long, TerrainSignature> signatures = new HashMap<>();
        for (int dz = -3; dz <= 3; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                final int cx = chunkX + dx;
                final int cz = chunkZ + dz;
                signatures.put(ChunkPos.asLong(cx, cz), this.terrainCache.get(level, cx, cz));
            }
        }
        return new WindEnvironment(x, y, z, captureClimate(level, x, y, z), signatures);
    }

    private WindEnvironment captureSamplingEnvironment(final Level level, final double x,
                                                        final double y, final double z,
                                                        final WindDynamics dynamics) {
        final WindSettings settings = dynamics.settings();
        final WindEnvironment.ClimateSample climate = settings.useBiomeTemperature()
                ? captureClimate(level, x, y, z)
                : new WindEnvironment.ClimateSample("", 0.0D, 0.0D, false);
        final Map<Long, TerrainSignature> signatures = new HashMap<>();
        final WindEnvironment position = new WindEnvironment(x, y, z, climate, Map.of());
        if (settings.terrainActive()) {
            final long localKey = WindFieldCalculator.chunkKey(x, z);
            final TerrainSignature sig = this.terrainCache.get(level, (int) localKey, (int) (localKey >> 32));
            signatures.put(localKey, sig);
            if (!sig.isEmpty() && settings.leeDecel() > 0.0D
                    && WindModifiers.terrainAltitudeWeight(y - sig.meanHeight()) > 1.0E-6D) {
                captureUpwind(level, position, dynamics, dynamics.seed(), signatures);
                if (dynamics.transitionRatio() < 1.0D) {
                    captureUpwind(level, position, dynamics, dynamics.previousSeed(), signatures);
                }
            }
        }
        return new WindEnvironment(x, y, z, climate, signatures);
    }

    private void captureUpwind(final Level level, final WindEnvironment position,
                               final WindDynamics dynamics, final long seed,
                               final Map<Long, TerrainSignature> signatures) {
        final Long key = WindFieldCalculator.upwindKey(position.x(), position.z(),
                WindFieldCalculator.baseWind(position, dynamics, seed));
        if (key != null && !signatures.containsKey(key)) {
            signatures.put(key, this.terrainCache.get(level, (int) (long) key, (int) (key >> 32)));
        }
    }

    private static WindEnvironment.ClimateSample captureClimate(final Level level, final double x,
                                                                 final double y, final double z) {
        final LevelChunk chunk = level.getChunkSource().getChunkNow(
                ((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4);
        if (chunk == null) {
            return new WindEnvironment.ClimateSample("", 0.0D, 0.0D, false);
        }
        final int qx = ((int) Math.floor(x)) >> 2;
        final int qy = ((int) Math.floor(y)) >> 2;
        final int qz = ((int) Math.floor(z)) >> 2;
        final Holder<Biome> biome = chunk.getNoiseBiome(qx & 3, qy & 3, qz & 3);
        final float raw = biome.value().getBaseTemperature();
        final String id = biome.unwrapKey().map(key -> key.location().toString()).orElse("(inline)");
        final boolean ocean = biome.is(BiomeTags.IS_OCEAN);
        return new WindEnvironment.ClimateSample(id, raw, biomeToCelsius(raw), ocean);
    }

    static double biomeToCelsius(final double rawBiomeTemperature) {
        return WindFieldCalculator.biomeToCelsius(rawBiomeTemperature);
    }

    static double precipitationFrom(final double rainLevel, final double thunderLevel) {
        return WindFieldCalculator.precipitationFrom(rainLevel, thunderLevel);
    }

    public record ClimateSample(String biomeId, double rawBiomeTemperature, double convertedC,
                                boolean isOcean) {
    }

    private static int dimensionSalt(final Level level) {
        return level.dimension().location().toString().hashCode();
    }
}
