package cn.chumenta.crosswind.wind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GustMathTest {
    @Test
    @DisplayName("★ C1：峰值 = 持续 + 分量，只加一次")
    void peakAddsComponentExactlyOnce() {
        assertEquals(12.0D, GustMath.peakSpeed(10.0D, 2.0D), 1.0E-12D);
        assertEquals(10.0D, GustMath.peakSpeed(10.0D, 0.0D), 1.0E-12D, "无阵风时峰值等于持续值");
        assertEquals(0.0D, GustMath.peakSpeed(0.0D, 0.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("★ C1 反例防护：绝不等同于 speed + (speed + gust)")
    void peakIsNeverDoubleCounted() {
        for (final double[] pair : new double[][]{{10.0D, 2.0D}, {5.0D, 0.5D}, {40.0D, 0.75D}}) {
            final double sustained = pair[0];
            final double gust = pair[1];
            final double correct = sustained + gust;
            final double wrong = sustained + (sustained + gust);
            assertEquals(correct, GustMath.peakSpeed(sustained, gust), 1.0E-12D,
                    "持续 " + sustained + " / 分量 " + gust
                            + " 时峰值应为 " + correct + "，而不是 " + wrong);
        }
    }

    @Test
    @DisplayName("分量与峰值可以互相转换（幂等）")
    void componentAndPeakRoundTrip() {
        assertEquals(2.0D, GustMath.componentFromPeak(10.0D, 12.0D), 1.0E-12D);

        for (final double gust : new double[]{0.0D, 0.75D, 3.0D, 12.5D}) {
            final double peak = GustMath.peakSpeed(7.0D, gust);
            assertEquals(gust, GustMath.componentFromPeak(7.0D, peak), 1.0E-12D,
                    "分量为 " + gust + " 时应能往返");
        }
    }

    @Test
    @DisplayName("退化输入不产生负值，也不会让峰值低于持续风速")
    void degenerateInputsAreGuarded() {
        assertEquals(10.0D, GustMath.peakSpeed(10.0D, -3.0D), 1.0E-12D);

        assertEquals(2.0D, GustMath.peakSpeed(-5.0D, 2.0D), 1.0E-12D);

        assertEquals(0.0D, GustMath.componentFromPeak(10.0D, 4.0D), 1.0E-12D);

        final double withNaN = GustMath.peakSpeed(10.0D, Double.NaN);
        org.junit.jupiter.api.Assertions.assertTrue(
                Double.isNaN(withNaN) || withNaN >= 10.0D,
                "NaN 分量要么透传，要么被当成 0；不该算出比持续风速更小的峰值，实际 " + withNaN);
    }
}
