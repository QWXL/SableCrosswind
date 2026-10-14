package cn.chumenta.crosswind.compat.sable;

import org.joml.Vector3d;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindDriveTest {
    private static final double EPS = 1.0E-12D;

    private static Vector3d drive(final double wx, final double wy, final double wz,
                                  final double bx, final double by, final double bz) {
        return WindForceMath.driveVelocity(wx, wy, wz, bx, by, bz, new Vector3d());
    }

    @Test
    @DisplayName("★ 竖直下坠不产生竖直风载荷（水平风只给水平力）")
    void verticalMotionProducesNoVerticalLoad() {
        final Vector3d d = drive(10.0D, 0.0D, 0.0D, 0.0D, -45.0D, 0.0D);
        System.out.println("风=(10,0,0) 物体=(0,-45,0) ⇒ drive=" + d);

        assertEquals(0.0D, d.y, EPS, "竖直下坠产生了竖直风载荷 —— 这正是要避免的 bug");
        assertEquals(0.0D, d.z, EPS);

        assertEquals(10.0D, d.x, EPS);
    }

    @Test
    @DisplayName("★ 物体在风向上达到风速时载荷归零")
    void loadVanishesAtWindSpeed() {
        final Vector3d d = drive(10.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D);
        System.out.println("物体恰好以风速沿风运动 ⇒ drive=" + d);
        assertEquals(0.0D, d.length(), EPS, "达到风速后仍有风载荷 —— 风能给的力应当用尽");
    }

    @Test
    @DisplayName("★ 物体超过风速时 drive 反向（力转为刹车，而非无限加速）")
    void loadReversesAboveWindSpeed() {
        final Vector3d d = drive(10.0D, 0.0D, 0.0D, 25.0D, 0.0D, 0.0D);
        System.out.println("物体 25 m/s 顺风（风速 10）⇒ drive=" + d);
        assertTrue(d.x < 0.0D, "超过风速后 drive 应变号（力转为阻碍），实际 " + d.x);
        assertEquals(-15.0D, d.x, EPS);
    }

    @Test
    @DisplayName("drive 始终平行于风轴，永不带横向分量")
    void driveStaysOnWindAxis() {
        final Vector3d wind = new Vector3d(3.0D, 0.0D, -4.0D);
        final Vector3d body = new Vector3d(-7.0D, 12.0D, 2.5D);
        final Vector3d d = WindForceMath.driveVelocity(
                wind.x, wind.y, wind.z, body.x, body.y, body.z, new Vector3d());

        final Vector3d cross = new Vector3d(wind).cross(d);
        System.out.println("风=" + wind + " 物体=" + body + " ⇒ drive=" + d
                + "  与风轴的叉积=" + cross);
        assertEquals(0.0D, cross.length(), 1.0E-12D, "drive 偏离了风轴");
    }

    @Test
    @DisplayName("无风时 drive 为零")
    void zeroWindGivesZeroDrive() {
        assertEquals(0.0D, drive(0.0D, 0.0D, 0.0D, 5.0D, -9.0D, 1.0D).length(), 0.0D);
    }

    @Test
    @DisplayName("侧向运动不改变 drive 大小（只有风向分量参与扣除）")
    void lateralMotionDoesNotReduceDrive() {
        final Vector3d d = drive(8.0D, 0.0D, 0.0D, 0.0D, 0.0D, 50.0D);
        System.out.println("物体横向 50 m/s ⇒ drive=" + d);
        assertEquals(8.0D, d.x, EPS);
        assertEquals(0.0D, d.y, EPS);
        assertEquals(0.0D, d.z, EPS);
    }

    private static Vector3d windForce(final double vBodyAlongWind) {
        final double rho = 1.225D;
        final double cd = 1.05D;
        final double area = 1.0D;
        final double windSpeed = 10.0D;

        final Vector3d d = WindForceMath.driveVelocity(
                windSpeed, 0.0D, 0.0D, vBodyAlongWind, 0.0D, 0.0D, new Vector3d());
        final double driveSpeed = d.length();
        return d.mul(0.5D * rho * cd * area * driveSpeed);
    }

    @Test
    @DisplayName("★ 情形一：物体从 0 速逐渐逼近风速（力随速度单调递减到 0）")
    void case1AcceleratesFromRest() {
        System.out.println("── 情形一：0 速 → 风速（力应单调递减）");
        double prev = Double.MAX_VALUE;
        for (final double v : new double[] {0.0D, 2.0D, 4.0D, 6.0D, 8.0D, 9.0D, 9.9D}) {
            final double f = windForce(v).x;
            System.out.println("  物体 " + v + " m/s ⇒ 风力 " + String.format("%.2f", f) + " N");
            assertTrue(f < prev, "风力未随物体加速而减小：v=" + v);
            assertTrue(f > 0.0D, "未达风速时风力应为正（顺风推）");
            prev = f;
        }
    }

    @Test
    @DisplayName("★ 情形二：物体保持风速时风力为 0")
    void case2ZeroForceAtWindSpeed() {
        final double f = windForce(10.0D).x;
        System.out.println("── 情形二：物体恰为风速 ⇒ 风力 " + String.format("%.6f", f) + " N");
        assertEquals(0.0D, f, 1.0E-9D, "达到风速后仍有风推力");
    }

    @Test
    @DisplayName("★ 情形三：物体超过风速（其他推进力所致）时风转为阻碍，且同样有上限")
    void case3AboveWindSpeed() {
        System.out.println("── 情形三：超过风速（其他力所致）");
        for (final double v : new double[] {12.0D, 15.0D, 20.0D, 30.0D}) {
            final double f = windForce(v).x;
            System.out.println("  物体 " + v + " m/s ⇒ 风力 " + String.format("%.2f", f) + " N（负=阻碍）");
            assertTrue(f < 0.0D, "超过风速后风应转为阻碍：v=" + v);
        }
    }

    @Test
    @DisplayName("★★ 风能提供的力【有上限】：最大值就在物体静止时 = ½ρCdA·v_wind²")
    void windForceIsMaximalAtRest() {
        final double rho = 1.225D;
        final double cd = 1.05D;
        final double area = 1.0D;
        final double windSpeed = 10.0D;
        final double maxAtRest = 0.5D * rho * cd * area * windSpeed * windSpeed;

        double prev = Double.MAX_VALUE;
        for (double v = 0.0D; v <= windSpeed; v += 0.05D) {
            final double f = windForce(v).x;
            assertTrue(f <= prev + 1.0E-9D, "顺风运动时风力未单调递减：v=" + v);
            assertTrue(f >= -1.0E-9D, "未达风速时不应出现逆风推力：v=" + v);
            prev = f;
        }

        final double atRest = windForce(0.0D).x;
        System.out.println("静止时风力 = " + String.format("%.4f", atRest)
                + " N，解析值 ½ρCdAv² = " + String.format("%.4f", maxAtRest) + " N");
        assertEquals(maxAtRest, atRest, 1.0E-9D, "静止时风力不等于 ½ρCdA·v_wind²");

        double observedMax = 0.0D;
        for (double v = 0.0D; v <= windSpeed; v += 0.001D) {
            observedMax = Math.max(observedMax, windForce(v).x);
        }
        assertEquals(atRest, observedMax, 1.0E-9D, "顺风区间内的最大风力不在静止处");

        assertEquals(0.0D, windForce(windSpeed).x, 1.0E-9D);
        assertTrue(windForce(windSpeed * 3.0D).x < 0.0D, "超过风速后风应转为阻碍");
    }

    @Test
    @DisplayName("唯一真正的上界：驱动速度 |drive| ≤ 风速 + 物体沿风速度（因此力不会无界爆炸）")
    void driveSpeedIsBoundedByWindPlusBodySpeed() {
        final double windSpeed = 10.0D;
        for (final double vBody : new double[] {-5.0D, -20.0D, -50.0D, 0.0D, 5.0D, 20.0D}) {
            final Vector3d d = WindForceMath.driveVelocity(
                    windSpeed, 0.0D, 0.0D, vBody, 0.0D, 0.0D, new Vector3d());
            final double bound = windSpeed + Math.abs(vBody);
            System.out.println("物体沿风方向速度 " + vBody + " ⇒ |drive| = "
                    + String.format("%.2f", d.length()) + "，上界 " + bound);
            assertTrue(d.length() <= bound + 1.0E-9D, "|drive| 超过了 windSpeed + |v_along|");
        }
    }
}
