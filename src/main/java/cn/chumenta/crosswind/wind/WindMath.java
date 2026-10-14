package cn.chumenta.crosswind.wind;

public final class WindMath {
    private WindMath() {
    }

    private static final long MIX_A = 0x9E3779B97F4A7C15L;
    private static final long MIX_B = 0xBF58476D1CE4E5B9L;
    private static final long MIX_C = 0x94D049BB133111EBL;
    private static final long MIX_D = 0x2545F4914F6CDD1DL;

    public static long mix(final long a, final long b) {
        long h = a + MIX_A;
        h = (h ^ (h >>> 30)) * MIX_B;
        h ^= b + MIX_C + (h << 6) + (h >>> 2);
        h = (h ^ (h >>> 27)) * MIX_C;
        h ^= h >>> 31;
        h *= MIX_D;
        h ^= h >>> 33;
        return h;
    }

    public static long mix(final long a, final long b, final long c) {
        return mix(mix(a, b), c);
    }

    public static long mix(final long a, final long b, final long c, final long d) {
        return mix(mix(a, b), mix(c, d));
    }

    public static double hashToUnit(final long a, final long b) {
        final long h = mix(a, b);

        return ((h >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    public static double hashToUnit(final long a, final long b, final long c) {
        return hashToUnit(mix(a, b), c);
    }

    public static double fade(final double t) {
        return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
    }

    public static double lerp(final double delta, final double start, final double end) {
        return start + delta * (end - start);
    }

    public static double biLerp(final double u, final double v,
                                final double v00, final double v10,
                                final double v01, final double v11) {
        return lerp(v, lerp(u, v00, v10), lerp(u, v01, v11));
    }

    public static double triLerp(final double u, final double v, final double w,
                                 final double v000, final double v100,
                                 final double v010, final double v110,
                                 final double v001, final double v101,
                                 final double v011, final double v111) {
        return lerp(w,
                biLerp(u, v, v000, v100, v010, v110),
                biLerp(u, v, v001, v101, v011, v111));
    }

    public static double valueNoise(final long seed, final long salt, final int component,
                                    final double x, final double y, final double z,
                                    final double gridSize) {
        final double gx = x / gridSize;
        final double gy = y / gridSize;
        final double gz = z / gridSize;

        final long i = (long) Math.floor(gx);
        final long j = (long) Math.floor(gy);
        final long k = (long) Math.floor(gz);

        final double u = fade(gx - i);
        final double v = fade(gy - j);
        final double w = fade(gz - k);

        final long base = mix(seed, salt, component);

        return triLerp(u, v, w,
                hashToUnit(base, mix(i,     j,     k)),
                hashToUnit(base, mix(i + 1, j,     k)),
                hashToUnit(base, mix(i,     j + 1, k)),
                hashToUnit(base, mix(i + 1, j + 1, k)),
                hashToUnit(base, mix(i,     j,     k + 1)),
                hashToUnit(base, mix(i + 1, j,     k + 1)),
                hashToUnit(base, mix(i,     j + 1, k + 1)),
                hashToUnit(base, mix(i + 1, j + 1, k + 1)));
    }

    public static double wrapDegrees(final double degrees) {
        final double d = degrees % 360.0D;
        return d < 0.0D ? d + 360.0D : d;
    }

    public static double deltaDegrees(final double from, final double to) {
        final double d = wrapDegrees(to - from);
        return d > 180.0D ? d - 360.0D : d;
    }

    public static double lerpDegrees(final double delta, final double start, final double end) {
        return wrapDegrees(start + delta * deltaDegrees(start, end));
    }

    public static double rotateX(final double x, final double z, final double degrees) {
        final double r = Math.toRadians(degrees);
        final double c = Math.cos(r);
        final double s = Math.sin(r);
        return x * c - z * s;
    }

    public static double rotateZ(final double x, final double z, final double degrees) {
        final double r = Math.toRadians(degrees);
        final double c = Math.cos(r);
        final double s = Math.sin(r);
        return x * s + z * c;
    }

    public static double smoothStep(final double edge0, final double edge1, final double t) {
        if (edge0 == edge1) {
            return t < edge0 ? 0.0D : 1.0D;
        }
        final double x = clamp((t - edge0) / (edge1 - edge0), 0.0D, 1.0D);
        return fade(x);
    }

    public static double clamp(final double v, final double min, final double max) {
        return v < min ? min : (v > max ? max : v);
    }

    public static int clamp(final int v, final int min, final int max) {
        return v < min ? min : (v > max ? max : v);
    }
}
