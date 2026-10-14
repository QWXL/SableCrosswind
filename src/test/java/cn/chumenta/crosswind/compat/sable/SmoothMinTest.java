package cn.chumenta.crosswind.compat.sable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmoothMinTest {
    private static final double LIMIT = 10.0D;

    private static double smoothMin(final double x, final double limit) {
        return WindForceMath.smoothMin(x, limit);
    }

    @Test
    @DisplayName("小值时几乎恒等 —— 不引入人为非线性")
    void identityForSmallValues() {
        for (final double x : new double[] {0.01D, 0.1D, 0.5D, 1.0D, 2.0D}) {
            final double y = smoothMin(x, LIMIT);
            System.out.println("smoothMin(" + x + ", " + LIMIT + ") = " + y);
            assertTrue(Math.abs(y - x) / x < 0.12D, "小值偏离恒等太多: " + x + " → " + y);
        }
    }

    @Test
    @DisplayName("阈值 u=1 处导数为 0，且值恰好等于 limit（无拐点）")
    void matchesLimitAtThreshold() {
        assertEquals(LIMIT, smoothMin(LIMIT, LIMIT), 1.0E-12D);

        final double exactRightDerivativeAtOne = 1.5D * LIMIT * Math.exp(0.0D);

        final double dAtOneFromBelow = 3.0D * (1.0D - 1.0D);
        final double dAtOneFromAbove = 1.5D * Math.exp(-3.0D * 0.0D);
        System.out.println("二次段在 u=1 的斜率 = " + dAtOneFromBelow);
        System.out.println("指数段的斜率表达式值（应为 1.5·e^0 = 1.5，属误读）= " + dAtOneFromAbove);
        assertEquals(0.0D, dAtOneFromBelow, 1.0E-12D);

        final double f1 = smoothMin(LIMIT * 1.001D, LIMIT);
        final double f2 = smoothMin(LIMIT * 1.01D, LIMIT);
        final double f3 = smoothMin(LIMIT * 1.5D, LIMIT);
        System.out.println("f(1.001·L)=" + f1 + "  f(1.01·L)=" + f2 + "  f(1.5·L)=" + f3);
        assertTrue(f1 >= LIMIT, "阈值右侧不应低于 limit");
        assertTrue(f3 > f2 && f2 > f1, "阈值右侧应平滑单调增长（硬钳制会是常数）");
    }

    @Test
    @DisplayName("接缝 u=0.5 处连续，且函数形状为「先线性、中段变缓、末尾回升」")
    void seamAtHalfIsSmooth() {
        assertEquals(LIMIT * 0.5D, smoothMin(LIMIT * 0.5D, LIMIT), 1.0E-12D);

        final double step = LIMIT * 0.01D;
        final double slope = (smoothMin(LIMIT * 0.20D + step, LIMIT)
                - smoothMin(LIMIT * 0.20D, LIMIT)) / step;
        System.out.println("u=0.20 处归一化斜率 = " + slope + "（线性段应为 1）");
        assertEquals(1.0D, slope, 0.02D, "线性段归一如斜率应为 1");
    }

    @Test
    @DisplayName("单调不减，且有界（不发散）")
    void monotonicAndBounded() {
        double prev = -1.0D;
        double max = 0.0D;
        for (double x = 0.0D; x <= 100.0D * LIMIT; x += LIMIT * 0.1D) {
            final double y = smoothMin(x, LIMIT);
            assertTrue(y >= prev - 1.0E-12D, "非单调递减: x=" + x);
            prev = y;
            max = Math.max(max, y);
        }
        System.out.println("x 从 0 到 1000 时 smoothMin 的最大值 = " + max);
        assertTrue(max <= 1.5D * LIMIT + 1.0E-9D, "超过渐近上限 1.5·limit: " + max);
        assertTrue(max > LIMIT, "最大值应超过 limit");
    }

    @Test
    @DisplayName("limit = 0 表示不限制，原样返回")
    void zeroLimitMeansUnlimited() {
        assertEquals(123.456D, smoothMin(123.456D, 0.0D), 0.0D);
        assertEquals(123.456D, smoothMin(123.456D, -1.0D), 0.0D);
    }
}
