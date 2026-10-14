package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import cn.chumenta.crosswind.wind.WindDirection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindInstrumentFieldsTest {
    @Test
    @DisplayName("★ 风向标：来向 + 去向【两个独立字段】，没有风速/阵风/温度/降水")
    void windVaneIsDirectionsOnly() {
        final WindReadout.Fields f = WindReadout.Fields.DIRECTIONS_ONLY;

        assertTrue(f.fromDirection(), "风向标必须提供【来向】");
        assertTrue(f.toDirection(), "风向标必须提供【去向】");
        assertFalse(f.speed(), "风向标不该提供风速（只有箭头，看不出速度）");
        assertFalse(f.gust(), "风向标不该提供阵风");
        assertFalse(f.temperature(), "风向标不该提供温度");
        assertFalse(f.precipitation(), "风向标不该提供降水");
    }

    @Test
    @DisplayName("★ 风速计：只有风速【不含阵风】，没有风向/温度/降水")
    void anemometerIsSpeedOnly() {
        final WindReadout.Fields f = WindReadout.Fields.SPEED_ONLY;
        assertFalse(f.fromDirection(), "风速计不该提供风向");
        assertFalse(f.toDirection(), "风速计不该提供风向");
        assertTrue(f.speed(), "风速计必须提供风速");

        assertFalse(f.gust(), "风速计【不该】提供阵风（旧实现给过，已按用户要求去掉）");
        assertFalse(f.temperature(), "风速计不该提供温度");
        assertFalse(f.precipitation(), "风速计不该提供降水");
    }

    @Test
    @DisplayName("★ 气象站：全部字段（阵风只有它提供）")
    void weatherStationProvidesEverything() {
        final WindReadout.Fields f = WindReadout.Fields.ALL;
        assertTrue(f.fromDirection());
        assertTrue(f.toDirection());
        assertTrue(f.speed());
        assertTrue(f.gust(), "阵风只由气象站提供");
        assertTrue(f.temperature());
        assertTrue(f.precipitation());
    }

    @Test
    @DisplayName("★ 黄铜级气象站与安山级仪表的差异是三件套：抗风 / 口径 / 字段")
    void brassTierDiffersFromAndesite() {
        final WindReadout.Fields andesiteFields = WindReadout.Fields.DIRECTIONS_ONLY;
        final WindReadout.Fields brassFields = WindReadout.Fields.ALL;

        assertFalse(andesiteFields.temperature(), "安山级不该有温度");
        assertFalse(andesiteFields.precipitation(), "安山级不该有降水");
        assertTrue(brassFields.temperature() && brassFields.precipitation(),
                "黄铜级必须给温度与降水");
    }

    @Test
    @DisplayName("★ 阵风只由气象站提供 —— 风速计与风向标都不能报阵风")
    void gustBelongsToWeatherStationAlone() {
        assertFalse(WindReadout.Fields.SPEED_ONLY.gust());
        assertFalse(WindReadout.Fields.DIRECTIONS_ONLY.gust());
        assertTrue(WindReadout.Fields.ALL.gust());
    }

    @Test
    @DisplayName("三个仪表的字段集互不相同 —— 防止被改成同一套")
    void theThreeFieldSetsAreDistinct() {
        assertFalse(WindReadout.Fields.DIRECTIONS_ONLY.equals(WindReadout.Fields.SPEED_ONLY),
                "风向标与风速计的字段集不该相同");
        assertFalse(WindReadout.Fields.DIRECTIONS_ONLY.equals(WindReadout.Fields.ALL));
        assertFalse(WindReadout.Fields.SPEED_ONLY.equals(WindReadout.Fields.ALL));

        final WindReadout.Fields d = WindReadout.Fields.DIRECTIONS_ONLY;
        final WindReadout.Fields s = WindReadout.Fields.SPEED_ONLY;
        assertFalse(d.fromDirection() && s.fromDirection(), "来向不该被两个仪表同时提供");
        assertFalse(d.toDirection() && s.toDirection(), "去向不该被两个仪表同时提供");
        assertFalse(d.speed() && s.speed(), "风速不该被两个仪表同时提供");
        assertFalse(d.gust() && s.gust(), "阵风不该被两个仪表同时提供");
    }

    @Test
    @DisplayName("any() 的语义：只要有任一字段就算有内容")
    void anyReflectsPresenceOfAnyField() {
        assertTrue(WindReadout.Fields.DIRECTIONS_ONLY.any());
        assertTrue(WindReadout.Fields.SPEED_ONLY.any());
        assertTrue(WindReadout.Fields.ALL.any());
        assertFalse(new WindReadout.Fields(false, false, false, false, false, false).any(),
                "全 false 的字段集不该被认为有内容 —— 呈现层用它决定要不要显示面板");

        assertTrue(new WindReadout.Fields(true, false, false, false, false, false).any(),
                "只有来向也必须算有内容");
        assertTrue(new WindReadout.Fields(false, true, false, false, false, false).any(),
                "只有去向也必须算有内容");
    }

    @Test
    @DisplayName("读数到角度的转换是唯一的（§5.2：不允许第二处 atan2）")
    void angleMethodsAreConsistent() {
        final WindReadout r = new WindReadout(198.0F, 18.0F, 6.21F, 0.65F, 5.8F, 9.9F, 0.0F, true);
        final double diff = Math.abs(cn.chumenta.crosswind.wind.WindMath.deltaDegrees(
                r.fromDegrees(), r.toDegrees()));
        System.out.printf("to=%.0f° from=%.0f° ⇒ 差值 %.1f°%n",
                r.toDegrees(), r.fromDegrees(), diff);
        assertEquals(180.0D, diff, 1.0E-6D, "来向与去向必须恒差 180°");
    }

    @Test
    @DisplayName("★ 方位分档：16 档，且「偏」向与数值一致")
    void cardinalTiersAreNumericallyCorrect() {
        for (int i = 0; i < 16; i++) {
            assertEquals(i, WindDirection.index(i * 22.5D), "档位 " + i + " 计算错误");
        }

        assertEquals(12, WindDirection.index(-90.0D), "-90° 应归一到 270°（西，档 12）");
        assertEquals(4, WindDirection.index(450.0D), "450° 应归一到 90°（东，档 4）");
        assertEquals(0, WindDirection.index(359.0D), "359° 应落在北档");

        assertEquals("gui.crosswind.dir.nne", WindDirection.cardinalKey(22.5D));
        assertEquals("gui.crosswind.dir.ene", WindDirection.cardinalKey(67.5D));
        assertEquals("gui.crosswind.dir.n", WindDirection.cardinalKey(0.0D));
        assertEquals("gui.crosswind.dir.e", WindDirection.cardinalKey(90.0D));
        assertEquals("gui.crosswind.dir.s", WindDirection.cardinalKey(180.0D));
        assertEquals("gui.crosswind.dir.w", WindDirection.cardinalKey(270.0D));
    }

    @Test
    @DisplayName("★ 语言文件必须把 16 个方位键全部填上，且中英都不含拼接错误")
    void langFilesCoverAllDirections() throws Exception {
        final var zh = readLang("zh_cn");
        final var en = readLang("en_us");
        final String[] expectZh = {"北", "北偏东", "东北", "东偏北", "东", "东偏南", "东南", "南偏东",
                "南", "南偏西", "西南", "西偏南", "西", "西偏北", "西北", "北偏西"};
        for (int i = 0; i < 16; i++) {
            final String key = "gui.crosswind.dir." + WindDirection.KEY_SUFFIXES[i];
            assertTrue(zh.containsKey(key), "zh_cn 缺少方位键 " + key);
            assertTrue(en.containsKey(key), "en_us 缺少方位键 " + key);

            assertEquals(expectZh[i], zh.get(key), "zh_cn 的 " + key + " 表述不对");

            assertEquals(WindDirection.ENGLISH_SHORT[i], en.get(key),
                    "en_us 的 " + key + " 应为国际三字母缩写");
        }

        for (int i = 0; i < 16; i++) {
            final String v = zh.get("gui.crosswind.dir." + WindDirection.KEY_SUFFIXES[i]);
            assertFalse(v.contains("北东北") || v.contains("东东北"),
                    "方位名不该出现三段拼接：" + v);
        }
    }

    private static java.util.Map<String, String> readLang(final String locale) throws Exception {
        final java.nio.file.Path p = java.nio.file.Path.of(
                "src/main/resources/assets/crosswind/lang/" + locale + ".json");
        assertTrue(java.nio.file.Files.exists(p), "语言文件不存在：" + p.toAbsolutePath());

        final var map = new java.util.HashMap<String, String>();

        final var re = java.util.regex.Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");
        final var m = re.matcher(java.nio.file.Files.readString(p,
                java.nio.charset.StandardCharsets.UTF_8));
        while (m.find()) {
            map.put(m.group(1), m.group(2));
        }
        return map;
    }

    @Test
    @DisplayName("英文缩写与中文名必须是同一套档位（不能只改一边）")
    void cardinalShortMatchesChineseTiers() {
        for (int i = 0; i < 16; i++) {
            assertEquals(WindDirection.ENGLISH_SHORT[i], WindDirection.cardinalShort(i * 22.5D),
                    "档位 " + i + " 的英文缩写不对");
        }
    }

    private static final String[] UI_KEYS = {
            "gui.crosswind.goggle.cup_speed",

            "gui.crosswind.goggle.second_speed",
            "gui.crosswind.goggle.speed",

            "gui.crosswind.goggle.wind",
            "gui.crosswind.goggle.temperature",
            "gui.crosswind.goggle.precipitation",
            "gui.crosswind.goggle.wind_from",
            "gui.crosswind.goggle.wind_to",
            "gui.crosswind.goggle.wind_from_to",

            "crosswind.display_source.terminal_speed",
            "crosswind.display_source.terminal_direction",

            "crosswind.display_source.aviation_weather",

            "crosswind.display_source.terminal_wind",
            "crosswind.display_source.terminal_wind.title",
            "crosswind.display_source.terminal_wind.terminal_speed",
            "crosswind.display_source.terminal_wind.terminal_direction",
            "crosswind.display_source.terminal_wind.wind_from",
            "crosswind.display_source.terminal_wind.wind_to",
            "crosswind.display_source.terminal_wind.temperature",
            "crosswind.display_source.terminal_wind.precipitation",
    };

    @Test
    @DisplayName("★ 护目镜/显示板的文本键在中英两侧必须都存在（不能只加一边）")
    void langFilesCoverAllUiKeys() throws Exception {
        final var zh = readLang("zh_cn");
        final var en = readLang("en_us");
        for (final String key : UI_KEYS) {
            assertTrue(zh.containsKey(key), "zh_cn 缺少 UI 键 " + key);
            assertTrue(en.containsKey(key), "en_us 缺少 UI 键 " + key);
            assertFalse(zh.get(key).isBlank(), "zh_cn 的 " + key + " 不能为空");
            assertFalse(en.get(key).isBlank(), "en_us 的 " + key + " 不能为空");
        }

        for (final String stale : new String[]{
                "gui.crosswind.display.wind_dir", "gui.crosswind.display.wind_from",
                "gui.crosswind.display.wind_to", "gui.crosswind.display.cup_speed",
                "gui.crosswind.display.wind_speed"}) {
            assertFalse(zh.containsKey(stale), "已弃用的键不该留在 zh_cn：" + stale);
            assertFalse(en.containsKey(stale), "已弃用的键不该留在 en_us：" + stale);
        }
    }

    @Test
    @DisplayName("★ 合成风向行的占位符：中英都用 %1$s/%2$s，且顺序一致（语序不同不能靠拼接）")
    void fromToFormatUsesPositionalPlaceholders() throws Exception {
        final var zh = readLang("zh_cn");
        final var en = readLang("en_us");
        final String key = "gui.crosswind.goggle.wind_from_to";
        final String zhVal = zh.get(key);
        final String enVal = en.get(key);

        assertTrue(zhVal.contains("%1$s") && zhVal.contains("%2$s"),
                "中文格式必须用 %1$s / %2$s，实际：" + zhVal);
        assertTrue(enVal.contains("%1$s") && enVal.contains("%2$s"),
                "英文格式必须用 %1$s / %2$s，实际：" + enVal);

        assertTrue(zhVal.indexOf("%1$s") < zhVal.indexOf("%2$s"),
                "中文里来向应排在去向之前：" + zhVal);
        assertTrue(enVal.indexOf("%1$s") < enVal.indexOf("%2$s"),
                "英文里来向应排在去向之前：" + enVal);

        assertTrue(zhVal.startsWith("自") && zhVal.contains("吹向"),
                "中文应为「自 X 吹向 Y」形态，实际：" + zhVal);
    }
}
