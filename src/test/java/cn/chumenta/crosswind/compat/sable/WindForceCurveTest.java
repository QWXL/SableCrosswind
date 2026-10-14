package cn.chumenta.crosswind.compat.sable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindForceCurveTest {
    private static double force(final double windSpeed, final double beta,
                                final double cd, final double area) {
        final double rho = 1.225D;
        final double k = 0.5D * rho * cd * area * windSpeed;
        double f = windSpeed * k;
        if (beta != 1.0D) {
            f *= Math.pow(windSpeed, beta - 1.0D);
        }
        return f;
    }

    @Test
    @DisplayName("β=1 时严格是 v²（物理公式，无额外运算）")
    void betaOneIsPhysicalV2() {
        final double f5 = force(5.0D, 1.0D, 1.05D, 4.33D);
        final double f10 = force(10.0D, 1.0D, 1.05D, 4.33D);
        System.out.println("β=1: F(5)=" + String.format("%.3f", f5)
                + " N, F(10)=" + String.format("%.3f", f10) + " N");
        assertEquals(4.0D, f10 / f5, 1.0E-9D, "β=1 时风速翻倍力应翻 4 倍");
    }

    @Test
    @DisplayName("★ β 越大，小风被压得越狠、大小风差距越大（用比值比较，与绝对尺度无关）")
    void higherBetaIncreasesDynamicRange() {
        final double area = 4.33D;
        final double cd = 1.05D;
        final double mass = 6.0D;
        System.out.println("── 3 原木结构（F 未乘 force_multiplier/mass_exponent 的裸值）");
        System.out.printf("%-6s %-8s %-14s %-14s %-12s%n",
                "β", "F∝", "F(5) N", "F(20) N", "大小风比");
        for (final double beta : new double[] {1.0D, 1.5D, 2.0D}) {
            final double f5 = force(5.0D, beta, cd, area);
            final double f20 = force(20.0D, beta, cd, area);
            System.out.printf("%-6.1f %-8s %-14.2f %-14.2f %-12.1f%n",
                    beta, "v^" + (1.0D + beta), f5, f20, f20 / f5);
        }

        final double r10 = force(20.0D, 1.0D, cd, area) / force(5.0D, 1.0D, cd, area);
        final double r15 = force(20.0D, 1.5D, cd, area) / force(5.0D, 1.5D, cd, area);
        final double r20 = force(20.0D, 2.0D, cd, area) / force(5.0D, 2.0D, cd, area);
        System.out.println("动力范围：β=1 → " + String.format("%.1f", r10)
                + "×，β=1.5 → " + String.format("%.1f", r15)
                + "×，β=2 → " + String.format("%.1f", r20) + "×");
        assertTrue(r15 > r10 && r20 > r15, "动力范围未随 β 单调增大");

        final double norm15 = (force(20.0D, 1.5D, cd, area) / force(5.0D, 1.5D, cd, area))
                / (force(20.0D, 1.0D, cd, area) / force(5.0D, 1.0D, cd, area));
        System.out.println("β=1.5 相对 β=1 的动力范围倍数 = " + String.format("%.2f", norm15));
        assertTrue(norm15 > 1.5D, "β=1.5 的动力范围提升不明显");
    }

    @Test
    @DisplayName("β 不改变力的方向（只缩放模长）")
    void betaDoesNotChangeDirection() {
        final org.joml.Vector3d drive = new org.joml.Vector3d(3.0D, 0.0D, -4.0D);
        final org.joml.Vector3d a = new org.joml.Vector3d(drive).mul(1.0D);
        final org.joml.Vector3d b = new org.joml.Vector3d(drive).mul(Math.pow(drive.length(), 0.5D));
        System.out.println("after β scaling: " + b);
        assertEquals(0.0D, new org.joml.Vector3d(a).cross(b).length(), 1.0E-12D,
                "β 缩放改变了力的方向");
    }
}
