package cn.chumenta.crosswind.instrument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolePlacementTest {
    private static java.util.function.IntPredicate at(final Set<Integer> ys) {
        return ys::contains;
    }

    @Test
    @DisplayName("★ 点击列的顶端（上方为空）⇒ 在最高那根上面接一根")
    void clickingTopExtendsUpward() {
        final Set<Integer> col = Set.of(64, 65, 66);

        assertEquals(67, PolePlacement.targetY(66, +1, at(col)),
                "应放到最高的 66 之上，即 67");
    }

    @Test
    @DisplayName("★ 点击列的底端（下方为空）⇒ 在最低那根下面接一根")
    void clickingBottomExtendsDownward() {
        final Set<Integer> col = Set.of(64, 65, 66);

        assertEquals(63, PolePlacement.targetY(64, -1, at(col)),
                "应放到最低的 64 之下，即 63");
    }

    @Test
    @DisplayName("★ 点击列的中间 ⇒ 仍然延伸到端点再接（不是紧挨着点击位置）")
    void clickingMiddleStillGoesToTheEnd() {
        final Set<Integer> col = Set.of(64, 65, 66);

        assertEquals(67, PolePlacement.targetY(65, +1, at(col)));

        assertEquals(63, PolePlacement.targetY(65, -1, at(col)));
    }

    @Test
    @DisplayName("相邻方向没有立柱 ⇒ 单根不触发（点击位置自己就是整列）")
    void singlePoleExtendsBothWays() {
        final Set<Integer> single = Set.of(65);
        assertEquals(66, PolePlacement.targetY(65, +1, at(single)), "单根向上接一根");
        assertEquals(64, PolePlacement.targetY(65, -1, at(single)), "单根向下接一根");
    }

    @Test
    @DisplayName("点击的位置不是立柱 ⇒ 不接管")
    void clickingNonPoleDoesNotTakeOver() {
        final Set<Integer> col = Set.of(64, 65, 66);

        assertEquals(PolePlacement.NO_PLACEMENT, PolePlacement.targetY(67, +1, at(col)));
        assertEquals(PolePlacement.NO_PLACEMENT, PolePlacement.targetY(63, -1, at(col)));
    }

    @Test
    @DisplayName("长列的端点定位正确（10 根）")
    void longColumn() {
        final Set<Integer> col = new java.util.HashSet<>();
        for (int y = 60; y < 70; y++) {
            col.add(y);
        }
        assertEquals(70, PolePlacement.targetY(60, +1, at(col)), "顶端接在 69 之上");
        assertEquals(59, PolePlacement.targetY(69, -1, at(col)), "底端接在 60 之下");
        assertEquals(70, PolePlacement.targetY(65, +1, at(col)), "从中间也到顶端");
        assertEquals(59, PolePlacement.targetY(65, -1, at(col)), "从中间也到底端");
    }

    @Test
    @DisplayName("findEnd 找到的是该方向的最后一根（不是第一根）")
    void findEndStopsAtTheLastOne() {
        final Set<Integer> col = Set.of(10, 11, 12, 13);
        assertEquals(10, PolePlacement.findEnd(11, -1, at(col)), "向下走应停在 10");
        assertEquals(13, PolePlacement.findEnd(11, +1, at(col)), "向上走应停在 13");

        final Set<Integer> gapped = Set.of(10, 11, 13, 14);
        assertEquals(14, PolePlacement.findEnd(14, +1, at(gapped)), "从 14 出发，15 为空 ⇒ 停在 14");
    }

    @Test
    @DisplayName("断开的两段被当成两列（不会跨过空隙）")
    void gapsAreNotCrossed() {
        final Set<Integer> gapped = Set.of(64, 65, 70, 71);

        assertEquals(66, PolePlacement.targetY(65, +1, at(gapped)),
                "列在 65 结束，接到 66（不跨到 70 那段）");

        assertEquals(69, PolePlacement.targetY(71, -1, at(gapped)),
                "列在 70 结束，接到 69（不跨到 65 那段）");
    }

    @Test
    @DisplayName("★ 方向掩码：仪表(mask=2) 只能向下延伸，向上不接管")
    void instrumentOnlyExtendsDownward() {
        final Set<Integer> col = Set.of(64, 65, 66);

        assertEquals(63, PolePlacement.targetY(64, -1, at(col), 2), "仪表向下延伸");

        assertEquals(PolePlacement.NO_PLACEMENT, PolePlacement.targetY(66, +1, at(col), 2),
                "仪表上方不接（它上面永远不会有立柱）");
    }

    @Test
    @DisplayName("★ 方向掩码：杆子(mask=3) 上下都能延伸")
    void rodExtendsBothWays() {
        final Set<Integer> col = Set.of(64, 65, 66);
        assertEquals(67, PolePlacement.targetY(66, +1, at(col), 3), "杆子向上延伸");
        assertEquals(63, PolePlacement.targetY(64, -1, at(col), 3), "杆子向下延伸");
    }

    @Test
    @DisplayName("★ 掩码为 0（不参与连接）时永不接管")
    void zeroMaskNeverTakesOver() {
        final Set<Integer> col = Set.of(64, 65, 66);
        assertEquals(PolePlacement.NO_PLACEMENT, PolePlacement.targetY(65, +1, at(col), 0));
        assertEquals(PolePlacement.NO_PLACEMENT, PolePlacement.targetY(65, -1, at(col), 0));
    }
}
