package cn.chumenta.crosswind;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CrosswindClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue WIND_PARTICLES_ENABLED;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_THRESHOLD;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_DENSITY;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_SIZE;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_DOT_SPACING;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_LENGTH_BLOCKS;

    public static final ModConfigSpec.IntValue WIND_PARTICLE_SWEEP_TICKS;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_LIFETIME_SECONDS;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_SPREAD_DEG;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_MIN_DISTANCE;

    public static final ModConfigSpec.DoubleValue WIND_PARTICLE_MAX_DISTANCE;

    public static final ModConfigSpec.BooleanValue CUP_SPIN_REVERSED;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("粒子效果")
                .push("wind_particles");

        WIND_PARTICLES_ENABLED = b
                .comment("是否显示风纹粒子")
                .define("enabled", true);

        WIND_PARTICLE_THRESHOLD = b
                .comment("出现风纹的风速下限，单位 m/s")
                .defineInRange("threshold", 10.0D, 0.0D, 200.0D);

        WIND_PARTICLE_DENSITY = b
                .comment("密度")
                .defineInRange("density", 0.15D, 0.0D, 10.0D);

        WIND_PARTICLE_SIZE = b
                .comment("单个点的尺寸，单位格")
                .defineInRange("size", 0.20D, 0.02D, 2.0D);

        WIND_PARTICLE_DOT_SPACING = b
                .comment("相邻点的间距，单位格")
                .defineInRange("dot_spacing", 0.16D, 0.02D, 4.0D);

        WIND_PARTICLE_LENGTH_BLOCKS = b
                .comment("由粒子排列成的宏观风纹的长度，单位格")
                .defineInRange("length_blocks", 3.0D, 0.2D, 24.0D);

        WIND_PARTICLE_SWEEP_TICKS = b
                .comment("风纹速度")
                .defineInRange("sweep_ticks", 10, 1, 60);

        WIND_PARTICLE_LIFETIME_SECONDS = b
                .comment("单个点的寿命，单位秒")
                .defineInRange("lifetime_seconds", 1.75D, 0.2D, 20.0D);

        WIND_PARTICLE_SPREAD_DEG = b
                .comment("生成方向的半张角，单位度")
                .defineInRange("spread_degrees", 180.0D, 10.0D, 180.0D);

        WIND_PARTICLE_MIN_DISTANCE = b
                .comment("生成时必须距玩家的最小距离，单位格")
                .defineInRange("min_distance", 16.0D, 1.0D, 128.0D);

        WIND_PARTICLE_MAX_DISTANCE = b
                .comment("生成时距玩家的最大距离，单位格")
                .defineInRange("max_distance", 30.0D, 2.0D, 30.0D);

        b.pop();

        b.comment("仪表")
                .push("instruments");

        CUP_SPIN_REVERSED = b
                .comment("反向风杯旋转")
                .define("cup_spin_reversed", false);

        b.pop();

        SPEC = b.build();
    }

    private CrosswindClientConfig() {
    }
}
