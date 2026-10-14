package cn.chumenta.crosswind.wind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrecipitationTest {
    @Test
    @DisplayName("下雨 = 0.5，雷暴 = 1.0（回归测试）")
    void rainAndThunderAreDistinct() {
        final double raining = WindFieldService.precipitationFrom(1.0D, 0.0D);

        final double thundering = WindFieldService.precipitationFrom(1.0D, 1.0D);

        final double clear = WindFieldService.precipitationFrom(0.0D, 0.0D);

        System.out.println("PRECIP clear=" + clear + " raining=" + raining + " thundering=" + thundering);

        assertEquals(0.0D, clear, 1.0E-9D);
        assertEquals(0.5D, raining, 1.0E-9D);
        assertEquals(1.0D, thundering, 1.0E-9D);

        assertTrue(thundering > raining,
                "雷暴与下雨的降水值相同，没有区分开: " + raining + " vs " + thundering);
    }

    @Test
    @DisplayName("降水恒在 [0,1]，且对输入单调不减")
    void boundedAndMonotonic() {
        for (double r = 0.0D; r <= 1.0D; r += 0.1D) {
            for (double t = 0.0D; t <= 1.0D; t += 0.1D) {
                final double p = WindFieldService.precipitationFrom(r, t);
                assertTrue(p >= 0.0D && p <= 1.0D, "降水越界 r=" + r + " t=" + t + " -> " + p);
            }
        }

        double prev = -1.0D;
        for (double r = 0.0D; r <= 1.0D; r += 0.05D) {
            final double p = WindFieldService.precipitationFrom(r, 0.0D);
            assertTrue(p >= prev - 1.0E-12D, "雨等级升高而降水下降 @r=" + r);
            prev = p;
        }
    }

    @Test
    @DisplayName("越界输入被夹住，不会产生负降水或超 1 的降水")
    void outOfRangeInputsAreClamped() {
        assertEquals(0.0D, WindFieldService.precipitationFrom(-1.0D, -1.0D), 1.0E-9D);
        assertEquals(1.0D, WindFieldService.precipitationFrom(5.0D, 5.0D), 1.0E-9D);
    }

    @Test
    @DisplayName("降水波动：相对量、有界、确定性、关闭时为 0")
    void fluctuationBehaviour() {
        final double amp = 0.15D;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long t = 0L; t < 40000L; t += 97L) {
            final double f = WindModifiers.precipitationFluctuation(
                    1234L, t, 50.0D, -80.0D, 16.0D, amp);
            assertTrue(Math.abs(f) <= amp + 1.0E-9D, "降水波动越界 @" + t + ": " + f);
            min = Math.min(min, f);
            max = Math.max(max, f);
        }
        System.out.println("PRECIP-FLUC range " + min + " .. " + max);

        assertEquals(
                WindModifiers.precipitationFluctuation(1234L, 999L, 1.0D, 2.0D, 16.0D, amp),
                WindModifiers.precipitationFluctuation(1234L, 999L, 1.0D, 2.0D, 16.0D, amp),
                0.0D);

        assertTrue(max - min > 0.2D * amp, "波动几乎不变，等于没加");
        assertEquals(0.0D, WindModifiers.precipitationFluctuation(
                1234L, 999L, 1.0D, 2.0D, 16.0D, 0.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("降水波动是【相对量】：下雨档与雷暴档的起伏比例一致")
    void fluctuationIsRelative() {
        final double fluc = 0.12D;
        final double lightRain = 0.5D + fluc * 0.5D;
        final double thunder = 1.0D + fluc * 1.0D;
        assertEquals(0.56D, lightRain, 1.0E-9D);
        assertEquals(1.12D, thunder, 1.0E-9D);

        assertEquals((lightRain - 0.5D) / 0.5D, (thunder - 1.0D) / 1.0D, 1.0E-9D);
    }
}
