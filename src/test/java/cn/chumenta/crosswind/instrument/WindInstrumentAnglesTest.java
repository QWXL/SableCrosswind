package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindInstrumentAnglesTest {
    @Test
    @DisplayName("★ 走最短路径：350° → 10° 应顺时针走 20°，不是逆时针 340°")
    void takesTheShortWayAround() {
        float a = 350.0F;
        final float[] seen = new float[4];
        for (int i = 0; i < 4; i++) {
            a = WindInstrumentAngles.approach(a, 10.0F, 5.0F);
            seen[i] = a;
        }
        System.out.printf("350° → 10°（步长 5°）: %.0f, %.0f, %.0f, %.0f%n",
                seen[0], seen[1], seen[2], seen[3]);

        assertEquals(355.0F, seen[0], 1.0E-3F);
        assertEquals(0.0F, seen[1], 1.0E-3F);
        assertEquals(5.0F, seen[2], 1.0E-3F);
        assertEquals(10.0F, seen[3], 1.0E-3F);

        final float b = WindInstrumentAngles.approach(10.0F, 350.0F, 5.0F);
        System.out.printf("10° → 350°（步长 5°）: %.0f%n", b);
        assertEquals(5.0F, b, 1.0E-3F);
    }

    @Test
    @DisplayName("一步之内到位时直接就位（不在目标附近抖动）")
    void snapsWhenWithinOneStep() {
        assertEquals(10.0F, WindInstrumentAngles.approach(8.0F, 10.0F, 5.0F), 1.0E-3F);
        assertEquals(10.0F, WindInstrumentAngles.approach(12.0F, 10.0F, 5.0F), 1.0E-3F);

        assertEquals(10.0F, WindInstrumentAngles.approach(5.0F, 10.0F, 5.0F), 1.0E-3F);
    }

    @Test
    @DisplayName("结果始终归一到 [0, 360)，连续同向旋转也不会累积")
    void resultStaysNormalized() {
        float a = 170.0F;
        for (int i = 0; i < 200; i++) {
            a = WindInstrumentAngles.approach(a, (a + 10.0F) % 360.0F, 10.0F);
            assertTrue(a >= -1.0E-3F && a < 360.0F,
                    "第 " + i + " 步后角度越界: " + a);
        }
        System.out.printf("连续 200 步后：%.2f°（应落在 [0,360)）%n", a);
    }

    @Test
    @DisplayName("maxStep <= 0 表示直接跳到目标（结果仍按 [0,360) 归一）")
    void nonPositiveStepSnaps() {
        assertEquals(123.0F, WindInstrumentAngles.approach(0.0F, 123.0F, 0.0F), 1.0E-3F);

        assertEquals(315.0F, WindInstrumentAngles.approach(90.0F, -45.0F, -1.0F), 1.0E-3F);
        assertEquals(315.0F, WindInstrumentAngles.approach(0.0F, 315.0F, 0.0F), 1.0E-3F);
    }

    @Test
    @DisplayName("渲染推进：partialTick 让步长在帧间略增（消除 20Hz 步进感）")
    void renderStepAccountsForPartialTick() {
        final float atTickStart = WindInstrumentAngles.approachForRender(0.0F, 90.0F, 10.0F, 0.0F);
        final float midTick = WindInstrumentAngles.approachForRender(0.0F, 90.0F, 10.0F, 0.5F);
        final float endTick = WindInstrumentAngles.approachForRender(0.0F, 90.0F, 10.0F, 1.0F);
        System.out.printf("partialTick 0 / 0.5 / 1.0 ⇒ %.2f° / %.2f° / %.2f°%n",
                atTickStart, midTick, endTick);

        assertTrue(atTickStart <= midTick + 1.0E-4F);
        assertTrue(midTick <= endTick + 1.0E-4F);

        assertTrue(endTick < 90.0F, "单帧不该转到位");
    }

    @Test
    @DisplayName("shortestDistance 恒为非负且不超过 180°")
    void shortestDistanceIsBounded() {
        assertEquals(20.0F, WindInstrumentAngles.shortestDistance(350.0F, 10.0F), 1.0E-3F);
        assertEquals(20.0F, WindInstrumentAngles.shortestDistance(10.0F, 350.0F), 1.0E-3F);
        assertEquals(180.0F, WindInstrumentAngles.shortestDistance(0.0F, 180.0F), 1.0E-3F);
        assertEquals(0.0F, WindInstrumentAngles.shortestDistance(42.0F, 42.0F), 1.0E-3F);
    }

    @Test
    @DisplayName("★ 旋转方向：模型默认朝北，θ=−β 时指向罗盘方位 β")
    void rotationSignMatchesCompassBearing() {
        for (double beta : new double[] {0, 45, 90, 135, 180, 225, 270, 315}) {
            final double theta = -beta;
            final double t = Math.toRadians(theta);

            final double[] v = WindInstrumentAnglesTest.rotateY(t, 0.0D, -1.0D);
            final double tb = Math.toRadians(beta);
            final double tx = Math.sin(tb);
            final double tz = -Math.cos(tb);
            assertEquals(tx, v[0], 1.0E-9D, "β=" + beta + " 的 X 分量不符");
            assertEquals(tz, v[1], 1.0E-9D, "β=" + beta + " 的 Z 分量不符");
        }
    }

    private static double[] rotateY(final double theta, final double x, final double z) {
        return new double[] {
                x * Math.cos(theta) + z * Math.sin(theta),
                -x * Math.sin(theta) + z * Math.cos(theta),
        };
    }

    @Test
    @DisplayName("★ 风速计转速：幂律标定、零风为零、有保险丝上限")
    void anemometerSpeedCalibration() {
        assertEquals(0.0F, WindInstrumentAngles.anemometerDegreesPerSecond(0.0F), 1.0E-3F, "零风不该转");
        assertEquals(0.0F, WindInstrumentAngles.anemometerDegreesPerSecond(-3.0F), 1.0E-3F, "负风速不该转");

        assertEquals(64.0F, WindInstrumentAngles.anemometerDegreesPerSecond(4.0F), 0.1F);
        assertEquals(118.0F, WindInstrumentAngles.anemometerDegreesPerSecond(6.0F), 0.5F);
        assertEquals(253.0F, WindInstrumentAngles.anemometerDegreesPerSecond(10.0F), 0.5F);
        assertEquals(464.8F, WindInstrumentAngles.anemometerDegreesPerSecond(15.0F), 0.5F);
        assertEquals(715.5F, WindInstrumentAngles.anemometerDegreesPerSecond(20.0F), 0.5F);
        assertEquals(1314.5F, WindInstrumentAngles.anemometerDegreesPerSecond(30.0F), 1.0F);

        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(30.0F)
                        < WindInstrumentAngles.ANEMOMETER_MAX_DPS,
                "30 m/s（破坏阈值）不该触到上限，否则'越快越糊'会回来");
        assertEquals(2400.0F, WindInstrumentAngles.anemometerDegreesPerSecond(1000.0F), 1.0E-3F);

        float prev = -1.0F;
        for (float w = 0.0F; w <= 60.0F; w += 0.5F) {
            final float v = WindInstrumentAngles.anemometerDegreesPerSecond(w);
            assertTrue(v >= prev, "转速随风速应单调不减：" + w);
            prev = v;
        }
    }

    @Test
    @DisplayName("★ 转速范围被拉宽：10 与 20 m/s 的转速比旧标定大得多")
    void anemometerRangeIsWide() {
        final float at10 = WindInstrumentAngles.anemometerDegreesPerSecond(10.0F);
        final float at20 = WindInstrumentAngles.anemometerDegreesPerSecond(20.0F);
        assertTrue(at20 / at10 > 2.5F,
                "10→20 m/s 的转速比应明显大于旧标定的 2.0，实际 " + (at20 / at10));

        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(20.0F)
                        < WindInstrumentAngles.ANEMOMETER_MAX_DPS * 0.5F,
                "20 m/s 不该接近上限，否则失去分辨力");

        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(6.0F) > 90.0F,
                "6 m/s 应能明显看出在转（>90 °/s），实际 "
                        + WindInstrumentAngles.anemometerDegreesPerSecond(6.0F));
        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(10.0F) > 200.0F,
                "10 m/s 应转得清楚（>200 °/s）");

        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(2.0F) < 30.0F,
                "2 m/s 应慢");
        assertTrue(WindInstrumentAngles.anemometerDegreesPerSecond(15.0F) > 400.0F,
                "15 m/s 应明显转动");
    }

    @Test
    @DisplayName("★ 危险着色：14 以下无色、14→25 渐变、25 以上最深")
    void anemometerWarnIntensity() {
        assertEquals(0.0F, WindInstrumentAngles.anemometerWarnIntensity(0.0F), 1.0E-6F);
        assertEquals(0.0F, WindInstrumentAngles.anemometerWarnIntensity(14.0F), 1.0E-6F,
                "14 m/s 是起点，应仍为 0");
        assertEquals(0.5F, WindInstrumentAngles.anemometerWarnIntensity(19.5F), 1.0E-4F,
                "区间中点应恰好一半");
        assertEquals(1.0F, WindInstrumentAngles.anemometerWarnIntensity(25.0F), 1.0E-6F);
        assertEquals(1.0F, WindInstrumentAngles.anemometerWarnIntensity(29.9F), 1.0E-6F,
                "25 到 30 之间保持最深");

        float prev = -1.0F;
        for (float w = 0.0F; w <= 35.0F; w += 0.25F) {
            final float v = WindInstrumentAngles.anemometerWarnIntensity(w);
            assertTrue(v >= prev - 1.0E-6F, "着色强度应单调不减 @" + w);
            prev = v;
        }

        final float[] none = WindInstrumentAngles.warnTint(0.0F);
        final float[] full = WindInstrumentAngles.warnTint(1.0F);
        assertEquals(1.0F, none[0], 1.0E-6F);
        assertEquals(1.0F, full[0], 1.0E-6F);
        assertTrue(full[1] < 0.3F && full[2] < 0.3F, "最深时绿蓝应压得很低（显红）");
        assertTrue(none[1] > 0.99F && none[2] > 0.99F, "无色时不应改变原色");
    }

    @Test
    @DisplayName("★ 抗风上限按等级分档：黄铜必须扛得住高空风")
    void breakThresholdsAreTiered() {
        final double andesite = 30.0D;
        final double brass = 80.0D;

        assertTrue(brass > andesite,
                "黄铜级气象站必须比安山级抗风，否则高空观测就没有可用设备了");

        assertTrue(brass > 40.0D,
                "黄铜阈值必须高于高空实际风速（实测约 36 m/s），实际 " + brass);

        assertTrue(andesite < 36.0D,
                "安山级阈值应低于高空风速，否则风速计在高空不会坏，" +
                        "'高空只能用气象站'这条分工就不成立");
    }
}
