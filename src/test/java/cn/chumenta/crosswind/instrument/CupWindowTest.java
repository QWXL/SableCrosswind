package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CupWindowTest {
    private static final class NaiveWindow {
        private final int size;
        private final List<Double> buf = new ArrayList<>();

        NaiveWindow(final int size) {
            this.size = Math.max(1, size);
        }

        double push(final double v) {
            if (!Double.isFinite(v)) {
                return this.average();
            }
            this.buf.add(Math.max(0.0D, v));
            if (this.buf.size() > this.size) {
                this.buf.remove(0);
            }
            return this.average();
        }

        double average() {
            return this.buf.isEmpty() ? 0.0D
                    : this.buf.stream().mapToDouble(Double::doubleValue).average().orElse(0.0D);
        }
    }

    @Test
    @DisplayName("★ 与朴素参考实现逐步一致（含各种风速走势）")
    void matchesNaiveReference() {
        final double[] pattern = {
                0.0D, 1.0D, 2.0D, 3.0D, 4.0D, 5.0D,
                5.0D, 5.0D, 5.0D,
                40.42D, 40.42D,
                5.0D, 0.0D,
                12.5D, 7.25D, 33.0D, 1.0D,
        };
        for (final int size : new int[]{1, 2, 3, 4, 7, 40}) {
            final CupWindow ring = new CupWindow(size);
            final NaiveWindow naive = new NaiveWindow(size);
            for (int i = 0; i < pattern.length; i++) {
                final double a = ring.push(pattern[i]);
                final double b = naive.push(pattern[i]);
                assertEquals(b, a, 1.0E-12D,
                        "窗口 " + size + " 的第 " + i + " 个样本（输入 " + pattern[i] + "）不一致");
            }
        }
    }

    @Test
    @DisplayName("★ 稳态无偏：风速恒定时读数就等于该风速")
    void steadyStateIsUnbiased() {
        for (final double v : new double[]{0.0D, 3.5D, 40.42D, 100.0D}) {
            final CupWindow w = new CupWindow(10);
            for (int i = 0; i < 200; i++) {
                w.push(v);
            }
            assertEquals(v, w.average(), 1.0E-9D, "恒定 " + v + " m/s 时读数应等于它本身");
        }
    }

    @Test
    @DisplayName("★ 还没填满时用【实际样本数】当分母，不是窗口大小")
    void partialWindowUsesActualCount() {
        final CupWindow w = new CupWindow(40);
        assertFalse(w.hasSamples());
        assertEquals(20.0D, w.push(20.0D), 1.0E-12D,
                "第一个样本就应读出它自己 —— 若用窗口大小当分母会得到 0.5，那是错的");
        assertTrue(w.hasSamples());
        assertEquals(15.0D, w.push(10.0D), 1.0E-12D, "(20+10)/2");
    }

    @Test
    @DisplayName("★ 窗口满了才滑动：第 N+1 个样本挤掉最旧的那个")
    void windowSlidesOnlyWhenFull() {
        final CupWindow w = new CupWindow(4);
        final NaiveWindow ref = new NaiveWindow(4);
        final double[] seq = {1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 6.0D, 7.0D};
        for (int i = 0; i < seq.length; i++) {
            final double got = w.push(seq[i]);
            final double want = ref.push(seq[i]);
            assertEquals(want, got, 1.0E-12D, "第 " + i + " 个样本（" + seq[i] + "）");
        }

        assertEquals(5.5D, w.average(), 1.0E-12D,
                "窗口大小为 4 时，7 个样本之后应只剩最后 4 个");

        assertTrue(Math.abs(w.average() - 4.0D) > 1.0D,
                "若窗口没滑动，均值会是 (1+..+7)/7 = 4.0 —— 那说明实现错了");
    }

    @Test
    @DisplayName("★ 能跟上大跳变：40 m/s 必须在窗口长度内追上（用户实测场景）")
    void followsLargeJumpWithinWindow() {
        final int ticks = 40;
        final CupWindow w = new CupWindow(ticks);
        for (int i = 0; i < ticks * 2; i++) {
            w.push(5.0D);
        }
        assertEquals(5.0D, w.average(), 1.0E-9D);

        final double target = 40.42D;
        int t = 0;
        while (w.average() < 5.0D + (target - 5.0D) * 0.95D && t < ticks * 3) {
            w.push(target);
            t++;
        }
        final double seconds = t * 0.05D;
        System.out.printf("窗口 %d tick：从 5 → %.2f m/s 走到 95%% 用了 %d tick（%.2f 秒）%n",
                ticks, target, t, seconds);
        assertTrue(t <= ticks,
                "跳变应在【一个窗口长度】内基本反映完（" + ticks + " tick），实际用了 " + t);
    }

    @Test
    @DisplayName("★ NaN / 无穷不污染窗口（坏数据不等于无风）")
    void nonFiniteSamplesDoNotPoison() {
        final CupWindow w = new CupWindow(4);
        w.push(10.0D);
        w.push(10.0D);
        assertEquals(10.0D, w.average(), 1.0E-12D);

        assertEquals(10.0D, w.push(Double.NaN), 1.0E-12D, "NaN 不该进入窗口");
        assertEquals(10.0D, w.push(Double.POSITIVE_INFINITY), 1.0E-12D, "∞ 不该进入窗口");
        assertEquals(10.0D, w.push(Double.NEGATIVE_INFINITY), 1.0E-12D, "-∞ 不该进入窗口");

        assertEquals(40.0D / 3.0D, w.push(20.0D), 1.0E-12D, "(10+10+20)/3");
    }

    @Test
    @DisplayName("负风速被夹到 0（不产生负读数）")
    void negativeSpeedClamped() {
        final CupWindow w = new CupWindow(2);
        assertEquals(0.0D, w.push(-5.0D), 1.0E-12D);
        assertEquals(3.0D, w.push(6.0D), 1.0E-12D, "(0+6)/2");
    }

    @Test
    @DisplayName("clear 后回到空状态")
    void clearResetsState() {
        final CupWindow w = new CupWindow(3);
        w.push(1.0D);
        w.push(2.0D);
        w.push(3.0D);
        assertTrue(w.hasSamples());
        w.clear();
        assertFalse(w.hasSamples());
        assertEquals(0.0D, w.average(), 1.0E-12D);

        assertEquals(9.0D, w.push(9.0D), 1.0E-12D);
    }

    @Test
    @DisplayName("窗口大小至少为 1（退化输入不会让类崩）")
    void windowSizeIsAtLeastOne() {
        assertEquals(1, new CupWindow(0).windowTicks());
        assertEquals(1, new CupWindow(-7).windowTicks());
        final CupWindow w = new CupWindow(0);
        assertEquals(4.0D, w.push(4.0D), 1.0E-12D);
        assertEquals(5.0D, w.push(5.0D), 1.0E-12D, "窗口为 1 ⇒ 读数就是最新样本");
    }
}
