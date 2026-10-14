package cn.chumenta.crosswind.instrument;

public final class PolePlacement {
    private PolePlacement() {
    }

    public static int findEnd(final int startY, final int step,
                              final java.util.function.IntPredicate scan) {
        int y = startY;
        while (scan.test(y + step)) {
            y += step;
        }
        return y;
    }

    public static int targetY(final int clickedY, final int step,
                              final java.util.function.IntPredicate scan) {
        return targetY(clickedY, step, scan, 3);
    }

    public static int targetY(final int clickedY, final int step,
                              final java.util.function.IntPredicate scan, final int mask) {
        if (step > 0 && (mask & 1) == 0) {
            return NO_PLACEMENT;
        }
        if (step < 0 && (mask & 2) == 0) {
            return NO_PLACEMENT;
        }

        if (!scan.test(clickedY)) {
            return NO_PLACEMENT;
        }

        final int end = findEnd(clickedY, step, scan);
        return end + step;
    }

    public static final int NO_PLACEMENT = Integer.MIN_VALUE;
}
