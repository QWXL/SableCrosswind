package cn.chumenta.crosswind.entity;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.world.entity.Entity;

public final class SubLevelProbe {
    private SubLevelProbe() {
    }

    public record Result(
            double feetX, double feetY, double feetZ,
            boolean onGround,
            int intersectingCount,
            boolean anyContainsFeet,
            double boxMinX, double boxMinY, double boxMinZ,
            double boxMaxX, double boxMaxY, double boxMaxZ,
            String subLevelId,
            String note) {
        public String summary() {
            return String.format(
                    "脚=(%.2f, %.2f, %.2f) onGround=%s ｜ 相交子世界=%d ｜ contains(脚)=%s ｜ "
                            + "盒=[%.2f,%.2f,%.2f]~[%.2f,%.2f,%.2f] ｜ %s ｜ %s",
                    this.feetX, this.feetY, this.feetZ, this.onGround,
                    this.intersectingCount, this.anyContainsFeet,
                    this.boxMinX, this.boxMinY, this.boxMinZ,
                    this.boxMaxX, this.boxMaxY, this.boxMaxZ,
                    this.subLevelId, this.note);
        }
    }

    public static Result probe(final Entity entity) {
        try {
            final var level = entity.level();
            final double cx = entity.getX();
            final double feetY = entity.getY();
            final double cz = entity.getZ();
            final boolean onGround = entity.onGround();

            final var point = new BoundingBox3d(cx, feetY, cz, cx, feetY, cz);
            int count = 0;
            boolean contains = false;
            double minX = Double.NaN, minY = Double.NaN, minZ = Double.NaN;
            double maxX = Double.NaN, maxY = Double.NaN, maxZ = Double.NaN;
            String id = "-";

            for (final var sub : dev.ryanhcode.sable.Sable.HELPER.getAllIntersecting(level, point)) {
                if (sub == null) {
                    continue;
                }
                count++;
                final var box = sub.boundingBox();
                if (box == null) {
                    continue;
                }
                if (count == 1) {
                    minX = box.minX();
                    minY = box.minY();
                    minZ = box.minZ();
                    maxX = box.maxX();
                    maxY = box.maxY();
                    maxZ = box.maxZ();
                    id = sub.getUniqueId() == null ? "-" : sub.getUniqueId().toString().substring(0, 8);
                }
                if (box.contains(cx, feetY, cz)) {
                    contains = true;
                }
            }

            return new Result(cx, feetY, cz, onGround, count, contains,
                    minX, minY, minZ, maxX, maxY, maxZ, id,
                    count == 0 ? "不在子世界上" : "");
        } catch (final Throwable t) {
            return new Result(entity.getX(), entity.getY(), entity.getZ(), entity.onGround(),
                    -1, false, Double.NaN, Double.NaN, Double.NaN,
                    Double.NaN, Double.NaN, Double.NaN, "-",
                    "探测异常：" + t.getClass().getSimpleName() + " " + t.getMessage());
        }
    }
}
