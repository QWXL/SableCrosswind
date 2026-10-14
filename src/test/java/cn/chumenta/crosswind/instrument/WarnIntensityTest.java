package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarnIntensityTest {
    private static final float EPS = 1.0E-4F;

    @Test
    @DisplayName("安山级曲线：14 以下不着色、25 以上满值、区间内线性")
    void andesiteCurveThresholds() {
        assertEquals(0.0F, WindInstrumentAngles.anemometerWarnIntensity(0.0F), EPS);
        assertEquals(0.0F, WindInstrumentAngles.anemometerWarnIntensity(13.9F), EPS,
                "14 m/s 是起点，之下不着色");
        assertEquals(0.0F, WindInstrumentAngles.anemometerWarnIntensity(14.0F), EPS);
        assertEquals(1.0F, WindInstrumentAngles.anemometerWarnIntensity(25.0F), EPS,
                "25 m/s 是满值点");
        assertEquals(1.0F, WindInstrumentAngles.anemometerWarnIntensity(30.0F), EPS,
                "30 m/s 会被吹坏，之前保持满值");
        assertEquals(0.5F, WindInstrumentAngles.anemometerWarnIntensity(19.5F), 0.01F,
                "中点应约 0.5");
    }

    @Test
    @DisplayName("强度始终落在 0..1，且随速度单调不减")
    void monotonicAndClamped() {
        float prev = -1.0F;
        for (float s = -5.0F; s <= 60.0F; s += 0.25F) {
            final float v = WindInstrumentAngles.anemometerWarnIntensity(s);
            assertTrue(v >= 0.0F && v <= 1.0F, "强度越界: " + v + " @ " + s);
            assertTrue(v >= prev - EPS, "在 " + s + " m/s 处强度回落了");
            prev = v;
        }
    }

    @Test
    @DisplayName("★ 着色是往纯红混（绿蓝一路压到 0），不是'从白到白'")
    void tintGoesToRed() {
        final float[] none = WindInstrumentAngles.warnTint(0.0F);
        assertEquals(1.0F, none[0], EPS);
        assertEquals(1.0F, none[1], EPS, "强度 0 时不改色");
        assertEquals(1.0F, none[2], EPS);

        final float[] full = WindInstrumentAngles.warnTint(1.0F);
        assertEquals(1.0F, full[0], EPS, "红分量保持 1");
        assertEquals(0.0F, full[1], EPS, "绿分量压到 0");
        assertEquals(0.0F, full[2], EPS, "蓝分量压到 0");

        final float[] mid = WindInstrumentAngles.warnTint(0.5F);
        assertTrue(mid[1] < 0.6F && mid[2] < 0.6F,
                "半强度时绿蓝必须明显下降（否则等于没着色）: " + mid[1] + "," + mid[2]);
    }

    @Test
    @DisplayName("强度越界时被夹住，不产生 NaN 或负色")
    void tintClampsIntensity() {
        for (final float bad : new float[]{-3.0F, 5.0F, Float.NaN}) {
            final float[] c = WindInstrumentAngles.warnTint(bad);
            for (final float v : c) {
                assertTrue(v >= 0.0F && v <= 1.0F, "分量越界: " + v + " (intensity=" + bad + ")");
            }
        }
    }
}
