package cn.chumenta.crosswind.wind;

import net.minecraft.world.level.Level;

import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class WindPressure {
    private WindPressure() {
    }

    public static double sample(final Level level, final double x, final double y, final double z) {
        return DimensionPhysicsData.getAirPressure(level, new Vector3d(x, y, z));
    }

    public static double sample(final Level level, final Vector3dc pos) {
        return DimensionPhysicsData.getAirPressure(level, pos);
    }
}
