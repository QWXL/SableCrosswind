package cn.chumenta.crosswind;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CrosswindConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue WIND_ENABLED;
    public static final ModConfigSpec.DoubleValue BASE_SPEED;
    public static final ModConfigSpec.DoubleValue MAX_SPEED;
    public static final ModConfigSpec.IntValue TRANSITION_TICKS;
    public static final ModConfigSpec.IntValue SEED_TRANSITION_TICKS;
    public static final ModConfigSpec.BooleanValue SEED_AUTO_ROTATE;
    public static final ModConfigSpec.IntValue GRID_SIZE;
    public static final ModConfigSpec.IntValue CACHE_LIMIT;

    public static final ModConfigSpec.BooleanValue TERRAIN_ENABLED;
    public static final ModConfigSpec.DoubleValue RIDGE_ACCEL;
    public static final ModConfigSpec.DoubleValue LEE_DECEL;
    public static final ModConfigSpec.DoubleValue LEE_RANGE;
    public static final ModConfigSpec.DoubleValue VALLEY_DEFLECTION_DEG;

    public static final ModConfigSpec.BooleanValue PRESSURE_AFFECTS_FORCE;

    public static final ModConfigSpec.BooleanValue PRESSURE_AFFECTS_WIND;

    public static final ModConfigSpec.DoubleValue INSTRUMENT_BREAK_SPEED_ANDESITE;

    public static final ModConfigSpec.DoubleValue INSTRUMENT_CUP_WINDOW_SECONDS;

    public static final ModConfigSpec.DoubleValue PRESSURE_REFERENCE;
    public static final ModConfigSpec.DoubleValue PRESSURE_EXPONENT;
    public static final ModConfigSpec.DoubleValue PRESSURE_MIN_FACTOR;
    public static final ModConfigSpec.DoubleValue PRESSURE_MAX_FACTOR;

    public static final ModConfigSpec.BooleanValue SABLE_ENABLED;
    public static final ModConfigSpec.DoubleValue SABLE_MAX_FORCE_PER_SUBSTEP;
    public static final ModConfigSpec.DoubleValue SABLE_MASS_EXPONENT;
    public static final ModConfigSpec.DoubleValue SABLE_MAX_ACCEL_G;
    public static final ModConfigSpec.DoubleValue SABLE_FORCE_EXPONENT;
    public static final ModConfigSpec.DoubleValue SABLE_FORCE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue SABLE_COARSE_WIND;
    public static final ModConfigSpec.DoubleValue SABLE_DRAG_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue SABLE_AIR_DENSITY;
    public static final ModConfigSpec.BooleanValue SABLE_LOGGING;

    public static final ModConfigSpec.BooleanValue ENTITY_ENABLED;
    public static final ModConfigSpec.DoubleValue NON_SLIP_BOOTS_SLIDE_FACTOR;
    public static final ModConfigSpec.DoubleValue ENTITY_MASS_COEFFICIENT;
    public static final ModConfigSpec.DoubleValue ENTITY_MIN_VOLUME;
    public static final ModConfigSpec.DoubleValue ENTITY_RESISTANCE_EXPONENT;
    public static final ModConfigSpec.DoubleValue ENTITY_REFERENCE_VOLUME;
    public static final ModConfigSpec.DoubleValue ENTITY_MAX_ACCELERATION;
    public static final ModConfigSpec.BooleanValue ENTITY_AFFECT_PLAYERS;
    public static final ModConfigSpec.BooleanValue ENTITY_USE_FORCE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue ENTITY_MIN_SYNC_SPEED;
    public static final ModConfigSpec.BooleanValue ENTITY_SABLE_GRACE;
    public static final ModConfigSpec.IntValue ENTITY_SYNC_INTERVAL;

    public static final ModConfigSpec.BooleanValue GUST_ENABLED;
    public static final ModConfigSpec.DoubleValue GUST_STRENGTH;

    public static final ModConfigSpec.IntValue INSTRUMENT_SYNC_INTERVAL;
    public static final ModConfigSpec.DoubleValue INSTRUMENT_ANGLE_TOLERANCE;
    public static final ModConfigSpec.DoubleValue INSTRUMENT_SPEED_TOLERANCE;
    public static final ModConfigSpec.DoubleValue INSTRUMENT_TEMP_TOLERANCE;
    public static final ModConfigSpec.DoubleValue INSTRUMENT_PRECIP_TOLERANCE;

    public static final ModConfigSpec.BooleanValue USE_BIOME_TEMPERATURE;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_DAY_OFFSET;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_NIGHT_OFFSET;
    public static final ModConfigSpec.DoubleValue TEMPERATURE_FLUCTUATION;
    public static final ModConfigSpec.DoubleValue PRECIPITATION_FLUCTUATION;

    static {
        final ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("风场基础参数")
                .push("wind");

        WIND_ENABLED = b
                .comment("是否启用本 Mod")
                .define("enabled", true);

        BASE_SPEED = b
                .comment("基础风速下限，单位 m/s")
                .defineInRange("base_speed", 4.0D, 0.0D, 200.0D);

        MAX_SPEED = b
                .comment("基础风速上限，单位 m/s")
                .defineInRange("max_speed", 20.0D, 0.0D, 200.0D);

        TRANSITION_TICKS = b
                .comment("风况切换周期，单位 tick")
                .defineInRange("transition_ticks", 600, 20, 24000);

        SEED_TRANSITION_TICKS = b
                .comment("更换种子后的过渡时长，单位 tick")
                .defineInRange("seed_transition_ticks", 1200, 0, 72000);

        SEED_AUTO_ROTATE = b
                .comment("是否每游戏日自动刷新风场种子")
                .define("seed_auto_rotate", true);

        GRID_SIZE = b
                .comment("风场空间采样格的边长，单位格。决定风场的空间尺度")
                .defineInRange("grid_size", 16, 2, 256);

        CACHE_LIMIT = b
                .comment("风场格点缓存的条目上限，超出时整个缓存清空")
                .defineInRange("cache_limit", 65536, 256, 1048576);

        b.comment("地形对风的修正")
                .push("terrain");

        TERRAIN_ENABLED = b
                .comment("是否启用地形修正")
                .define("enabled", true);

        RIDGE_ACCEL = b
                .comment("迎风坡加速系数上限。0 为关闭")
                .defineInRange("ridge_accel", 0.25D, 0.0D, 2.0D);

        LEE_DECEL = b
                .comment("背风减速系数上限。0 为关闭")
                .defineInRange("lee_decel", 0.5D, 0.0D, 1.0D);

        LEE_RANGE = b
                .comment("背风侧风速的恢复距离，单位格")
                .defineInRange("lee_range", 64.0D, 1.0D, 512.0D);

        VALLEY_DEFLECTION_DEG = b
                .comment("峡谷导流的最大偏转角，单位度。0 为关闭")
                .defineInRange("valley_deflection_deg", 20.0D, 0.0D, 90.0D);

        b.pop();

        b.comment("气压相关参数")
                .push("pressure");

        PRESSURE_AFFECTS_FORCE = b
                .comment("是否按气压缩放风力")
                .define("affects_force", true);

        INSTRUMENT_CUP_WINDOW_SECONDS = b
                .comment("风速计读数的滑动窗口长度，单位秒")
                .defineInRange("cup_window_seconds", 2.0D, 0.05D, 30.0D);

        INSTRUMENT_BREAK_SPEED_ANDESITE = b
                .comment("安山级仪表的抗风上限，单位 m/s。0 为禁用破坏机制")
                .defineInRange("break_speed_andesite", 30.0D, 0.0D, 500.0D);

        PRESSURE_AFFECTS_WIND = b
                .comment("是否让风速随气压降低而升高")
                .define("affects_wind", true);

        PRESSURE_REFERENCE = b
                .comment("基准气压，此处施力倍率为 1.0")
                .defineInRange("reference", 1.0D, 1.0E-6D, 1.0E6D);

        PRESSURE_EXPONENT = b
                .comment("气压缩放指数。倍率 = (气压 / 基准气压)^指数")
                .defineInRange("exponent", 1.0D, 0.0D, 4.0D);

        PRESSURE_MIN_FACTOR = b
                .comment("施力倍率下限")
                .defineInRange("min_factor", 0.25D, 0.0D, 100.0D);

        PRESSURE_MAX_FACTOR = b
                .comment("施力倍率上限")
                .defineInRange("max_factor", 4.0D, 0.0D, 100.0D);

        b.pop();

        b.comment("对 Sable 施力")
                .push("sable");

        SABLE_ENABLED = b
                .comment("是否对 Sable 子世界施加风力")
                .define("enabled", true);

        SABLE_MAX_FORCE_PER_SUBSTEP = b
                .comment("每个物理子步施加给单个子世界的力上限，单位 N。0 为不限制")
                .defineInRange("max_force_per_substep", 200000.0D, 0.0D, 1.0E9D);

        SABLE_MASS_EXPONENT = b
                .comment("质量加权指数 α")
                .defineInRange("mass_exponent", 1.0D, 0.0D, 3.0D);

        SABLE_MAX_ACCEL_G = b
                .comment("风力能给单个子世界的最大加速度，单位 g（1 g = 11 m/s²）。0 为不限制")
                .defineInRange("max_acceleration_g", 0.5D, 0.0D, 100.0D);

        SABLE_FORCE_EXPONENT = b
                .comment("风速到风力的响应指数 β")
                .defineInRange("force_exponent", 1.0D, 0.0D, 3.0D);

        SABLE_FORCE_MULTIPLIER = b
                .comment("风力总强度倍率")
                .defineInRange("force_multiplier", 0.1D, 0.0D, 1000.0D);

        SABLE_COARSE_WIND = b
                .comment("是否对没有帆的子世界也施加风力")
                .define("coarse_wind", true);

        SABLE_DRAG_COEFFICIENT = b
                .comment("粗粒度风力的阻力系数 Cd")
                .defineInRange("drag_coefficient", 1.05D, 0.0D, 5.0D);

        SABLE_AIR_DENSITY = b
                .comment("基准空气密度，单位 kg/m³")
                .defineInRange("air_density", 1.225D, 0.0D, 100.0D);

        SABLE_LOGGING = b
                .comment("是否在日志中打印调试信息")
                .define("logging", false);

        b.pop();

        b.comment("对实体施力")
                .push("entity");

        ENTITY_ENABLED = b
                .comment("是否让普通实体受风")
                .define("enabled", true);

        NON_SLIP_BOOTS_SLIDE_FACTOR = b
                .comment("防滑鞋对风致位移的额外折减系数")
                .defineInRange("non_slip_boots_slide_factor", 0.25D, 0.0D, 1.0D);

        ENTITY_MASS_COEFFICIENT = b
                .comment("体积换算成有效质量的系数 k_m，单位 kg/m³")
                .defineInRange("mass_coefficient", 3000.0D, 0.1D, 100000.0D);

        ENTITY_MIN_VOLUME = b
                .comment("计算有效质量时用的体积下限 V_min，单位 m³")
                .defineInRange("min_volume", 0.01D, 0.0D, 10.0D);

        ENTITY_RESISTANCE_EXPONENT = b
                .comment("抗风能力的体积指数 k_resist")
                .defineInRange("resistance_exponent", 0.0D, 0.0D, 3.0D);

        ENTITY_REFERENCE_VOLUME = b
                .comment("抗风因子的参考体积 V_ref，单位 m³")
                .defineInRange("reference_volume", 8.0D, 0.001D, 1000.0D);

        ENTITY_MAX_ACCELERATION = b
                .comment("实体的风力加速度上限，单位 m/s²。0 为不限制")
                .defineInRange("max_acceleration", 0.5D, 0.0D, 100.0D);

        ENTITY_AFFECT_PLAYERS = b
                .comment("玩家是否受风")
                .define("affect_players", true);

        ENTITY_USE_FORCE_MULTIPLIER = b
                .comment("实体风力是否也乘 wind.sable.force_multiplier")
                .define("use_force_multiplier", false);

        ENTITY_SYNC_INTERVAL = b
                .comment("风力增量的服务器发包间隔")
                .defineInRange("sync_interval", 5, 1, 100);

        ENTITY_SABLE_GRACE = b
                .comment("从 Sable 子世界离开后、触地或入水之前是否不施风")
                .define("sable_grace", true);

        ENTITY_MIN_SYNC_SPEED = b
                .comment("低于这个速度就不发包，单位 m/s。0 = 关闭")
                .defineInRange("min_sync_speed", 0.0D, 0.0D, 10.0D);

        b.pop();

        b.comment("阵风")
                .push("gust");

        GUST_ENABLED = b
                .comment("是否启用阵风")
                .define("enabled", true);

        GUST_STRENGTH = b
                .comment("阵风强度上限，相对基础风的比例")
                .defineInRange("strength", 0.35D, 0.0D, 2.0D);

        b.pop();

        b.comment("风速仪表的同步行为")
                .push("instrument");

        INSTRUMENT_SYNC_INTERVAL = b
                .comment("同步间隔，单位 tick")
                .defineInRange("sync_interval", 10, 1, 200);

        INSTRUMENT_ANGLE_TOLERANCE = b
                .comment("风向变化超过这个角度才立即同步，单位度")
                .defineInRange("angle_tolerance_deg", 2.0D, 0.0D, 180.0D);

        INSTRUMENT_SPEED_TOLERANCE = b
                .comment("风速或阵风变化超过这个值才立即同步，单位 m/s")
                .defineInRange("speed_tolerance_mps", 0.15D, 0.0D, 50.0D);

        INSTRUMENT_TEMP_TOLERANCE = b
                .comment("温度变化超过这个值才立即同步，单位 °C")
                .defineInRange("temp_tolerance_c", 0.25D, 0.0D, 50.0D);

        INSTRUMENT_PRECIP_TOLERANCE = b
                .comment("降水强度变化超过这个值才立即同步，范围 0..1")
                .defineInRange("precip_tolerance", 0.05D, 0.0D, 1.0D);

        b.pop();

        b.comment("温度与降水")
                .push("climate");

        USE_BIOME_TEMPERATURE = b
                .comment("是否使用生物群系温度估算气温")
                .define("use_biome_temperature", true);

        TEMPERATURE_DAY_OFFSET = b
                .comment("白天最热时相对群系基准温度的气温偏移，单位 °C")
                .defineInRange("day_offset", 4.0D, -50.0D, 50.0D);

        TEMPERATURE_NIGHT_OFFSET = b
                .comment("夜间最冷时相对群系基准温度的气温偏移，单位 °C")
                .defineInRange("night_offset", -6.0D, -50.0D, 50.0D);

        TEMPERATURE_FLUCTUATION = b
                .comment("气温的微波动幅度，单位 ±°C。0 为关闭波动")
                .defineInRange("fluctuation", 1.5D, 0.0D, 20.0D);

        PRECIPITATION_FLUCTUATION = b
                .comment("降水的相对波动幅度（0.15 = ±15%）。0 为关闭波动")
                .defineInRange("precipitation_fluctuation", 0.15D, 0.0D, 1.0D);

        b.pop();

        SPEC = b.build();
    }

    private CrosswindConfig() {
    }

    public static int instrumentCupWindowTicks() {
        return Math.max(1, (int) Math.round(INSTRUMENT_CUP_WINDOW_SECONDS.get() * 20.0D));
    }
}
