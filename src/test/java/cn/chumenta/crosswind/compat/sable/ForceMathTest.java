package cn.chumenta.crosswind.compat.sable;

import org.joml.Vector3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForceMathTest {
    private static final double K_PARALLEL = 0.75D;
    private static final double K_DIRECTIONLESS = 0.06888202261D;
    private static final double K_LIFT = 0.475D;

    private static final double K_PARALLEL_SYMMETRIC_SAIL = 1.75D;

    private static final class S {
        final Vector3d out = new Vector3d();
        final Vector3d drag = new Vector3d();
        final Vector3d tmp = new Vector3d();
        final Vector3d base = new Vector3d();
        final Vector3d wind = new Vector3d();
        final Vector3d dragA = new Vector3d();
        final Vector3d dragB = new Vector3d();
        final Vector3d tmpA = new Vector3d();
        final Vector3d tmpB = new Vector3d();
        final Vector3d veloWind = new Vector3d();
    }

    private static double coeff(final double pressure, final double dt) {
        return pressure * dt;
    }

    @Test
    @DisplayName("★ S-0 零风不变性：v_wind = 0 时修正力恒为 0")
    void zeroWindProducesZeroCorrection() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(3.0D, -1.0D, 2.0D);
        final S s = new S();
        final double c = coeff(1.0D, 0.05D);

        final Vector3d r1 = ForceMath.windCorrection(normal, velo, null, c,
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);
        assertEquals(0.0D, r1.x, 0.0D);
        assertEquals(0.0D, r1.y, 0.0D);
        assertEquals(0.0D, r1.z, 0.0D);

        final Vector3d r2 = ForceMath.windCorrection(normal, velo, new Vector3d(0.0D, 0.0D, 0.0D), c,
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);
        assertEquals(0.0D, r2.x, 0.0D);
        assertEquals(0.0D, r2.y, 0.0D);
        assertEquals(0.0D, r2.z, 0.0D);
    }

    @Test
    @DisplayName("★ S-0 补充：有风时修正不为 0（防止恒返回 0 的假通过）")
    void correctionIsNotTriviallyZero() {
        final Vector3d normal = new Vector3d(0.0D, 1.0D, 0.0D);
        final Vector3d velo = new Vector3d(0.0D, 0.0D, 10.0D);
        final Vector3d wind = new Vector3d(0.0D, 0.0D, -5.0D);
        final S s = new S();

        final Vector3d r = ForceMath.windCorrection(normal, velo, wind, coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);
        final double mag = r.length();
        System.out.println("非零修正力的模长 = " + mag);
        assertTrue(mag > 1.0E-9D, "有风时修正力为 0 —— 实现可能恒返回 0");
    }

    @Test
    @DisplayName("线性项：修正力在纯无方向阻力下等于 F(−v_wind)")
    void correctionEqualsForceOfNegativeWindForLinearTerm() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(4.0D, -2.0D, 7.0D);
        final Vector3d wind = new Vector3d(1.0D, 0.5D, -3.0D);
        final double pressure = 1.3D;
        final double dt = 0.05D;
        final S s = new S();

        final Vector3d correction = ForceMath.windCorrection(normal, velo, wind, coeff(pressure, dt),
                0.0D, K_DIRECTIONLESS, 0.0D,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);

        final double factor = -K_DIRECTIONLESS * coeff(pressure, dt);
        final Vector3d analytic = new Vector3d(wind).mul(factor);

        System.out.println("线性项修正 实测=" + correction + "  解析=" + analytic);
        assertEquals(analytic.x, correction.x, 1.0E-15D);
        assertEquals(analytic.y, correction.y, 1.0E-15D);
        assertEquals(analytic.z, correction.z, 1.0E-15D);
    }

    @Test
    @DisplayName("平行阻力项：修正力方向沿法线，大小按 (N·w) 线性")
    void parallelDragCorrectionFollowsNormal() {
        final Vector3d normal = new Vector3d(0.0D, 1.0D, 0.0D);
        final Vector3d velo = new Vector3d(5.0D, 3.0D, 0.0D);
        final double pressure = 1.0D;
        final double dt = 0.1D;
        final S s = new S();

        final Vector3d wind = new Vector3d(0.0D, 2.0D, 0.0D);
        final Vector3d r = ForceMath.windCorrection(normal, velo, wind, coeff(pressure, dt),
                K_PARALLEL, 0.0D, 0.0D,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);

        final double expectedY = -(normal.dot(wind)) * K_PARALLEL * coeff(pressure, dt);
        System.out.println("平行阻力修正 实测=" + r + "  期望 y=" + expectedY);
        assertEquals(0.0D, r.x, 1.0E-15D);
        assertEquals(expectedY, r.y, 1.0E-15D);
        assertEquals(0.0D, r.z, 1.0E-15D);
    }

    @Test
    @DisplayName("无方向阻力项对力系数精确线性")
    void directionlessDragScalesExactly() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final S s = new S();

        ForceMath.force(normal, new Vector3d(2.0D, 3.0D, 1.0D), coeff(1.0D, 0.05D),
                0.0D, K_DIRECTIONLESS, 0.0D, s.base, s.drag, s.tmp);
        final double baseMag = s.base.length();
        assertTrue(baseMag > 0.0D, "基础情形就产生了零力，测试数据有问题");

        ForceMath.force(normal, new Vector3d(2.0D, 3.0D, 1.0D), coeff(2.0D, 0.05D),
                0.0D, K_DIRECTIONLESS, 0.0D, s.wind, s.dragA, s.tmpA);
        assertEquals(2.0D, s.wind.length() / baseMag, 1.0E-12D, "系数翻倍应精确翻倍");
    }

    @Test
    @DisplayName("复现公式：含升力项时力随系数单调增但【不是】精确线性（记录上游的真实形状）")
    void liftTermMakesForceSublinear() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final S s = new S();

        ForceMath.force(normal, new Vector3d(2.0D, 3.0D, 1.0D), coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.base, s.drag, s.tmp);
        final double baseMag = s.base.length();

        ForceMath.force(normal, new Vector3d(2.0D, 3.0D, 1.0D), coeff(2.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.wind, s.dragA, s.tmpA);
        final double ratio = s.wind.length() / baseMag;
        System.out.println("含升力项时系数翻倍 → 力 ×" + ratio);

        assertTrue(ratio > 1.0D, "力没有随系数增大");
        assertTrue(ratio < 2.0D, "比值不应达到 2 —— 升力项是非线性的: " + ratio);
        assertTrue(ratio > 1.9D, "偏离线性太多，公式可能写错了: " + ratio);
    }

    @Test
    @DisplayName("复现公式无副作用：相同输入反复调用必须得到逐位相同的结果")
    void forceIsSideEffectFree() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(2.0D, 3.0D, 1.0D);
        final S s = new S();
        final double c = coeff(1.0D, 0.05D);

        ForceMath.force(normal, velo, c, K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                s.base, s.drag, s.tmp);
        final double first = s.base.length();

        for (int i = 0; i < 3; i++) {
            ForceMath.force(normal, velo, c, K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                    s.wind, s.dragA, s.tmpA);
            assertEquals(first, s.wind.length(), 0.0D,
                    "第 " + (i + 2) + " 次调用结果不同 —— force() 有副作用");
        }

        assertEquals(2.0D, velo.x, 0.0D);
        assertEquals(3.0D, velo.y, 0.0D);
        assertEquals(1.0D, velo.z, 0.0D);
        assertEquals(1.0D, normal.x, 0.0D);
    }

    @Test
    @DisplayName("复现公式：零速度 → 零力（上游是 F ∝ v，不是 v²）")
    void forceIsZeroAtZeroVelocity() {
        final Vector3d normal = new Vector3d(0.0D, 0.0D, 1.0D);
        final S s = new S();

        ForceMath.force(normal, new Vector3d(0.0D, 0.0D, 0.0D), coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.base, s.drag, s.tmp);
        assertEquals(0.0D, s.base.length(), 0.0D,
                "零速度产生了非零气动力 —— 上游是线性的，这不可能");
    }

    @Test
    @DisplayName("复现公式：纯方向量项（k1=k3=0）时力与速度严格成正比")
    void forceIsLinearInVelocity() {
        final Vector3d normal = new Vector3d(0.0D, 0.0D, 1.0D);
        final S s = new S();
        final double c = coeff(1.0D, 0.05D);

        ForceMath.force(normal, new Vector3d(0.0D, 0.0D, 3.0D), c,
                0.0D, K_DIRECTIONLESS, 0.0D, s.base, s.drag, s.tmp);
        ForceMath.force(normal, new Vector3d(0.0D, 0.0D, 6.0D), c,
                0.0D, K_DIRECTIONLESS, 0.0D, s.wind, s.dragA, s.tmpA);
        assertEquals(2.0D, s.wind.length() / s.base.length(), 1.0E-12D,
                "力的增长速度偏离线性 —— 可能把公式写成了 v²");
    }

    @Test
    @DisplayName("复现公式：k3 = 0（对称帆）时走纯阻力，且不因 DRAG 未写而算错")
    void symmetricSailPathUsesOnlyDrag() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(3.0D, -4.0D, 5.0D);
        final S s = new S();
        final double c = coeff(1.0D, 0.05D);

        ForceMath.force(normal, velo, c, K_PARALLEL_SYMMETRIC_SAIL, K_DIRECTIONLESS, 0.0D,
                s.base, s.drag, s.tmp);

        final double nDotV = normal.dot(velo);
        final Vector3d parallel = new Vector3d(normal).mul(nDotV * K_PARALLEL_SYMMETRIC_SAIL * c);
        final Vector3d directionless = new Vector3d(velo).mul(K_DIRECTIONLESS * c);
        final Vector3d expected = parallel.add(directionless);

        System.out.println("对称帆路径 实测=" + s.base + "  解析=" + expected);
        assertEquals(expected.x, s.base.x, 1.0E-15D);
        assertEquals(expected.y, s.base.y, 1.0E-15D);
        assertEquals(expected.z, s.base.z, 1.0E-15D);
    }

    @Test
    @DisplayName("复现公式：所有标量为 0 时不产生力")
    void allScalarsZeroProducesNoForce() {
        final Vector3d normal = new Vector3d(0.0D, 1.0D, 0.0D);
        final S s = new S();

        ForceMath.force(normal, new Vector3d(9.0D, 9.0D, 9.0D), coeff(1.0D, 0.05D),
                0.0D, 0.0D, 0.0D, s.base, s.drag, s.tmp);
        assertEquals(0.0D, s.base.length(), 0.0D);
    }

    @Test
    @DisplayName("叠加修正不重复计力：差值幅度受限于两组力之差")
    void correctionDoesNotDoubleCount() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(10.0D, 0.0D, 0.0D);
        final Vector3d wind = new Vector3d(2.0D, 0.0D, 0.0D);
        final S s = new S();

        ForceMath.force(normal, velo, coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.base, s.drag, s.tmp);
        final Vector3d r = ForceMath.windCorrection(normal, velo, wind, coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT,
                s.out, s.base, s.wind, s.dragA, s.dragB, s.tmpA, s.tmpB, s.veloWind);

        System.out.println("baseline 力 = " + s.base + "  修正 = " + r);
        assertTrue(r.length() < 2.0D * s.base.length() + 1.0E-9D,
                "修正力量级失控，疑似重复计力: " + r.length() + " vs " + s.base.length());
    }

    @Test
    @DisplayName("★ 单位自检：timeStep 只由调用方乘一次，绝不在复现里预先乘")
    void timeStepIsAppliedExactlyOnce() {
        final Vector3d normal = new Vector3d(1.0D, 0.0D, 0.0D);
        final Vector3d velo = new Vector3d(2.0D, 3.0D, 1.0D);
        final S s = new S();

        ForceMath.force(normal, velo, coeff(1.0D, 0.05D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.base, s.drag, s.tmp);
        final double withDt1 = s.base.length();

        ForceMath.force(normal, velo, coeff(1.0D, 0.15D),
                K_PARALLEL, K_DIRECTIONLESS, K_LIFT, s.wind, s.dragA, s.tmpA);
        final double withDt3 = s.wind.length();

        final double ratio = withDt3 / withDt1;
        System.out.println("系数含 3×Δt → 力 ×" + ratio);
        assertTrue(ratio > 2.9D && ratio < 3.1D,
                "比值远离 3，说明 Δt 被乘了多次: " + ratio);
    }
}
