package cn.chumenta.crosswind.entity;

import org.joml.Vector3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityAerodynamicsTest {
    private static final double RHO = 1.225D;
    private static final double CD = 1.05D;
    private static final double KM = 3000.0D;
    private static final double VMIN = 0.01D;

    private static Vector3d accelFor(final double sx, final double sy, final double sz,
                                     final double windX, final double windZ,
                                     final double velX, final double velZ,
                                     final double maxAccel) {
        final double area = EntityAerodynamics.projectedArea(sx, sy, sz, windX, windZ);
        final double mass = EntityAerodynamics.effectiveMass(sx * sy * sz, KM, VMIN);
        final Vector3d drive = EntityAerodynamics.horizontalDrive(windX, windZ, velX, velZ, new Vector3d());
        return EntityAerodynamics.acceleration(drive, area, mass, RHO, CD, maxAccel, new Vector3d());
    }

    @Test
    @DisplayName("★ E-1：2×4×2 的受力是 1×2×1 的 4 倍，但加速度只有一半")
    void largerBoxHasMoreForceButLessAcceleration() {
        final double windX = 10.0D;

        final double a1 = EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, windX, 0.0D);
        final double m1 = EntityAerodynamics.effectiveMass(1.0D * 2.0D * 1.0D, KM, VMIN);

        final double a2 = EntityAerodynamics.projectedArea(2.0D, 4.0D, 2.0D, windX, 0.0D);
        final double m2 = EntityAerodynamics.effectiveMass(2.0D * 4.0D * 2.0D, KM, VMIN);

        System.out.printf("1x2x1: A=%.3f m²  m=%.1f kg%n", a1, m1);
        System.out.printf("2x4x2: A=%.3f m²  m=%.1f kg%n", a2, m2);
        System.out.printf("面积比 %.2f（应=4）  质量比 %.2f（应=8）%n", a2 / a1, m2 / m1);

        assertEquals(4.0D, a2 / a1, 1.0E-9D, "投影面积应为 4 倍（尺寸²）");
        assertEquals(8.0D, m2 / m1, 1.0E-9D, "有效质量应为 8 倍（尺寸³）");

        final double acc1 = accelFor(1.0D, 2.0D, 1.0D, windX, 0.0D, 0.0D, 0.0D, 0.0D).length();
        final double acc2 = accelFor(2.0D, 4.0D, 2.0D, windX, 0.0D, 0.0D, 0.0D, 0.0D).length();
        System.out.printf("加速度：小 %.5f m/s²  大 %.5f m/s²  比值 %.3f（应=0.5）%n",
                acc1, acc2, acc2 / acc1);
        assertEquals(0.5D, acc2 / acc1, 1.0E-9D, "大实体的加速度应是小实体的一半");
    }

    @Test
    @DisplayName("★ E-2：以风同向同速运动的实体受力接近 0")
    void entityAtWindSpeedFeelsNoForce() {
        final Vector3d a = accelFor(1.0D, 2.0D, 1.0D, 10.0D, 0.0D, 10.0D, 0.0D, 0.0D);
        System.out.println("实体与风同向同速 ⇒ 加速度 = " + a);
        assertEquals(0.0D, a.length(), 1.0E-12D, "达到风速后风应给不出力");
    }

    @Test
    @DisplayName("★ 竖直运动不产生竖直风载荷（与 Sable 那条路径同一纪律）")
    void verticalMotionProducesNoVerticalLoad() {
        final double area = EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, 10.0D, 0.0D);
        final double mass = EntityAerodynamics.effectiveMass(2.0D, KM, VMIN);

        final Vector3d drive = EntityAerodynamics.horizontalDrive(10.0D, 0.0D, 0.0D, 0.0D, new Vector3d());
        final Vector3d a = EntityAerodynamics.acceleration(drive, area, mass, RHO, CD, 0.0D, new Vector3d());
        System.out.println("风=(10,0,0) 实体竖直下坠 ⇒ drive=" + drive + "  加速度=" + a);
        assertEquals(0.0D, drive.y, 0.0D, "drive 的竖直分量必须恒为 0");
        assertEquals(0.0D, a.y, 0.0D, "风力的竖直分量必须恒为 0");
    }

    @Test
    @DisplayName("投影面积：轴向与斜向")
    void projectedAreaGeometry() {
        assertEquals(2.0D, EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, 1.0D, 0.0D), 1.0E-12D);

        assertEquals(2.0D, EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, 0.0D, 1.0D), 1.0E-12D);

        final double diag = EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, 1.0D, 1.0D);
        System.out.printf("45° 斜风投影面积 = %.4f（解析 2√2 = %.4f）%n", diag, 2.0D * Math.sqrt(2.0D));
        assertEquals(2.0D * Math.sqrt(2.0D), diag, 1.0E-12D);

        assertEquals(0.0D, EntityAerodynamics.projectedArea(1.0D, 2.0D, 1.0D, 0.0D, 0.0D), 0.0D);
    }

    @Test
    @DisplayName("加速度上限：正常体型不该被它钳制（它是兜底，不是手感旋钮）")
    void capOnlyEngagesForExtremeCases() {
        final double cap = 0.5D;

        final Vector3d normal = accelFor(1.0D, 2.0D, 1.0D, 20.0D, 0.0D, 0.0D, 0.0D, cap);
        final Vector3d normalUncapped = accelFor(1.0D, 2.0D, 1.0D, 20.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        System.out.printf("正常体型 @20 m/s：裸 %.4f，加上限后 %.4f m/s²（上限 %.2f）%n",
                normalUncapped.length(), normal.length(), cap);
        assertEquals(normalUncapped.length(), normal.length(), 1.0E-12D,
                "正常体型不该被上限钳制 —— 否则体型差异会被抹平（Sable 那边踩过的同一个坑）");

        final double px = 0.25D;
        final double py = 6.0D;
        final double pz = 6.0D;
        final double strongWind = 25.0D;
        final double area = EntityAerodynamics.projectedArea(px, py, pz, strongWind, 0.0D);
        final Vector3d extreme = accelFor(px, py, pz, strongWind, 0.0D, 0.0D, 0.0D, cap);
        final Vector3d extremeUncapped = accelFor(px, py, pz, strongWind, 0.0D, 0.0D, 0.0D, 0.0D);
        System.out.printf("极端体型(厚 %.2f, A=%.1f m²) @%.0f m/s：裸 %.4f → 钳到 %.4f m/s²%n",
                px, area, strongWind, extremeUncapped.length(), extreme.length());
        assertTrue(extremeUncapped.length() > cap, "这个极端配置本该超过上限");
        assertEquals(cap, extreme.length(), 1.0E-12D, "上限应在极端情况下恰好生效");

        assertEquals(1.0D, extreme.x / extreme.length(), 1.0E-12D, "上限改变了力的方向");
    }

    @Test
    @DisplayName("V_min 的作用是给极小体积【兜底】，真正防吹飞的是加速度上限")
    void minVolumeIsAFloorNotAProtection() {
        final double cap = 0.5D;

        final double item = EntityAerodynamics.effectiveMass(0.25D * 0.25D * 0.25D, KM, VMIN);

        final double tiny = EntityAerodynamics.effectiveMass(0.001D, KM, VMIN);
        System.out.printf("物品 m_eff=%.1f kg；微小物 m_eff=%.1f kg（下限抬到 %.1f）%n",
                item, tiny, KM * VMIN);

        assertEquals(KM * VMIN, tiny, 1.0E-9D, "触发下限后质量应停在 k_m·V_min");
        assertTrue(tiny < item, "下限把极小体积抬到了 k_m·V_min");

        final double aItem = accelFor(0.25D, 0.25D, 0.25D, 5.0D, 0.0D, 0.0D, 0.0D, cap).length();
        final double aSmall = accelFor(0.1D, 0.1D, 0.1D, 5.0D, 0.0D, 0.0D, 0.0D, cap).length();
        System.out.printf("5 m/s 下：物品 a=%.5f，更小的东西 a=%.5f m/s²（上限 %.2f）%n",
                aItem, aSmall, cap);
        assertTrue(aItem <= cap + 1.0E-12D && aSmall <= cap + 1.0E-12D,
                "加速度上限必须对掉落物量级生效");
    }

    @Test
    @DisplayName("超过风速时风转为刹车（drive 反号）")
    void aboveWindSpeedWindBrakes() {
        final Vector3d a = accelFor(1.0D, 2.0D, 1.0D, 10.0D, 0.0D, 25.0D, 0.0D, 0.0D);
        System.out.println("风速 10、实体 25 ⇒ 加速度 = " + a);
        assertTrue(a.x < 0.0D, "超过风速时风力必须反向（阻碍）");
    }

    @Test
    @DisplayName("风与实体的实际加速度量级（供调参参照）")
    void realisticMagnitudes() {
        System.out.println("── 当前标定（k_m=3000, V_min=0.01, 上限 0.5 m/s²）");
        System.out.printf("%-22s %-10s %-12s %-12s%n", "实体", "V(m³)", "A(m²)", "a@5m/s");
        final double[][] cases = {
                {0.6D, 1.8D, 0.6D},
                {0.6D, 1.95D, 0.6D},
                {0.25D, 0.25D, 0.25D},
                {1.4D, 1.4D, 1.4D},
        };
        final String[] names = {"玩家/玩家型", "僵尸型", "掉落物", "船/矿车型"};
        for (int i = 0; i < cases.length; i++) {
            final double[] c = cases[i];
            final double area = EntityAerodynamics.projectedArea(c[0], c[1], c[2], 1.0D, 0.0D);
            final double m = EntityAerodynamics.effectiveMass(c[0] * c[1] * c[2], KM, VMIN);
            final Vector3d d = EntityAerodynamics.horizontalDrive(5.0D, 0.0D, 0.0D, 0.0D, new Vector3d());
            final Vector3d a = EntityAerodynamics.acceleration(d, area, m, RHO, CD, 0.5D, new Vector3d());
            System.out.printf("%-22s %-10.4f %-12.4f %-12.5f%n", names[i], c[0] * c[1] * c[2], area, a.length());
        }
    }
}
