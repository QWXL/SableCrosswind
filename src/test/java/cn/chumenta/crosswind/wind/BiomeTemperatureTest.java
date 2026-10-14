package cn.chumenta.crosswind.wind;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiomeTemperatureTest {
    private static final double MIN_C = -25.0D;
    private static final double MAX_C = 35.0D;

    private static double c(final double raw) {
        return WindFieldService.biomeToCelsius(raw);
    }

    @Test
    @DisplayName("海洋（0.50）是温和的 13 °C —— 既不能是 0（冰点）也不能是 20（热带）")
    void oceanIsTemperate() {
        final double ocean = c(0.50D);
        System.out.println("ocean 0.50 -> " + ocean);
        assertEquals(13.0D, ocean, 1.0E-6D);
        assertTrue(ocean >= 10.0D && ocean <= 15.0D,
                "海洋温度应落在 10~15 °C（温带海面量级），实际 " + ocean);
    }

    @Test
    @DisplayName("所有 frozen / snowy 群系都必须在零下（回归测试）")
    void frozenBiomesAreBelowFreezing() {
        final double frozenPeaks = c(-0.70D);
        final double snowyTaiga = c(-0.50D);
        final double snowySlopes = c(-0.30D);
        final double snowyBeach = c(0.05D);
        final double frozenOcean = c(0.00D);

        System.out.println("FROZEN frozen_peaks(-0.70) -> " + frozenPeaks);
        System.out.println("FROZEN snowy_taiga (-0.50) -> " + snowyTaiga);
        System.out.println("FROZEN snowy_slopes(-0.30) -> " + snowySlopes);
        System.out.println("FROZEN snowy_beach ( 0.05) -> " + snowyBeach);
        System.out.println("FROZEN frozen_ocean( 0.00) -> " + frozenOcean);

        assertTrue(frozenPeaks < 0.0D, "冰峰必须在零下: " + frozenPeaks);
        assertTrue(snowyTaiga < 0.0D, "雪岭针叶林必须在零下: " + snowyTaiga);
        assertTrue(snowySlopes < 0.0D, "积雪山坡必须在零下: " + snowySlopes);

        assertTrue(frozenOcean < 0.0D,
                "frozen_ocean 在零上 —— 说明映射又退回线性了: " + frozenOcean);
        assertTrue(snowyBeach < 0.0D, "snowy_beach 应在零下: " + snowyBeach);

        assertTrue(frozenPeaks < snowyTaiga, "frozen_peaks 应比 snowy_taiga 冷");
        assertTrue(snowyTaiga < snowySlopes, "snowy_taiga 应比 snowy_slopes 冷");
        assertTrue(snowySlopes < frozenOcean, "snowy_slopes 应比 frozen_ocean 冷");

        assertTrue(frozenOcean < snowyBeach,
                "frozen_ocean(0.00) 应比 snowy_beach(0.05) 冷: "
                        + frozenOcean + " vs " + snowyBeach);
    }

    @Test
    @DisplayName("最热的群系不超过 25 °C（回归测试）")
    void hottestBiomeIsReasonable() {
        final double desert = c(2.00D);
        final double jungle = c(0.95D);
        final double stonyPeaks = c(1.00D);
        System.out.println("HOT desert(2.00) -> " + desert + "  jungle(0.95) -> " + jungle
                + "  stony_peaks(1.00) -> " + stonyPeaks);

        assertTrue(desert <= MAX_C + 1.0E-9D, "沙漠温度超过硬上限: " + desert);
        assertTrue(desert <= 25.0D, "沙漠温度偏高（用户嫌 45 度太高）: " + desert);
        assertTrue(desert >= 18.0D, "沙漠温度过低，与温带拉不开差距: " + desert);
        assertTrue(jungle < desert, "丛林应比沙漠凉");
    }

    @Test
    @DisplayName("2.00 与 1.20 映射到同一个值（断崖被吃掉）")
    void cliffIsCollapsed() {
        assertEquals(c(1.20D), c(2.00D), 1.0E-9D);
    }

    @Test
    @DisplayName("整个映射单调不减，且落在 [-25, 35]")
    void monotonicAndBounded() {
        double prev = Double.NEGATIVE_INFINITY;
        for (double raw = -1.0D; raw <= 3.0D; raw += 0.05D) {
            final double v = c(raw);
            assertTrue(v >= prev - 1.0E-9D, "映射不单调 @" + raw + ": " + prev + " -> " + v);
            assertTrue(v >= MIN_C - 1.0E-9D && v <= MAX_C + 1.0E-9D, "越界 @" + raw + ": " + v);
            prev = v;
        }
    }

    @Test
    @DisplayName("接缝处（raw=0.5）值连续，不会跳变")
    void seamIsContinuous() {
        final double below = c(0.5D - 1.0E-9D);
        final double above = c(0.5D + 1.0E-9D);
        final double jump = Math.abs(above - below);
        System.out.println("SEAM below=" + below + " above=" + above + " jump=" + jump);

        assertTrue(jump < 0.01D, "接缝处温度跳变 " + jump + " °C");
    }

    @Test
    @DisplayName("各气候带的取值都能从报告里读得通")
    void realBiomesReadSensibly() {
        final double frozenPeaks = c(-0.70D);
        final double frozenOcean = c(0.00D);
        final double taiga = c(0.25D);
        final double ocean = c(0.50D);
        final double forest = c(0.70D);
        final double beach = c(0.80D);
        final double jungle = c(0.95D);

        System.out.println("BIOME frozen_peaks  raw=-0.70 -> " + frozenPeaks);
        System.out.println("BIOME frozen_ocean  raw= 0.00 -> " + frozenOcean);
        System.out.println("BIOME taiga         raw= 0.25 -> " + taiga);
        System.out.println("BIOME ocean         raw= 0.50 -> " + ocean);
        System.out.println("BIOME forest        raw= 0.70 -> " + forest);
        System.out.println("BIOME beach         raw= 0.80 -> " + beach);
        System.out.println("BIOME jungle        raw= 0.95 -> " + jungle);

        assertTrue(frozenPeaks >= -20.0D, "冰峰过冷，超出合理估值范围: " + frozenPeaks);
        assertTrue(frozenOcean < 0.0D && frozenOcean > -15.0D, "冻洋应在零下但不过分: " + frozenOcean);
        assertTrue(taiga > -5.0D && taiga < 5.0D, "针叶林应接近冰点: " + taiga);
        assertTrue(forest > ocean, "森林应比海洋暖");
        assertTrue(beach > forest, "海滩应比森林暖");
        assertTrue(jungle > beach, "丛林应比海滩暖");

        assertTrue(jungle - frozenPeaks < 45.0D, "气候带跨度过大: " + (jungle - frozenPeaks));
    }

    private static long atHour(final double hour) {
        return (long) (((hour - 6.0D + 24.0D) % 24.0D) * 1000.0D);
    }

    @Test
    @DisplayName("日变化：下午最热（14:00 峰值）、凌晨最冷，且从不越界")
    void diurnalPeaksInAfternoon() {
        final double day = 4.0D;
        final double night = -6.0D;
        final double at1400 = WindModifiers.diurnalOffset(atHour(14.0D), day, night, false);
        final double at1200 = WindModifiers.diurnalOffset(atHour(12.0D), day, night, false);
        final double at1600 = WindModifiers.diurnalOffset(atHour(16.0D), day, night, false);
        final double at0200 = WindModifiers.diurnalOffset(atHour(2.0D), day, night, false);
        final double at0600 = WindModifiers.diurnalOffset(atHour(6.0D), day, night, false);

        System.out.println("DIURNAL 14:00 " + at1400 + "  12:00 " + at1200 + "  16:00 " + at1600
                + "  2:00 " + at0200 + "  6:00 " + at0600);

        assertTrue(at1400 > at1200, "下午应比正午热（相位滞后）: " + at1400 + " vs " + at1200);
        assertTrue(at1400 > at1600, "峰值应在 14:00 而不是 16:00: " + at1400 + " vs " + at1600);
        assertTrue(at1400 > at0600, "下午应比清晨热");
        assertTrue(at0600 > at0200, "清晨应比凌晨热");
        assertTrue(at1400 > 0.0D, "下午应为正偏移");
        assertTrue(at0200 < 0.0D, "凌晨应为负偏移");

        assertEquals(day, at1400, 0.01D);
        assertEquals(night, at0200, 0.01D);

        for (long t = 0L; t < 24000L; t += 100L) {
            final double off = WindModifiers.diurnalOffset(t, day, night, false);
            assertTrue(off <= day + 1.0E-9D && off >= night - 1.0E-9D,
                    "日变化越界 @" + t + ": " + off);
        }
    }

    @Test
    @DisplayName("时刻换算自检：dayTime=0 是 06:00、6000 是 12:00")
    void hourConversionIsCorrect() {
        assertEquals(0L, atHour(6.0D));
        assertEquals(6000L, atHour(12.0D));
        assertEquals(8000L, atHour(14.0D));
        assertEquals(20000L, atHour(2.0D));
    }

    @Test
    @DisplayName("海洋的日变化相位相反：白天比陆地凉、夜里比陆地暖（用户要求）")
    void oceanDiurnalIsInverted() {
        final double day = 4.0D;
        final double night = -6.0D;
        final double landAfternoon = WindModifiers.diurnalOffset(atHour(14.0D), day, night, false);
        final double seaAfternoon = WindModifiers.diurnalOffset(atHour(14.0D), day, night, true);
        final double landNight = WindModifiers.diurnalOffset(atHour(2.0D), day, night, false);
        final double seaNight = WindModifiers.diurnalOffset(atHour(2.0D), day, night, true);

        System.out.println("OCEAN 下午 陆地 " + landAfternoon + " / 海洋 " + seaAfternoon
                + "；凌晨 陆地 " + landNight + " / 海洋 " + seaNight);

        assertTrue(seaAfternoon < 0.0D, "海洋下午应比基准【凉】: " + seaAfternoon);
        assertTrue(seaNight > 0.0D, "海洋凌晨应比基准【暖】: " + seaNight);

        assertEquals(-landAfternoon, seaAfternoon, 1.0E-9D);
        assertEquals(-landNight, seaNight, 1.0E-9D);

        assertTrue(seaAfternoon < landAfternoon, "下午海洋应比陆地凉");
        assertTrue(seaNight > landNight, "凌晨海洋应比陆地暖");
    }

    @Test
    @DisplayName("日变化关闭时恒为 0（两个偏移都为 0）")
    void diurnalDisabled() {
        for (long t = 0L; t < 24000L; t += 1000L) {
            assertEquals(0.0D, WindModifiers.diurnalOffset(t, 0.0D, 0.0D, false), 1.0E-12D);
            assertEquals(0.0D, WindModifiers.diurnalOffset(t, 0.0D, 0.0D, true), 1.0E-12D);
        }
    }

    @Test
    @DisplayName("日变化全天连续：相邻 tick 的变化极小")
    void diurnalIsContinuous() {
        final double day = 4.0D;
        final double night = -6.0D;
        double worst = 0.0D;
        double prev = WindModifiers.diurnalOffset(0L, day, night, false);
        for (long t = 1L; t <= 24000L; t++) {
            final double cur = WindModifiers.diurnalOffset(t, day, night, false);
            worst = Math.max(worst, Math.abs(cur - prev));
            prev = cur;
        }
        System.out.println("DIURNAL 每 tick 最大变化 " + worst);

        assertTrue(worst < 0.01D, "日变化不够平滑，单 tick 变化 " + worst);
    }

    @Test
    @DisplayName("微波动：有界、确定性、且确实在变")
    void fluctuationIsBoundedDeterministicAndAlive() {
        final double amp = 1.5D;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (long t = 0L; t < 20000L; t += 37L) {
            final double f = WindModifiers.temperatureFluctuation(
                    4242L, t, 100.0D, 70.0D, -200.0D, 16.0D, amp);
            assertTrue(Math.abs(f) <= amp + 1.0E-9D, "波动越界 @" + t + ": " + f);
            min = Math.min(min, f);
            max = Math.max(max, f);
        }
        System.out.println("TEMP-FLUC range " + min + " .. " + max + " (amp " + amp + ")");

        assertEquals(
                WindModifiers.temperatureFluctuation(4242L, 1234L, 10.0D, 70.0D, 20.0D, 16.0D, amp),
                WindModifiers.temperatureFluctuation(4242L, 1234L, 10.0D, 70.0D, 20.0D, 16.0D, amp),
                0.0D);

        assertTrue(max - min > 0.2D * amp, "波动几乎不变，等于没加: " + (max - min));
        assertEquals(0.0D, WindModifiers.temperatureFluctuation(
                4242L, 1234L, 10.0D, 70.0D, 20.0D, 16.0D, 0.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("微波动在空间上连续：相邻 1 格的变化远小于幅度")
    void fluctuationIsSpatiallySmooth() {
        final double amp = 1.5D;
        double worst = 0.0D;
        for (int i = 0; i < 500; i++) {
            final double x = i * 0.7D;
            final double a = WindModifiers.temperatureFluctuation(77L, 5000L, x, 70.0D, 0.0D, 16.0D, amp);
            final double b = WindModifiers.temperatureFluctuation(77L, 5000L, x + 1.0D, 70.0D, 0.0D, 16.0D, amp);
            worst = Math.max(worst, Math.abs(b - a));
        }
        System.out.println("TEMP-FLUC 每格最大变化 " + worst);

        assertTrue(worst < amp * 0.2D, "空间上不够平滑，每格变化 " + worst);
    }
}
