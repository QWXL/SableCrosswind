package cn.chumenta.crosswind.wind;

import org.joml.Vector3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindModifiersTest {
    private static final double GRID = 16.0D;

    private static List<Double> baseSpeeds(final int count, final double lo, final double hi) {
        final List<Double> out = new ArrayList<>();
        final Vector3d v = new Vector3d();
        for (int bucket = 0; bucket < count; bucket++) {
            WindModifiers.baseWind(12345L, 7, bucket * 600L, 600L, lo, hi, GRID, 0.0D, 0.0D, v);
            out.add(Math.sqrt(v.x * v.x + v.z * v.z));
        }
        return out;
    }

    @Test
    @DisplayName("基础风永远落在 [base_speed, max_speed] 内")
    void baseWindStaysInRange() {
        for (double s : baseSpeeds(400, 4.0D, 20.0D)) {
            assertTrue(s >= 4.0D - 1.0E-9D, "低于下限: " + s);
            assertTrue(s <= 20.0D + 1.0E-9D, "高于上限: " + s);
        }
    }

    @Test
    @DisplayName("基础风分布：常态落在轻风~清风，>10.7 m/s 是少数（回归测试）")
    void baseWindDistributionTargetsLightToFreshBreeze() {
        final List<Double> speeds = baseSpeeds(4000, 4.0D, 20.0D);
        final double mean = speeds.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        final double p50 = quantile(speeds, 0.50D);
        final double p90 = quantile(speeds, 0.90D);
        final double p99 = quantile(speeds, 0.99D);
        final long belowLight = speeds.stream().filter(s -> s < 1.6D).count();
        final long aboveFresh = speeds.stream().filter(s -> s > 10.7D).count();
        final double aboveFrac = aboveFresh / (double) speeds.size();

        System.out.printf("基础风：均值 %.2f  中位 %.2f  p90 %.2f  p99 %.2f | "
                        + "<1.6 占 %.1f%%  >10.7 占 %.1f%%%n",
                mean, p50, p90, p99,
                belowLight * 100.0D / speeds.size(), aboveFrac * 100.0D);

        assertTrue(p50 > 1.6D && p50 < 10.7D, "中位数不在轻风~清风区间内: " + p50);

        assertTrue(belowLight * 100.0D / speeds.size() < 1.0D,
                "出现过低于 1.6 m/s 的风况，与 base_speed 作为下界的设定不符");

        assertTrue(aboveFrac < 0.35D, "高于 10.7 m/s 的比例过大: " + aboveFrac);
        assertTrue(aboveFrac > 0.10D, "高于 10.7 m/s 的比例过小，风太平淡: " + aboveFrac);

        assertTrue(mean < 9.5D, "均值偏高，分布又堆向大风了: " + mean);
    }

    private static double quantile(final List<Double> values, final double q) {
        final List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compare);
        return sorted.get((int) (q * (sorted.size() - 1)));
    }

    @Test
    @DisplayName("地形修正不得把风速推过硬上限（回归测试）")
    void terrainRespectsSpeedCap() {
        final TerrainSignature rough = new TerrainSignature(60, 200, 8.0F, 45.0F, 90.0F, 1L);
        final Vector3d v = new Vector3d(0.0D, 0.0D, 20.0D);
        WindModifiers.applyTerrain(v, rough, null, 0.25D, 0.5D, 64.0D, 20.0D, 20.0D);
        final double after = Math.sqrt(v.x * v.x + v.z * v.z);
        System.out.printf("地形修正后：%.2f m/s（上限 20）%n", after);
        assertTrue(after <= 20.0D + 1.0E-6D, "地形修正突破了风速上限: " + after);
    }

    @Test
    @DisplayName("地形倍率本身被限制在 [0.35, 1.6]")
    void terrainFactorBounded() {
        final TerrainSignature high = new TerrainSignature(150, 200, 0.8F, 0.0F, 12.0F, 1L);
        final TerrainSignature low = new TerrainSignature(60, 64, 0.8F, 0.0F, 12.0F, 1L);

        final Vector3d vUp = new Vector3d(0.0D, 0.0D, 10.0D);
        WindModifiers.applyTerrain(vUp, high, null, 5.0D, 0.0D, 64.0D, 0.0D, 1.0E9D);
        final double upFactor = Math.sqrt(vUp.x * vUp.x + vUp.z * vUp.z) / 10.0D;

        final Vector3d vDown = new Vector3d(0.0D, 0.0D, 10.0D);
        WindModifiers.applyTerrain(vDown, low, high, 0.0D, 5.0D, 64.0D, 0.0D, 1.0E9D);
        final double downFactor = Math.sqrt(vDown.x * vDown.x + vDown.z * vDown.z) / 10.0D;

        System.out.printf("极端参数下地形倍率：加速向 %.3f，减速向 %.3f%n", upFactor, downFactor);

        assertTrue(upFactor > 1.0D, "加速向没有生效，检查测试数据是否走了早退分支: " + upFactor);
        assertTrue(downFactor < 1.0D, "减速向没有生效，检查测试数据是否走了早退分支: " + downFactor);
        assertTrue(upFactor <= 1.6D + 1.0E-6D, "倍率突破上界: " + upFactor);
        assertTrue(downFactor >= 0.35D - 1.0E-6D, "倍率跌破下界: " + downFactor);
    }

    @Test
    @DisplayName("零风时地形修正不做任何事")
    void terrainDoesNothingAtZeroWind() {
        final TerrainSignature rough = new TerrainSignature(60, 200, 8.0F, 45.0F, 90.0F, 1L);
        final Vector3d v = new Vector3d(0.0D, 0.0D, 0.0D);
        WindModifiers.applyTerrain(v, rough, null, 0.25D, 0.5D, 64.0D, 20.0D, 30.0D);
        assertEquals(0.0D, v.x, 1.0E-12D);
        assertEquals(0.0D, v.z, 1.0E-12D);
    }

    @Test
    @DisplayName("阵风恒非负，关闭时恒为 0")
    void gustIsNonNegative() {
        for (int i = 0; i < 500; i++) {
            final double g = WindModifiers.gustStrength(99L, i, 0.3D, i * 3.0D, 70.0D, i * 7.0D,
                    GRID, 10.0D, 0.35D, true);
            assertTrue(g >= 0.0D, "阵风为负: " + g);
            assertTrue(g <= 10.0D * 0.35D + 1.0E-9D, "阵风超过上限: " + g);
        }
        assertEquals(0.0D, WindModifiers.gustStrength(99L, 1L, 0.0D, 0.0D, 70.0D, 0.0D,
                GRID, 10.0D, 0.35D, false), 1.0E-12D);
    }

    @Test
    @DisplayName("气压缩放：与基准相等时为 1，退化输入不把风力打成 0")
    void pressureScaleBehaviour() {
        assertEquals(1.0D, PressureMath.forceScale(1.0D, 1.0D, 1.0D), 1.0E-12D);

        assertEquals(0.5D, PressureMath.forceScale(0.5D, 1.0D, 1.0D), 1.0E-12D);

        assertEquals(0.25D, PressureMath.forceScale(0.5D, 1.0D, 2.0D), 1.0E-12D);

        assertEquals(1.0D, PressureMath.forceScale(0.0D, 1.0D, 1.0D), 1.0E-12D);
        assertEquals(1.0D, PressureMath.forceScale(-5.0D, 1.0D, 1.0D), 1.0E-12D);
        assertEquals(1.0D, PressureMath.forceScale(1.0D, 0.0D, 1.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("气压缩放被上下限钳住（防炸求解器）")
    void pressureScaleIsClamped() {
        final double huge = PressureMath.clampedForceScale(1000.0D, 1.0D, 1.0D, 0.25D, 4.0D);
        System.out.printf("气压 ×1000 时施力倍数被钳到 ×%.2f%n", huge);
        assertEquals(4.0D, huge, 1.0E-9D);

        final double tiny = PressureMath.clampedForceScale(1.0E-9D, 1.0D, 1.0D, 0.25D, 4.0D);
        assertEquals(0.25D, tiny, 1.0E-9D);
    }

    @Test
    @DisplayName("气压缩放单调：气压越高，风力越大")
    void pressureScaleIsMonotonic() {
        double prev = -1.0D;
        for (double p = 0.05D; p <= 5.0D; p += 0.05D) {
            final double f = PressureMath.clampedForceScale(p, 1.0D, 1.0D, 0.0D, 100.0D);
            assertTrue(f >= prev - 1.0E-12D, "气压升高而倍数下降 @" + p);
            prev = f;
        }
    }

    @Test
    @DisplayName("高度切变已移除（占位，防止误以为测试漏了）")
    void shearRemoved() {
        assertTrue(true);
    }

    @Test
    @DisplayName("★ 软压缩：拐点及以下【完全不变】，只压上尾")
    void softKneeLeavesNormalWindUntouched() {
        for (double v = 0.0D; v <= WindModifiers.STRONG_WIND_KNEE_MPS; v += 0.25D) {
            assertEquals(v, WindModifiers.compressStrongWind(v), 1.0E-12D,
                    "拐点以下不该被改动 @" + v);
        }

        final double knee = WindModifiers.STRONG_WIND_KNEE_MPS;
        assertEquals(knee, WindModifiers.compressStrongWind(knee), 1.0E-12D);
        assertTrue(WindModifiers.compressStrongWind(knee + 1.0E-9D) >= knee,
                "拐点右侧不能低于拐点（否则出现折角）");
    }

    @Test
    @DisplayName("★ 软压缩：单调不减 —— 风更大时压缩后仍更大")
    void softKneeIsMonotonic() {
        double prev = -1.0D;
        for (double v = 0.0D; v <= 60.0D; v += 0.05D) {
            final double c = WindModifiers.compressStrongWind(v);
            assertTrue(c >= prev - 1.0E-12D, "出现倒挂 @" + v + " → " + c + " < " + prev);
            prev = c;
        }
    }

    @Test
    @DisplayName("★ 软压缩：显著压低上尾，20 以上的出现率降到可忽略")
    void softKneeThinsTheTail() {
        final double lo = 4.0D;
        final double hi = 20.0D;
        final double r = (WindModifiers.FRESH_BREEZE_MPS - lo) / (hi - lo);
        final double p = Math.log(r) / Math.log(1.0D - WindModifiers.TARGET_TAIL);

        final int n = 200_000;
        final double[] raw = new double[n];
        final double[] comp = new double[n];
        for (int i = 0; i < n; i++) {
            final double s = (i + 0.5D) / n;
            raw[i] = lo + (hi - lo) * Math.pow(s, p);
            comp[i] = WindModifiers.compressStrongWind(raw[i]);
        }

        final double rawP99 = percentile(raw, 0.99D);
        final double compP99 = percentile(comp, 0.99D);
        final double rawP90 = percentile(raw, 0.90D);
        final double compP90 = percentile(comp, 0.90D);
        final double rawMax = percentile(raw, 1.0D);
        final double compMax = percentile(comp, 1.0D);
        System.out.printf("软压缩前后：p90 %.2f→%.2f  p99 %.2f→%.2f  max %.2f→%.2f%n",
                rawP90, compP90, rawP99, compP99, rawMax, compMax);

        assertTrue(compP99 < rawP99 - 1.0D, "p99 应明显下降");
        assertTrue(compMax < 18.0D, "压缩后的上界应低于 18 m/s，实际 " + compMax);

        assertTrue(Math.abs(compP90 - rawP90) < 0.5D,
                "p90 不该被明显改动: " + rawP90 + " → " + compP90);

        final long aboveKnee = java.util.Arrays.stream(comp)
                .filter(v -> v > WindModifiers.STRONG_WIND_KNEE_MPS).count();
        final double abovePct = 100.0D * aboveKnee / n;
        System.out.printf("压缩后 >%.0f m/s 占比 %.1f%%%n",
                WindModifiers.STRONG_WIND_KNEE_MPS, abovePct);
        assertTrue(abovePct < 12.0D, "大风应是少数，实际 " + abovePct + "%");
    }

    private static double percentile(final double[] values, final double q) {
        final double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);
        final int idx = (int) Math.round(q * (sorted.length - 1));
        return sorted[Math.max(0, Math.min(sorted.length - 1, idx))];
    }

    @Test
    @DisplayName("★ 气压→风速：100%~50% 区间【完全无影响】（乘数恒为 1）")
    void pressureHasNoEffectAboveOnset() {
        for (double p = 1.0D; p >= WindModifiers.PRESSURE_WIND_ONSET; p -= 0.01D) {
            assertEquals(1.0D, WindModifiers.pressureWindMultiplier(p), 1.0E-12D,
                    "起效点之上必须严格为 1.0，实际 @" + p);
        }

        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(1.0D), 1.0E-12D);

        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(1.5D), 1.0E-12D);
        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(10.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("★ 气压→风速：低于 40% 后明显增大（用户要求的形状）")
    void pressureEffectGrowsBelow40Percent() {
        final double at40 = WindModifiers.pressureWindMultiplier(0.40D);
        assertTrue(at40 > 1.15D && at40 < 1.35D, "0.40 处乘数应约 1.24，实际 " + at40);

        final double at30 = WindModifiers.pressureWindMultiplier(0.30D);
        final double at20 = WindModifiers.pressureWindMultiplier(0.20D);
        final double at10 = WindModifiers.pressureWindMultiplier(0.10D);
        assertTrue(at30 > at40 && at20 > at30 && at10 > at20, "乘数应随气压下降单调增");
        assertTrue(at30 > 1.8D, "0.30 处应接近 2 倍，实际 " + at30);
        assertTrue(at10 > 4.0D, "0.10 处应超过 4 倍，实际 " + at10);

        final double slopeShallow = WindModifiers.pressureWindMultiplier(0.44D)
                - WindModifiers.pressureWindMultiplier(0.46D);
        final double slopeDeep = WindModifiers.pressureWindMultiplier(0.20D)
                - WindModifiers.pressureWindMultiplier(0.22D);
        assertTrue(slopeDeep > slopeShallow, "曲线应加速增长（越深越陡）");
    }

    @Test
    @DisplayName("★ 气压→风速：在起效点连续，且对气压单调不增")
    void pressureCurveIsContinuousAndMonotonic() {
        final double justBelow = WindModifiers.pressureWindMultiplier(
                WindModifiers.PRESSURE_WIND_ONSET - 1.0E-9D);
        assertEquals(1.0D, justBelow, 1.0E-6D, "起效点左侧应趋于 1.0（连续）");
        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(WindModifiers.PRESSURE_WIND_ONSET),
                1.0E-12D);

        double prev = 0.0D;
        for (double p = 1.0D; p >= 0.001D; p -= 0.001D) {
            final double m = WindModifiers.pressureWindMultiplier(p);
            assertTrue(m >= prev - 1.0E-12D, "出现倒挂 @" + p + " → " + m + " < " + prev);
            prev = m;
        }

        assertEquals(1.0D + WindModifiers.PRESSURE_WIND_STRENGTH,
                WindModifiers.pressureWindMultiplier(1.0E-9D), 1.0E-6D,
                "趋近 0 的极限应为 1 + strength");
    }

    @Test
    @DisplayName("★ 气压严格为 0 ⇒ 【无风】：真空里没有空气，也就没有风")
    void pressureExactlyZeroMeansNoWind() {
        assertEquals(0.0D, WindModifiers.pressureWindMultiplier(0.0D), 1.0E-12D,
                "气压比为 0（真空）必须是 0 风速乘数");
    }

    @Test
    @DisplayName("★ 但【趋近 0 的极小正数】仍给最大加成（与严格 0 是两回事）")
    void tinyPositivePressureStillGetsMaxBoost() {
        assertEquals(1.0D + WindModifiers.PRESSURE_WIND_STRENGTH,
                WindModifiers.pressureWindMultiplier(1.0E-9D), 1.0E-6D,
                "1e-9 不是 0 —— 它应当拿到最大加成");
    }

    @Test
    @DisplayName("气压→风速：真正的无效输入才退化到不缩放")
    void pressureMultiplierDegenerateInputs() {
        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(-0.5D), 1.0E-12D,
                "负气压是无效输入 ⇒ 不缩放");
        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(Double.NaN), 1.0E-12D,
                "NaN 应退化到不缩放（而不是把风速变成 NaN）");
        assertEquals(1.0D, WindModifiers.pressureWindMultiplier(Double.POSITIVE_INFINITY),
                1.0E-12D, "无穷大应退化到不缩放");
    }

    @Test
    @DisplayName("★ 地形高度权重：低空满权、高空归零、中间平滑")
    void terrainAltitudeWeightShape() {
        final double start = WindModifiers.TERRAIN_FADE_START;
        final double end = WindModifiers.TERRAIN_FADE_END;

        assertEquals(1.0D, WindModifiers.terrainAltitudeWeight(0.0D), 1.0E-12D);
        assertEquals(1.0D, WindModifiers.terrainAltitudeWeight(start), 1.0E-12D);

        assertEquals(1.0D, WindModifiers.terrainAltitudeWeight(-100.0D), 1.0E-12D);

        assertEquals(0.0D, WindModifiers.terrainAltitudeWeight(end), 1.0E-12D);
        assertEquals(0.0D, WindModifiers.terrainAltitudeWeight(500.0D), 1.0E-12D,
                "Y=500 这种高度必须完全不受地形影响（用户实测报告的位置）");

        assertEquals(0.5D, WindModifiers.terrainAltitudeWeight((start + end) / 2.0D), 1.0E-9D);

        final double stepSlope = 1.0D / (end - start);

        double prev = WindModifiers.terrainAltitudeWeight(-50.0D);
        for (double h = -49.0D; h <= 400.0D; h += 1.0D) {
            final double w = WindModifiers.terrainAltitudeWeight(h);
            assertTrue(w <= prev + 1.0E-12D, "权重应对高度单调不增 @" + h);
            assertTrue(w >= 0.0D && w <= 1.0D, "权重必须落在 [0,1] @" + h);
            assertTrue(prev - w <= stepSlope + 1.0E-9D,
                    "相邻 1 格内权重变化超过线性斜率（断崖）@" + h + "：" + prev + " → " + w);
            prev = w;
        }
    }

    @Test
    @DisplayName("★ 地形倍率淡出：权重 0 时不加速也不减速（不是撤回加速）")
    void terrainFactorFadesToNeutral() {
        assertEquals(1.4D, WindModifiers.fadeTerrainFactor(1.4D, 1.0D), 1.0E-12D);

        assertEquals(1.0D, WindModifiers.fadeTerrainFactor(1.4D, 0.0D), 1.0E-12D);
        assertEquals(1.0D, WindModifiers.fadeTerrainFactor(0.6D, 0.0D), 1.0E-12D);

        assertEquals(1.2D, WindModifiers.fadeTerrainFactor(1.4D, 0.5D), 1.0E-12D);
        assertEquals(0.8D, WindModifiers.fadeTerrainFactor(0.6D, 0.5D), 1.0E-12D);

        assertEquals(1.4D, WindModifiers.fadeTerrainFactor(1.4D, 2.0D), 1.0E-12D);
        assertEquals(1.0D, WindModifiers.fadeTerrainFactor(1.4D, -1.0D), 1.0E-12D);
    }
}
