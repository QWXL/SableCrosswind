package cn.chumenta.crosswind.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class TerrainCache {
    private final Map<Long, TerrainSignature> signatures = new ConcurrentHashMap<>();

    private final AtomicLong version = new AtomicLong();

    public TerrainSignature get(final Level level, final int chunkX, final int chunkZ) {
        final long key = ChunkPos.asLong(chunkX, chunkZ);
        final TerrainSignature cached = this.signatures.get(key);
        if (cached != null) {
            return cached;
        }
        return this.rebuild(level, chunkX, chunkZ, key);
    }

    public TerrainSignature get(final Level level, final ChunkPos pos) {
        return this.get(level, pos.x, pos.z);
    }

    public TerrainSignature rebuild(final Level level, final int chunkX, final int chunkZ) {
        final long key = ChunkPos.asLong(chunkX, chunkZ);
        return this.rebuild(level, chunkX, chunkZ, key);
    }

    private TerrainSignature rebuild(final Level level, final int chunkX, final int chunkZ,
                                     final long key) {
        final LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        final long v = this.version.incrementAndGet();
        if (chunk == null) {
            return TerrainSignature.empty(v);
        }

        final Heightmap heightmap = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE);

        final int[] heights = new int[16 * 16];
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean anySolid = false;

        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                final int h = heightmap.getFirstAvailable(dx, dz);
                heights[dz * 16 + dx] = h;
                if (h > level.getMinBuildHeight()) {
                    anySolid = true;
                }
                if (h < minY) {
                    minY = h;
                }
                if (h > maxY) {
                    maxY = h;
                }
            }
        }

        if (!anySolid) {
            final TerrainSignature empty = TerrainSignature.empty(v);
            this.signatures.put(key, empty);
            return empty;
        }

        double sum = 0.0D;
        for (final int h : heights) {
            sum += h;
        }
        final double mean = sum / heights.length;

        double varSum = 0.0D;
        for (final int h : heights) {
            final double d = h - mean;
            varSum += d * d;
        }
        final float roughness = (float) Math.sqrt(varSum / heights.length);

        double slopeSum = 0.0D;
        int slopeCount = 0;
        for (int dz = 0; dz < 16; dz++) {
            for (int dx = 0; dx < 16; dx++) {
                final int h = heights[dz * 16 + dx];
                if (dx + 1 < 16) {
                    slopeSum += Math.abs(h - heights[dz * 16 + dx + 1]);
                    slopeCount++;
                }
                if (dz + 1 < 16) {
                    slopeSum += Math.abs(h - heights[(dz + 1) * 16 + dx]);
                    slopeCount++;
                }
            }
        }
        final float meanSlope = slopeCount == 0 ? 0.0F : (float) (slopeSum / slopeCount);

        final float ridgeAxisDeg = computeRidgeAxisDeg(heights);

        final TerrainSignature sig =
                new TerrainSignature(minY, maxY, meanSlope, ridgeAxisDeg, roughness, v);
        this.signatures.put(key, sig);
        return sig;
    }

    private static float computeRidgeAxisDeg(final int[] heights) {
        double sxx = 0.0D;
        double szz = 0.0D;
        double sxz = 0.0D;

        for (int dz = 1; dz < 15; dz++) {
            for (int dx = 1; dx < 15; dx++) {
                final double gx = (heights[dz * 16 + dx + 1] - heights[dz * 16 + dx - 1]) * 0.5D;
                final double gz = (heights[(dz + 1) * 16 + dx] - heights[(dz - 1) * 16 + dx]) * 0.5D;
                sxx += gx * gx;
                szz += gz * gz;
                sxz += gx * gz;
            }
        }

        if (sxx + szz < 1.0E-6D) {
            return 0.0F;
        }

        final double theta = 0.5D * Math.atan2(2.0D * sxz, sxx - szz);
        final double c = Math.cos(theta);
        final double s = Math.sin(theta);

        final double lambda1 = sxx * c * c + 2.0D * sxz * c * s + szz * s * s;

        final double lambda2 = sxx * s * s - 2.0D * sxz * c * s + szz * c * c;

        final double axisX;
        final double axisZ;
        if (lambda1 <= lambda2) {
            axisX = c;
            axisZ = s;
        } else {
            axisX = -s;
            axisZ = c;
        }

        double deg;
        if (Math.abs(axisX) < 1.0E-12D && Math.abs(axisZ) < 1.0E-12D) {
            deg = 0.0D;
        } else {
            deg = Math.toDegrees(Math.atan2(axisX, axisZ));
        }

        deg = WindMath.wrapDegrees(deg) % 180.0D;
        return (float) deg;
    }

    public void invalidate(final ChunkPos pos) {
        this.signatures.remove(ChunkPos.asLong(pos.x, pos.z));
    }

    public void invalidate(final BlockPos pos) {
        this.invalidate(new ChunkPos(pos));
    }

    public void invalidateAll() {
        this.signatures.clear();
    }

    public int size() {
        return this.signatures.size();
    }

    public long currentVersion() {
        return this.version.get();
    }
}
