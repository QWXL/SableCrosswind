package cn.chumenta.crosswind.wind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DensityMathTest {
    private static final double P_REF = 1.0D;

    @Test
    @DisplayName("★ 标准状态（基准气压 + 15 ℃）下密度比恰好为 1")
    void referenceStateIsUnity() {
        assertEquals(1.0D, DensityMath.densityRatio(P_REF, P_REF, 15.0D), 1.0E-9D);
        assertEquals(1.0D, DensityMath.temperatureRatio(15.0D), 1.0E-9D);
    }

    @Test
    @DisplayName("★ 温度项：同样气压下热天更稀薄（旧实现漏掉的正是这一项）")
    void hotterAirIsThinner() {
        final double cold = DensityMath.densityRatio(P_REF, P_REF, -20.0D);
        final double mild = DensityMath.densityRatio(P_REF, P_REF, 15.0D);
        final double hot = DensityMath.densityRatio(P_REF, P_REF, 45.0D);

        assertTrue(cold > mild, "冷天密度应更大：" + cold + " vs " + mild);
        assertTrue(hot < mild, "热天密度应更小：" + hot + " vs " + mild);

        assertEquals(0.9057D, hot, 0.002D, "45 ℃ 的温度项应约为 0.906");
    }

    @Test
    @DisplayName("★ 气压项：气压越低密度越小（用户直觉的物理版本）")
    void lowerPressureIsThinner() {
        final double low = DensityMath.densityRatio(0.8D, P_REF, 15.0D);
        final double mid = DensityMath.densityRatio(1.0D, P_REF, 15.0D);
        final double high = DensityMath.densityRatio(1.3D, P_REF, 15.0D);

        assertTrue(low < mid && mid < high, "密度应随气压单调增");
        assertEquals(0.8D, low, 1.0E-9D);
        assertEquals(1.3D, high, 1.0E-9D);
    }

    @Test
    @DisplayName("★ 风速主导：v 是平方项、ρ 是一次项 —— 风速加倍比气压加倍影响大得多")
    void windSpeedDominatesDensity() {
        final double rho0 = 1.225D;
        final double v0 = 10.0D;
        final double q0 = 0.5D * rho0 * v0 * v0;

        final double qDoubleSpeed = 0.5D * rho0 * (v0 * 2.0D) * (v0 * 2.0D);
        final double qDoubleDensity = 0.5D * (rho0 * 2.0D) * v0 * v0;

        assertEquals(4.0D, qDoubleSpeed / q0, 1.0E-9D, "风速加倍 ⇒ 风压 ×4");
        assertEquals(2.0D, qDoubleDensity / q0, 1.0E-9D, "密度加倍 ⇒ 风压 ×2（等温下即气压加倍）");
        assertTrue(qDoubleSpeed > qDoubleDensity, "风速的影响更大 —— 用户的判断是对的");
    }

    @Test
    @DisplayName("退化输入不把风力打成 0（宁可'不缩放'也不要'风消失'）")
    void degenerateInputsFallBackToUnity() {
        assertEquals(1.0D, DensityMath.densityRatio(0.0D, P_REF, 15.0D), 1.0E-9D);
        assertEquals(1.0D, DensityMath.densityRatio(-5.0D, P_REF, 15.0D), 1.0E-9D);
        assertEquals(1.0D, DensityMath.densityRatio(P_REF, 0.0D, 15.0D), 1.0E-9D);

        assertEquals(1.0D, DensityMath.temperatureRatio(-300.0D), 1.0E-9D);
        assertEquals(0.5D, DensityMath.densityRatio(0.5D, P_REF, -300.0D), 1.0E-9D,
                "温度无效时气压项仍应保留");
    }

    @Test
    @DisplayName("温度项被钳制在 [0.5, 2.0]，但真实地表温度不会被钳到")
    void temperatureRatioIsClampedButRealWeatherIsNot() {
        assertEquals(DensityMath.MAX_TEMPERATURE_RATIO, DensityMath.temperatureRatio(-200.0D), 1.0E-9D);
        assertEquals(DensityMath.MIN_TEMPERATURE_RATIO, DensityMath.temperatureRatio(5000.0D), 1.0E-9D);

        for (double c = -60.0D; c <= 60.0D; c += 5.0D) {
            final double r = DensityMath.temperatureRatio(c);
            assertTrue(r > DensityMath.MIN_TEMPERATURE_RATIO + 1.0E-6D
                            && r < DensityMath.MAX_TEMPERATURE_RATIO - 1.0E-6D,
                    "真实温度不该被钳到 @" + c + " ℃ → " + r);
        }
    }

    @Test
    @DisplayName("密度比对气压与温度都单调（不被钳制削出倒挂）")
    void monotonicInBothInputs() {
        double prev = -1.0D;
        for (double p = 0.1D; p <= 3.0D; p += 0.1D) {
            final double r = DensityMath.densityRatio(p, P_REF, 15.0D);
            assertTrue(r > prev, "对气压应严格单调增 @" + p);
            prev = r;
        }
        prev = Double.MAX_VALUE;
        for (double c = -100.0D; c <= 200.0D; c += 10.0D) {
            final double r = DensityMath.densityRatio(P_REF, P_REF, c);
            assertTrue(r <= prev + 1.0E-9D, "对温度应单调不增 @" + c);
            prev = r;
        }
    }
}
