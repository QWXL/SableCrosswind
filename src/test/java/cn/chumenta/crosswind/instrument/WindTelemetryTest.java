package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindTelemetryTest {
    private static Map<String, String> parse(final String text) {
        final Map<String, String> out = new HashMap<>();
        for (final String token : text.split(" ")) {
            final int i = token.indexOf(':');
            out.put(token.substring(0, i), token.substring(i + 1));
        }
        return out;
    }

    @Test
    @DisplayName("★★ 完整文本逐字符固定（键名/顺序/位数都不能漂）")
    void exactFormat() {
        final String s = WindTelemetry.windCondition(
                23.4D, 31.2D, 215.0D, 35.0D, 16.5D, 0.0D, true);
        assertEquals("S:23.4 G:31.2 F:215 T:035 C:16.5 P:0.00", s);
    }

    @Test
    @DisplayName("固定 6 个键，顺序恒定")
    void fixedKeysInFixedOrder() {
        final String s = WindTelemetry.windCondition(1.0D, 2.0D, 3.0D, 4.0D, 5.0D, 0.5D, true);
        final String[] tokens = s.split(" ");
        assertEquals(6, tokens.length, "键的数量必须固定为 6");
        assertEquals(
                "S,G,F,T,C,P",
                String.join(",", java.util.Arrays.stream(tokens)
                        .map(t -> t.substring(0, t.indexOf(':')))
                        .toList()));
    }

    @Test
    @DisplayName("角度规范到 [0,360) 且补零到 3 位")
    void degreesNormalised() {
        final Map<String, String> m = parse(
                WindTelemetry.windCondition(5.0D, 6.0D, 360.0D, -10.0D, 0, 0, true));
        assertEquals("000", m.get("F"), "360 度应规范成 000");
        assertEquals("350", m.get("T"), "-10 度应规范成 350");
    }

    @Test
    @DisplayName("★★ 无风时 F/T 输出 ? —— 不给 atan2(0,0) 那个假「正北」")
    void calmYieldsUnknownDirection() {
        final Map<String, String> m = parse(WindTelemetry.windCondition(
                0.0D, 0.0D, 0.0D, 180.0D, 12.0D, 0.0D, true));
        assertEquals("0.0", m.get("S"), "无风时风速照实报 0");
        assertEquals("?", m.get("F"), "无风时不该报方位");
        assertEquals("?", m.get("T"));

        assertEquals("12.0", m.get("C"));
        assertEquals("0.00", m.get("P"));
    }

    @Test
    @DisplayName("★ 有风向的判据边界：恰好在阈值上就应当给方位")
    void justAboveDirectionFloorStillReportsBearing() {
        final Map<String, String> m = parse(WindTelemetry.windCondition(
                0.1D, 0.1D, 90.0D, 270.0D, 0.0D, 0.0D, true));
        assertEquals("090", m.get("F"), "达到下限就该报方位");
        assertEquals("270", m.get("T"));
    }

    @Test
    @DisplayName("★★ 气候不可用时 C/P 输出 ?（不是 0.0）")
    void climateUnavailableUsesQuestionMark() {
        final Map<String, String> m = parse(
                WindTelemetry.windCondition(10.0D, 12.0D, 90.0D, 270.0D, 0.0D, 0.0D, false));
        assertEquals("?", m.get("C"), "气候不可用时不能把占位的 0.0 报成 0 °C（C2）");
        assertEquals("?", m.get("P"));

        assertEquals("10.0", m.get("S"));
        assertEquals("090", m.get("F"));
    }

    @Test
    @DisplayName("非有限值退化成 ? （解析器只需处理一种异常值）")
    void nonFiniteBecomesQuestionMark() {
        final Map<String, String> m = parse(WindTelemetry.windCondition(
                Double.NaN, Double.POSITIVE_INFINITY, Double.NaN, 10.0D, 1.0D, 0.0D, true));
        assertEquals("?", m.get("S"));
        assertEquals("?", m.get("G"));
        assertEquals("?", m.get("F"));
        assertEquals("?", m.get("T"));
    }

    @Test
    @DisplayName("★★ 小数点与区域设置无关（德语区默认是逗号，会毁掉解析）")
    void localeIndependentDecimalPoint() {
        final Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            final String s = WindTelemetry.windCondition(
                    23.4D, 31.2D, 215.0D, 35.0D, 16.5D, 0.25D, true);
            assertEquals("S:23.4 G:31.2 F:215 T:035 C:16.5 P:0.25", s);
            assertTrue(s.indexOf(',') < 0, "文本里不能出现逗号小数点：" + s);
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    @DisplayName("降水是 0..1 的比例（两位小数），不是百分比")
    void precipitationIsFraction() {
        final Map<String, String> m = parse(
                WindTelemetry.windCondition(0, 0, 0, 0, 0, 0.75D, true));
        assertEquals("0.75", m.get("P"));
    }

    @Test
    @DisplayName("整串长度在显示板一行的量级内（简短）")
    void reasonablyShort() {
        final String s = WindTelemetry.windCondition(
                123.4D, 456.7D, 359.0D, 359.0D, -40.5D, 1.0D, true);
        assertTrue(s.length() <= 48, "最长也不该超过 48 字符，实际 " + s.length() + "：" + s);
    }
}
