package cn.chumenta.crosswind.wind;

import org.joml.Vector3d;

public final class WindModifiers {
    private WindModifiers() {
    }

    private static final double TICKS_PER_DAY = 24000.0D;

    static double diurnalOffset(final long dayTime, final double dayOffset, final double nightOffset,
                                final boolean flipForOcean) {
        if (dayOffset == 0.0D && nightOffset == 0.0D) {
            return 0.0D;
        }

        final double phase = (dayTime % 24000L) / TICKS_PER_DAY;

        final double peakPhase = 8.0D / 24.0D;

        final double swing = Math.cos(2.0D * Math.PI * (phase - peakPhase));

        final double mid = (dayOffset + nightOffset) * 0.5D;
        final double amp = (dayOffset - nightOffset) * 0.5D;
        final double offset = mid + amp * swing;
        return flipForOcean ? -offset : offset;
    }

    static double temperatureFluctuation(final long seed, final long gameTime,
                                         final double x, final double y, final double z,
                                         final double gridSize, final double amplitude) {
        if (amplitude <= 0.0D) {
            return 0.0D;
        }

        final long shortBucket = Math.floorDiv(gameTime, 200L);

        final double tempGrid = Math.max(1.0D, gridSize * 4.0D);
        final double n = WindMath.valueNoise(seed ^ 0x5DEECE66DL, shortBucket, 20,
                x, y, z, tempGrid);
        return amplitude * WindMath.clamp(n, -1.0D, 1.0D);
    }

    static double precipitationFluctuation(final long seed, final long gameTime,
                                           final double x, final double z,
                                           final double gridSize, final double amplitude) {
        if (amplitude <= 0.0D) {
            return 0.0D;
        }
        final long bucket = Math.floorDiv(gameTime, 400L);
        final double grid = Math.max(1.0D, gridSize * 8.0D);
        final double n = WindMath.valueNoise(seed ^ 0x27D4EB2F165667C5L, bucket, 21,
                x, 0.0D, z, grid);
        return amplitude * WindMath.clamp(n, -1.0D, 1.0D);
    }

    static double gustStrength(final long seed, final long timeBucket, final double timeFrac,
                               final double x, final double y, final double z,
                               final double gridSize, final double baseSpeed,
                               final double strength, final boolean enabled) {
        if (!enabled || strength <= 0.0D) {
            return 0.0D;
        }
        final double gustGrid = Math.max(1.0D, gridSize * 0.25D);

        final long gustSalt = timeBucket * 7919L + (long) (timeFrac * 8192.0D);

        final double n = WindMath.valueNoise(seed ^ 0x9E3779B9L, gustSalt, 3, x, y, z, gustGrid);

        final double t = (n + 1.0D) * 0.5D;

        return baseSpeed * strength * t * t;
    }

    static void applyTerrain(final Vector3d v, final TerrainSignature sig,
                             final TerrainSignature leeSig,
                             final double ridgeAccel, final double leeDecel,
                             final double leeRange, final double valleyDeflectionDeg,
                             final double maxSpeed) {
        if (sig.isEmpty()) {
            return;
        }

        final double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        if (speed < 1.0E-6D) {
            return;
        }

        final double relief = sig.relief();
        if (relief < 2.0D && sig.meanSlope() < 0.5D) {
            return;
        }

        final double nx = v.x / speed;
        final double nz = v.z / speed;

        double factor = 1.0D;
        if (ridgeAccel > 0.0D) {
            final double terrainStrength = WindMath.clamp(
                    sig.meanSlope() * 0.15D + sig.roughness() / 64.0D, 0.0D, 1.0D);
            factor += ridgeAccel * terrainStrength * 0.5D;
        }

        if (leeDecel > 0.0D && leeSig != null && !leeSig.isEmpty()) {
            final double upwindHeight = leeSig.meanHeight();
            final double hereHeight = sig.meanHeight();
            final double drop = upwindHeight - hereHeight;
            if (drop > 1.0D) {
                final double decay = Math.exp(-Math.max(0.0D, drop) / Math.max(1.0D, leeRange));
                factor -= leeDecel * WindMath.clamp(decay, 0.0D, 1.0D);
            }
        }

        factor = WindMath.clamp(factor, 0.35D, 1.6D);

        double deflection = 0.0D;
        if (valleyDeflectionDeg > 0.0D && relief >= 2.0D) {
            final double windBearing = WindMath.wrapDegrees(Math.toDegrees(Math.atan2(nx, nz)));
            final double axis = sig.ridgeAxisDeg();

            double diff = Math.abs(WindMath.deltaDegrees(windBearing, axis)) % 180.0D;
            if (diff > 90.0D) {
                diff = 180.0D - diff;
            }

            final double alignment = 1.0D - WindMath.clamp(diff / 90.0D, 0.0D, 1.0D);
            deflection = valleyDeflectionDeg * alignment * alignment;

            final double toward = WindMath.deltaDegrees(windBearing, axis);
            deflection *= (toward < 0.0D) ? -1.0D : 1.0D;
        }

        final double scaled = Math.min(speed * factor, Math.max(0.0D, maxSpeed));
        final double newBearing = Math.toDegrees(Math.atan2(nx, nz)) + deflection;
        final double rad = Math.toRadians(newBearing);
        v.x = Math.sin(rad) * scaled;
        v.z = Math.cos(rad) * scaled;
    }

    static Vector3d baseWind(final long seed, final int dimensionSalt, final long gameTime,
                             final long transitionTicks,
                             final double baseSpeed, final double maxSpeed,
                             final double gridSize, final double x, final double z,
                             final Vector3d out) {
        final long bucket = Math.floorDiv(gameTime, Math.max(1L, transitionTicks));
        final double frac = (gameTime - bucket * Math.max(1L, transitionTicks))
                / (double) Math.max(1L, transitionTicks);

        final double dirA = directionFor(seed, dimensionSalt, bucket, x, z, gridSize);
        final double dirB = directionFor(seed, dimensionSalt, bucket + 1L, x, z, gridSize);

        final double blend = WindMath.smoothStep(0.0D, 0.5D, frac);
        final double dir = WindMath.lerpDegrees(blend, dirA, dirB);

        final double speedA = speedFor(seed, dimensionSalt, bucket, baseSpeed, maxSpeed);
        final double speedB = speedFor(seed, dimensionSalt, bucket + 1L, baseSpeed, maxSpeed);
        final double speed = WindMath.lerp(blend, speedA, speedB);

        final double rad = Math.toRadians(dir);

        out.set(Math.sin(rad) * speed, 0.0D, Math.cos(rad) * speed);
        return out;
    }

    private static double directionFor(final long seed, final int dimensionSalt, final long bucket,
                                       final double x, final double z, final double gridSize) {
        final double n = WindMath.valueNoise(seed, bucket, 10, dimensionSalt * 1024.0D, 0.0D, 0.0D,
                Math.max(1.0D, gridSize * 8.0D));

        final double local = WindMath.valueNoise(seed, bucket, 11, x, 0.0D, z,
                Math.max(1.0D, gridSize * 16.0D));
        final double combined = WindMath.clamp(n * 0.75D + local * 0.25D, -1.0D, 1.0D);
        return WindMath.wrapDegrees(combined * 180.0D);
    }

    private static double speedFor(final long seed, final int dimensionSalt, final long bucket,
                                   final double baseSpeed, final double maxSpeed) {
        final double lo = Math.min(baseSpeed, maxSpeed);
        final double hi = Math.max(baseSpeed, maxSpeed);
        final double n = WindMath.valueNoise(seed, bucket, 12, dimensionSalt * 2048.0D, 0.0D, 0.0D, 1.0D);
        final double s = WindMath.clamp((n + 1.0D) * 0.5D, 0.0D, 1.0D);

        final double span = hi - lo;
        if (span <= 1.0E-9D) {
            return lo;
        }

        final double r = (FRESH_BREEZE_MPS - lo) / span;
        final double p;
        if (r <= 1.0E-6D) {
            p = 1.0D;
        } else if (r >= 1.0D) {
            p = 3.0D;
        } else {
            p = WindMath.clamp(Math.log(r) / Math.log(1.0D - TARGET_TAIL), 1.0D, 40.0D);
        }

        return lo + span * Math.pow(s, p);
    }

    static final double FRESH_BREEZE_MPS = 10.7D;

    static final double TARGET_TAIL = 0.22D;

    static final double STRONG_WIND_KNEE_MPS = 15.0D;

    static final double STRONG_WIND_TAIL = 0.55D;

    static double compressStrongWind(final double speedMps) {
        if (speedMps <= STRONG_WIND_KNEE_MPS) {
            return speedMps;
        }
        return STRONG_WIND_KNEE_MPS + (speedMps - STRONG_WIND_KNEE_MPS) * STRONG_WIND_TAIL;
    }

    public static final double TERRAIN_FADE_START = 48.0D;

    public static final double TERRAIN_FADE_END = 192.0D;

    public static double terrainAltitudeWeight(final double heightAboveTerrain) {
        final double h = Math.max(0.0D, heightAboveTerrain);
        if (h <= TERRAIN_FADE_START) {
            return 1.0D;
        }

        final double t = (h - TERRAIN_FADE_START) / (TERRAIN_FADE_END - TERRAIN_FADE_START);
        return WindMath.clamp(1.0D - t, 0.0D, 1.0D);
    }

    public static double fadeTerrainFactor(final double terrainFactor, final double weight) {
        final double w = WindMath.clamp(weight, 0.0D, 1.0D);
        return 1.0D + (terrainFactor - 1.0D) * w;
    }

    public static final double PRESSURE_WIND_ONSET = 0.5D;

    public static final double PRESSURE_WIND_STRENGTH = 6.0D;

    public static final double PRESSURE_WIND_EXPONENT = 2.0D;

    public static double pressureWindMultiplier(final double pressureRatio) {
        if (Double.isNaN(pressureRatio) || Double.isInfinite(pressureRatio)
                || pressureRatio < 0.0D) {
            return 1.0D;
        }

        if (pressureRatio == 0.0D) {
            return 0.0D;
        }
        if (pressureRatio >= PRESSURE_WIND_ONSET) {
            return 1.0D;
        }
        final double t = (PRESSURE_WIND_ONSET - pressureRatio) / PRESSURE_WIND_ONSET;
        return 1.0D + PRESSURE_WIND_STRENGTH * Math.pow(t, PRESSURE_WIND_EXPONENT);
    }
}
