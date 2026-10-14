package cn.chumenta.crosswind.command;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.compat.sable.SubLevelTracker;
import cn.chumenta.crosswind.wind.TerrainSignature;
import cn.chumenta.crosswind.wind.WeatherSeedState;
import cn.chumenta.crosswind.wind.WindFieldService;
import cn.chumenta.crosswind.wind.WindMath;
import cn.chumenta.crosswind.wind.WindModifiers;
import cn.chumenta.crosswind.wind.WindSample;
import cn.chumenta.crosswind.wind.WindSettings;
import cn.chumenta.crosswind.wind.WindStages;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.UUID;

@EventBusSubscriber(modid = Crosswind.MOD_ID)
public final class CrosswindCommands {
    private CrosswindCommands() {
    }

    @SubscribeEvent
    public static void register(final RegisterCommandsEvent event) {
        final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("crosswind")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("wind")
                        .executes(ctx -> windAt(ctx, null, false))
                        .then(Commands.literal("at")
                                .executes(ctx -> windAt(ctx, null, false))
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> windAt(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), false))
                                        .then(Commands.literal("verbose")
                                                .executes(ctx -> windAt(ctx,
                                                        BlockPosArgument.getBlockPos(ctx, "pos"), true)))))
                        .then(Commands.literal("here")
                                .executes(ctx -> windAt(ctx, null, false))
                                .then(Commands.literal("verbose")
                                        .executes(ctx -> windAt(ctx, null, true))))
                        .then(Commands.literal("entity")
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> windAtEntity(ctx,
                                                EntityArgument.getEntity(ctx, "target")))))

                        .then(Commands.literal("instrument")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> instrumentAt(ctx,
                                                BlockPosArgument.getBlockPos(ctx, "pos")))))
                        .then(Commands.literal("profile")
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> windProfile(ctx, BlockPosArgument.getBlockPos(ctx, "pos"),
                                                8, 16, 320))))
                        .then(Commands.literal("line")
                                .then(Commands.argument("from", BlockPosArgument.blockPos())
                                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                                .executes(ctx -> windLine(ctx,
                                                        BlockPosArgument.getBlockPos(ctx, "from"),
                                                        BlockPosArgument.getBlockPos(ctx, "to"),
                                                        0.25D))
                                                .then(Commands.argument("step", DoubleArgumentType.doubleArg(0.05D, 64.0D))
                                                        .executes(ctx -> windLine(ctx,
                                                                BlockPosArgument.getBlockPos(ctx, "from"),
                                                                BlockPosArgument.getBlockPos(ctx, "to"),
                                                                DoubleArgumentType.getDouble(ctx, "step"))))))))

                .then(Commands.literal("seed")
                        .then(Commands.literal("get").executes(CrosswindCommands::seedGet))
                        .then(Commands.literal("random").executes(ctx -> seedSet(ctx, -1L)))
                        .then(Commands.literal("set")
                                .then(Commands.argument("value", LongArgumentType.longArg())
                                        .executes(ctx -> seedSet(ctx,
                                                LongArgumentType.getLong(ctx, "value"))))))

                .then(Commands.literal("terrain")
                        .executes(ctx -> terrainInfo(ctx, null, false))
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(ctx -> terrainInfo(ctx, BlockPosArgument.getBlockPos(ctx, "pos"), false))
                                .then(Commands.literal("rebuild")
                                        .executes(ctx -> terrainInfo(ctx,
                                                BlockPosArgument.getBlockPos(ctx, "pos"), true)))))

                .then(Commands.literal("sublevel")
                        .then(Commands.literal("list").executes(CrosswindCommands::subLevelList))

                        .then(Commands.literal("probe")
                                .executes(ctx -> subLevelProbe(ctx,
                                        ctx.getSource().getEntity() == null
                                                ? null : ctx.getSource().getEntity()))
                                .then(Commands.argument("target", EntityArgument.entity())
                                        .executes(ctx -> subLevelProbe(ctx,
                                                EntityArgument.getEntity(ctx, "target")))))

                        .then(Commands.literal("track")
                                .then(Commands.argument("target", SubLevelArgumentType.singleSubLevel())

                                        .suggests(CrosswindCommands::suggestSubLevelSelectors)
                                        .executes(CrosswindCommands::subLevelTrack)))

                        .then(Commands.literal("trackuuid")
                                .then(Commands.argument("uuid", StringArgumentType.string())
                                        .suggests(CrosswindCommands::suggestSubLevelUuids)
                                        .executes(ctx -> subLevelTrackUuid(ctx,
                                                StringArgumentType.getString(ctx, "uuid")))))
                        .then(Commands.literal("untrack").executes(CrosswindCommands::subLevelUntrack)))

                .then(Commands.literal("info").executes(CrosswindCommands::info)));
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            suggestSubLevelSelectors(final CommandContext<CommandSourceStack> ctx,
                                     final com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        builder.suggest("@v", Component.literal("正在看向的子世界"));
        builder.suggest("@n", Component.literal("最近的子世界"));
        builder.suggest("@i", Component.literal("所在的子世界"));
        builder.suggest("@l", Component.literal("最新创建的子世界"));
        builder.suggest("@t", Component.literal("正在被追踪的子世界"));
        builder.suggest("@r", Component.literal("随机一个子世界"));
        builder.suggest("@e", Component.literal("全部子世界"));

        final var container = SubLevelContainer.getContainer(ctx.getSource().getLevel());
        if (container != null) {
            for (final ServerSubLevel sub : container.getAllSubLevels()) {
                final var p = sub.logicalPose().position();
                builder.suggest(sub.getUniqueId().toString(), Component.literal(String.format(
                        "%s 位置(%.0f,%.0f,%.0f) 质量%.2f 气动件%d",
                        sub.getName() == null ? "未命名" : sub.getName(),
                        p.x(), p.y(), p.z(), safeMass(sub), safeProviderCount(sub))));
            }
        }
        return builder.buildFuture();
    }

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions>
            suggestSubLevelUuids(final CommandContext<CommandSourceStack> ctx,
                                 final com.mojang.brigadier.suggestion.SuggestionsBuilder builder) {
        final var container = SubLevelContainer.getContainer(ctx.getSource().getLevel());
        if (container != null) {
            final UUID tracked = SubLevelTracker.tracked();
            for (final ServerSubLevel sub : container.getAllSubLevels()) {
                final String id = sub.getUniqueId().toString();
                final var p = sub.logicalPose().position();
                final String tip = String.format("%s 位置(%.0f,%.0f,%.0f) 质量%.2f 气动件%d",
                        tracked != null && tracked.equals(sub.getUniqueId()) ? "追踪中 " : "",
                        p.x(), p.y(), p.z(), safeMass(sub), safeProviderCount(sub));
                builder.suggest(id, Component.literal(tip));
            }
        }
        return builder.buildFuture();
    }

    private static int subLevelProbe(final CommandContext<CommandSourceStack> ctx,
                                     final net.minecraft.world.entity.Entity target) {
        final CommandSourceStack source = ctx.getSource();
        if (target == null) {
            source.sendFailure(Component.literal("需要一个实体作为目标。"));
            return 0;
        }
        final var r = cn.chumenta.crosswind.entity.SubLevelProbe.probe(target);

        final boolean managed = cn.chumenta.crosswind.entity.EntityWindPolicy.isInsideSubLevel(target);
        source.sendSuccess(() -> Component.literal("子世界风力检测")
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.literal("  " + r.summary()), false);
        source.sendSuccess(() -> Component.literal(String.format(
                "当前%s",
                managed ? "→ 子世界继承速度" : "→ 独立施风")), false);
        return 1;
    }

    private static int subLevelList(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final var container = SubLevelContainer.getContainer(level);
        if (container == null) {
            source.sendFailure(Component.literal("这个维度没有 Sable 子世界容器。"));
            return 0;
        }
        final var all = container.getAllSubLevels();
        if (all.isEmpty()) {
            source.sendSuccess(() -> Component.literal("当前维度没有 Sable 子世界。")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        final UUID tracked = SubLevelTracker.tracked();
        source.sendSuccess(() -> Component.literal(String.format(
                        "当前维度 %d 个 Sable 子世界", all.size()))
                .withStyle(ChatFormatting.GOLD), false);

        for (final ServerSubLevel sub : all) {
            final var pose = sub.logicalPose();
            final var p = pose.position();
            final double mass = safeMass(sub);
            final double area = safeArea(sub);
            final int providers = safeProviderCount(sub);
            final boolean isTracked = tracked != null && tracked.equals(sub.getUniqueId());

            final String line = String.format(
                    "%s  (%s)  位置 (%.1f, %.1f, %.1f)  质量 %.2f  面积 %.2f m²  气动件 %d",
                    sub.getUniqueId(),
                    sub.getName() == null ? "未命名" : sub.getName(),
                    p.x(), p.y(), p.z(), mass, area, providers);

            source.sendSuccess(() -> Component.literal(line)
                    .withStyle(isTracked ? ChatFormatting.GREEN : ChatFormatting.GRAY), false);
        }

        return all.size();
    }

    private static int subLevelTrack(final CommandContext<CommandSourceStack> ctx)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        final ServerSubLevel sub = SubLevelArgumentType.getSingleSubLevel(ctx, "target");
        return beginTracking(ctx.getSource(), sub, sub.getUniqueId().toString());
    }

    private static int subLevelTrackUuid(final CommandContext<CommandSourceStack> ctx, final String raw) {
        final CommandSourceStack source = ctx.getSource();
        final UUID id;
        try {
            id = UUID.fromString(raw.trim());
        } catch (final IllegalArgumentException e) {
            source.sendFailure(Component.literal("UUID 格式错误：" + raw
                    ));
            return 0;
        }

        final var container = SubLevelContainer.getContainer(source.getLevel());

        final ServerSubLevel sub =
                (container != null && container.getSubLevel(id) instanceof final ServerSubLevel s)
                        ? s : null;
        if (sub == null) {
            source.sendFailure(Component.literal("当前维度找不到子世界 " + id));
            return 0;
        }
        return beginTracking(source, sub, id.toString());
    }

    private static int beginTracking(final CommandSourceStack source, final ServerSubLevel sub,
                                     final String label) {
        SubLevelTracker.track(sub.getUniqueId());
        source.sendSuccess(() -> Component.literal(String.format(
                        "开始追踪 %s（%s）质量 %.2f 面积 %.2f m² 气动件 %d 个 —— 输出到日志，每 tick 4 行",
                        label,
                        sub.getName() == null ? "未命名" : sub.getName(),
                        safeMass(sub), safeArea(sub), safeProviderCount(sub)))
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int subLevelUntrack(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final UUID t = SubLevelTracker.tracked();
        if (t == null) {
            source.sendSuccess(() -> Component.literal("当前没有在追踪任何子世界。")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }
        SubLevelTracker.untrack();
        source.sendSuccess(() -> Component.literal("已停止追踪 " + t)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static double safeMass(final ServerSubLevel sub) {
        try {
            final var md = sub.getMassTracker();
            return md.isInvalid() ? 0.0D : md.getMass();
        } catch (final Throwable t) {
            return 0.0D;
        }
    }

    private static double safeArea(final ServerSubLevel sub) {
        try {
            final var box = sub.boundingBox();
            if (box == null) {
                return 0.0D;
            }
            final var size = box.size();
            return Math.max(size.x, 0.0D) * Math.max(size.y, 0.0D) * Math.max(size.z, 0.0D);
        } catch (final Throwable t) {
            return 0.0D;
        }
    }

    private static int safeProviderCount(final ServerSubLevel sub) {
        try {
            final var plot = sub.getPlot();
            if (plot == null || plot.getLiftProviders() == null) {
                return 0;
            }
            return plot.getLiftProviders().size();
        } catch (final Throwable t) {
            return 0;
        }
    }

    private static int windAt(final CommandContext<CommandSourceStack> ctx, final BlockPos pos,
                              final boolean verbose) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final Vec3 where = resolvePos(source, pos);

        final WindSettings settings = WindSettings.fromConfig();
        final WindFieldService service = Crosswind.windService();

        if (!settings.enabled()) {
            source.sendSuccess(() -> Component.literal("[crosswind] 风场已禁用（wind.enabled = false）")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        final WindSample sample = service.sample(level, where.x, where.y, where.z, settings);

        source.sendSuccess(() -> Component.literal("风场 @ ")
                        .append(dim(level))
                        .append(Component.literal(String.format(" %.1f %.1f %.1f", where.x, where.y, where.z)))
                        .withStyle(ChatFormatting.GOLD),
                false);

        source.sendSuccess(() -> line("平均风速", String.format("%.2f m/s  (%.1f kt)",
                sample.speed(), sample.speedKnots())), false);
        source.sendSuccess(() -> line("阵风峰值", String.format("%.2f m/s  (%.1f kt)（增量 %.2f）",
                sample.peakSpeed(), sample.peakSpeed() * WindSample.KNOTS_PER_MPS,
                sample.peakSpeed() - sample.speed(),
                sample.gustComponent())), false);
        source.sendSuccess(() -> line("风向",
                String.format("%s %.0f°  (来向)  /  %.0f°  (去向)",
                        cardinal(sample.fromDegrees()), sample.fromDegrees(), sample.toDegrees())), false);
        source.sendSuccess(() -> line("气温", sample.climateAvailable()
                ? String.format("%.1f °C  [群系估值]", sample.temperatureC())
                : "未知"), false);
        source.sendSuccess(() -> line("降水", String.format("%.2f", sample.precipitation())), false);
        source.sendSuccess(() -> line("地形修正",
                sample.terrainApplied() ? "已应用" : "未应用"), false);
        source.sendSuccess(() -> line("向量", String.format("%.3f, %.3f, %.3f",
                sample.velocity().x, sample.velocity().y, sample.velocity().z)), false);

        if (verbose) {
            final WindStages st = service.sampleDetailed(level, where.x, where.y, where.z, settings);
            source.sendSuccess(() -> Component.literal("分项贡献").withStyle(ChatFormatting.GOLD), false);
            source.sendSuccess(() -> line("基础风", String.format("%.2f m/s  风向 %.0f°",
                    horizontal(st.baseVelocity().x, st.baseVelocity().z),
                    WindMath.wrapDegrees(Math.toDegrees(Math.atan2(st.baseVelocity().x, st.baseVelocity().z)) + 180.0D))), false);
            source.sendSuccess(() -> line("地形倍率", String.format("×%.3f", st.terrainFactor())), false);

            final double terrainWeight = WindModifiers.terrainAltitudeWeight(
                    where.y - st.terrainReferenceHeight());
            source.sendSuccess(() -> line("地形高度权重", String.format("%.3f （离地 %.0f 格）",
                    terrainWeight,
                    where.y - st.terrainReferenceHeight(),
                    WindModifiers.TERRAIN_FADE_START)), false);
            source.sendSuccess(() -> line("阵风增量", String.format("%.2f m/s",
                    st.gustSpeed())), false);

            source.sendSuccess(() -> line("气压", String.format("%.4f   施力倍数 ×%.3f",
                    st.pressure(), st.pressureForceScale())), false);

            final double pressureRatio = settings.pressureReference() > 0.0D
                    ? st.pressure() / settings.pressureReference() : 1.0D;
            final double pressureWindScale = WindModifiers.pressureWindMultiplier(pressureRatio);
            source.sendSuccess(() -> line("气压→风速", String.format("×%.3f   （气压/基准 = %.4f）",
                    settings.pressureAffectsWind() ? pressureWindScale : 1.0D,
                    pressureRatio)), false);

            source.sendSuccess(() -> line("气温构成", String.format("%.2f = %.2f(群系) %+.2f(时间) %+.2f(波动)",
                    st.temperatureC(), st.temperatureBiomeBaseC(),
                    st.temperatureDiurnalC(), st.temperatureFluctuationC())), false);
            source.sendSuccess(() -> line("群系", String.format("%s  (原始温度 %.2f%s)",
                    st.biomeId().isEmpty() ? "未加载" : st.biomeId(),
                    st.rawBiomeTemperature(),
                    st.isOcean() ? "  #is_ocean → 日变化相位反转" : "")), false);
            source.sendSuccess(() -> line("降水构成", String.format("%.3f = %.2f × %.2f",
                    st.precipitation(),
                    st.precipitationBase(),
                    st.precipitationBase() > 1.0E-9D
                            ? st.precipitation() / st.precipitationBase() : 1.0D)), false);

            final WeatherSeedState state = WeatherSeedState.get(level);
            final long gameTime = level.getGameTime();
            final long transition = Math.max(1L, settings.transitionTicks());
            source.sendSuccess(() -> line("种子", String.format("%d  (过渡进度 %.0f%%)",
                    state.seed(), state.transitionRatio(gameTime) * 100.0D)), false);
            source.sendSuccess(() -> line("时间槽", String.format("第 %d 槽，槽内进度 %.0f%%（槽长 %d tick）",
                    Math.floorDiv(gameTime, transition),
                    (gameTime - Math.floorDiv(gameTime, transition) * transition) * 100.0D / transition,
                    transition)), false);
            source.sendSuccess(() -> line("缓存", String.format("地形摘要 %d 条",
                    service.terrainCache().size())), false);
        }
        return 1;
    }

    private static int instrumentAt(final CommandContext<CommandSourceStack> ctx, final BlockPos pos) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();

        final var be = level.getBlockEntity(pos);
        if (be == null) {
            source.sendFailure(Component.literal("[crosswind] " + pos.toShortString()
                    + " 没有方块实体。"));
            return 0;
        }
        final boolean isInstrument = be instanceof cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity;
        final var readout = isInstrument
                ? ((cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity) be).readout()
                : null;

        source.sendSuccess(() -> Component.literal("仪表 @ " + pos.toShortString())
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> line("方块实体类型", be.getType().toString()), false);
        source.sendSuccess(() -> line("所在 level", level.dimension().location()
                + "   方块坐标 " + pos.toShortString()
                ), false);

        if (readout == null) {
            source.sendSuccess(() -> line("读数", "未知方块"), false);
            return 1;
        }

        source.sendSuccess(() -> line("① 仪表缓存",
                String.format("%.2f m/s   杯体 %.2f   阵风分量 %.2f",
                        readout.speedMps(), readout.cupSpeedMps(), readout.gustMps())), false);

        final WindSettings settings = WindSettings.fromConfig();
        final double[] samplePos = cn.chumenta.crosswind.instrument
                .AbstractWindInstrumentBlockEntity.samplePositionFor(level, pos);
        final WindSample fresh = Crosswind.windService()
                .sample(level, samplePos[0], samplePos[1], samplePos[2], settings);
        source.sendSuccess(() -> line("仪表采样点", String.format("%.1f %.1f %.1f",
                samplePos[0], samplePos[1], samplePos[2])), false);
        final boolean transformed = Math.abs(samplePos[0] - (pos.getX() + 0.5D)) > 1.0E-6D
                || Math.abs(samplePos[2] - (pos.getZ() + 0.5D)) > 1.0E-6D;
        source.sendSuccess(() -> line("是否为子世界", transformed
                ? "是"
                : "否"), false);
        source.sendSuccess(() -> line("② 同坐标现场采样",
                String.format("%.2f m/s   峰值 %.2f", fresh.speed(), fresh.peakSpeed())), false);

        final double diff = Math.abs(readout.speedMps() - fresh.speed());
        source.sendSuccess(() -> line("差异",
                String.format("%.2f m/s   %s", diff,
                        diff < 1.0D ? "一致"
                                : "不一致")), false);
        return 1;
    }

    private static int windAtEntity(final CommandContext<CommandSourceStack> ctx, final Entity target) {
        final Vec3 pos = target.position();
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final WindSample sample = Crosswind.windService().sample(level, pos);

        source.sendSuccess(() -> Component.literal("实体风场 @ ")
                        .append(target.getDisplayName())
                        .append(Component.literal(String.format(" (%.1f %.1f %.1f)", pos.x, pos.y, pos.z)))
                        .withStyle(ChatFormatting.GOLD),
                false);
        source.sendSuccess(() -> line("风速", String.format("%.2f m/s  (%.1f kt)", sample.speed(), sample.speedKnots())), false);
        source.sendSuccess(() -> line("风向", String.format("%s %.0f°  去向 %.0f°",
                cardinal(sample.fromDegrees()), sample.fromDegrees(), sample.toDegrees())), false);
        source.sendSuccess(() -> line("碰撞箱", String.format("%.2f × %.2f × %.2f",
                        target.getBbWidth(), target.getBbHeight(), target.getBbWidth())), false);
        return 1;
    }

    private static int windProfile(final CommandContext<CommandSourceStack> ctx, final BlockPos pos,
                                   final int fromY, final int toY, final int step) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final WindSettings settings = WindSettings.fromConfig();
        final WindFieldService service = Crosswind.windService();

        source.sendSuccess(() -> Component.literal(String.format(
                        "风廓线 @ %s x=%d z=%d ", level.dimension().location(),
                        pos.getX(), pos.getZ()))
                .withStyle(ChatFormatting.GOLD), false);

        int prevY = Integer.MIN_VALUE;
        double prevSpeed = Double.NaN;
        double prevDir = Double.NaN;

        for (int y = fromY; y <= toY; y += step) {
            final WindSample s = service.sample(level, pos.getX() + 0.5D, y, pos.getZ() + 0.5D, settings);
            final double dSpeed = Double.isNaN(prevSpeed) ? 0.0D : s.speed() - prevSpeed;
            final double dDir = Double.isNaN(prevDir) ? 0.0D : Math.abs(WindMath.deltaDegrees(prevDir, s.fromDegrees()));
            final int yy = y;

            final boolean suspicious = !Double.isNaN(prevSpeed) && (Math.abs(dSpeed) > 0.5D || dDir > 5.0D);
            source.sendSuccess(() -> Component.literal(String.format(
                            "  y=%-5d %6.2f m/s  %5.0f°  Δv=%+.3f  Δdir=%.1f°%s",
                            yy, s.speed(), s.fromDegrees(), dSpeed, dDir, suspicious ? "   ← 不平滑?" : ""))
                    .withStyle(suspicious ? ChatFormatting.RED : ChatFormatting.GRAY), false);

            prevY = y;
            prevSpeed = s.speed();
            prevDir = s.fromDegrees();
        }
        return 1;
    }

    private static int windLine(final CommandContext<CommandSourceStack> ctx, final BlockPos from,
                                final BlockPos to, final double step) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final WindSettings settings = WindSettings.fromConfig();
        final WindFieldService service = Crosswind.windService();

        final double dx = to.getX() - from.getX();
        final double dz = to.getZ() - from.getZ();
        final double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0E-6D) {
            source.sendSuccess(() -> Component.literal("[crosswind] 起点与终点相同").withStyle(ChatFormatting.RED), false);
            return 0;
        }

        final int samples = (int) Math.floor(length / step);
        final double y = from.getY() + 0.5D;

        source.sendSuccess(() -> Component.literal(String.format(
                        "沿线风场 %d → %d  长度 %.1f 格  步长 %.2f  共 %d 个采样点",
                        from.getX(), to.getX(), length, step, samples))
                .withStyle(ChatFormatting.GOLD), false);

        double prevSpeed = Double.NaN;
        double prevDir = Double.NaN;
        int worstAt = -1;
        double worstDir = 0.0D;
        double worstDSpeed = 0.0D;

        for (int i = 0; i <= samples; i++) {
            final double t = (i * step) / length;
            final double x = from.getX() + 0.5D + dx * t;
            final double z = from.getZ() + 0.5D + dz * t;
            final WindSample s = service.sample(level, x, y, z, settings);

            if (!Double.isNaN(prevSpeed)) {
                final double dSpeed = s.speed() - prevSpeed;
                final double dDir = Math.abs(WindMath.deltaDegrees(prevDir, s.fromDegrees()));
                if (dDir > worstDir || Math.abs(dSpeed) > worstDSpeed) {
                    worstDir = Math.max(worstDir, dDir);
                    worstDSpeed = Math.max(worstDSpeed, Math.abs(dSpeed));
                    worstAt = i;
                }

                if (dDir > 15.0D) {
                    final int idx = i;
                    final double fx = x;
                    final double fz = z;
                    final double fd = dDir;
                    source.sendSuccess(() -> Component.literal(String.format(
                                    "  #%d  (%.2f, %.2f)  风向跳变 %.1f°", idx, fx, fz, fd))
                            .withStyle(ChatFormatting.RED), false);
                }
            }
            prevSpeed = s.speed();
            prevDir = s.fromDegrees();
        }

        final int wat = worstAt;
        final double wd = worstDir;
        final double wds = worstDSpeed;
        source.sendSuccess(() -> Component.literal(String.format(
                        "  最大单步变化：风向 %.2f°  风速 %.3f m/s  (在第 %d 个采样点)", wd, wds, wat))
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private static int seedGet(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final WeatherSeedState state = WeatherSeedState.get(level);
        final long gameTime = level.getGameTime();

        source.sendSuccess(() -> Component.literal("风场种子").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> line("当前", Long.toString(state.seed())), false);
        source.sendSuccess(() -> line("上一个", Long.toString(state.previousSeed())), false);
        source.sendSuccess(() -> line("过渡进度", String.format("%.0f%%", state.transitionRatio(gameTime) * 100.0D)), false);
        source.sendSuccess(() -> line("维度", level.dimension().location().toString()), false);
        return 1;
    }

    private static int seedSet(final CommandContext<CommandSourceStack> ctx, final long requested) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final WindSettings settings = WindSettings.fromConfig();
        final WeatherSeedState state = WeatherSeedState.get(level);

        final long newSeed = requested == -1L
                ? new java.util.Random().nextLong()
                : requested;

        final long gameTime = level.getGameTime();

        final int duration = (int) Math.min(Integer.MAX_VALUE, settings.transitionTicks() * 2L);

        final long oldSeed = state.seed();
        state.setSeed(newSeed, gameTime, duration);

        source.sendSuccess(() -> Component.literal("[crosswind] 风场种子已改")
                .withStyle(ChatFormatting.GREEN), false);
        source.sendSuccess(() -> line("旧", Long.toString(oldSeed)), false);
        source.sendSuccess(() -> line("新", Long.toString(newSeed)), false);
        source.sendSuccess(() -> line("过渡", String.format("%d tick（%.1f 秒）内平滑插值",
                duration, duration / 20.0D)), false);
        return 1;
    }

    private static int terrainInfo(final CommandContext<CommandSourceStack> ctx, final BlockPos pos,
                                   final boolean rebuild) {
        final CommandSourceStack source = ctx.getSource();
        final ServerLevel level = source.getLevel();
        final Vec3 where = resolvePos(source, pos);

        final int cx = ((int) Math.floor(where.x)) >> 4;
        final int cz = ((int) Math.floor(where.z)) >> 4;

        final TerrainSignature sig = rebuild
                ? Crosswind.windService().terrainCache().rebuild(level, cx, cz)
                : Crosswind.windService().terrainCache().get(level, cx, cz);

        source.sendSuccess(() -> Component.literal(String.format("── 地形摘要 区块 (%d, %d)", cx, cz))
                .withStyle(ChatFormatting.GOLD), false);

        if (sig.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                            "  区块未加载，跳过地形修正")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        source.sendSuccess(() -> line("高度范围", String.format("y %d .. %d",
                sig.minY(), sig.maxY(), sig.relief())), false);
        source.sendSuccess(() -> line("平均坡度", String.format("%.3f", sig.meanSlope())), false);
        source.sendSuccess(() -> line("粗糙度", String.format("%.3f", sig.roughness())), false);
        source.sendSuccess(() -> line("山脊走向", String.format("%.1f°",
                sig.ridgeAxisDeg())), false);
        source.sendSuccess(() -> line("版本", Long.toString(sig.version())), false);
        source.sendSuccess(() -> Component.literal(String.format(
                        "  缓存共 %d 条%s", Crosswind.windService().terrainCache().size(),
                        rebuild ? "（本次已强制重建）" : ""))
                .withStyle(ChatFormatting.DARK_GRAY), false);
        return 1;
    }

    private static int info(final CommandContext<CommandSourceStack> ctx) {
        final CommandSourceStack source = ctx.getSource();
        final WindSettings s = WindSettings.fromConfig();

        source.sendSuccess(() -> Component.literal("Sable: Crosswind 风场状态")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> line("总开关", s.enabled() ? "开" : "关"), false);
        source.sendSuccess(() -> line("基础风速", String.format("%.1f .. %.1f m/s", s.baseSpeed(), s.maxSpeed())), false);
        source.sendSuccess(() -> line("采样格", String.format("%d 方块", s.gridSize())), false);
        source.sendSuccess(() -> line("风况周期", String.format("%d tick（%.1f 秒）", s.transitionTicks(),
                s.transitionTicks() / 20.0D)), false);
        source.sendSuccess(() -> line("地形修正", s.terrainActive()
                ? String.format("开（迎风 ×%.2f / 背风 -%.2f / 导流 %.0f°）",
                s.ridgeAccel(), s.leeDecel(), s.valleyDeflectionDeg())
                : "关"), false);
        source.sendSuccess(() -> line("气压缩放", s.pressureAffectsForce()
                ? String.format("开（基准 %.4f，指数 %.2f，倍数范围 ×%.2f ~ ×%.2f）",
                s.pressureReference(), s.pressureExponent(),
                s.pressureMinFactor(), s.pressureMaxFactor())
                : "关（风力与气压无关）"), false);
        source.sendSuccess(() -> line("阵风", s.gustActive() ? String.format("开（上限 ×%.2f）", s.gustStrength()) : "关"), false);
        return 1;
    }

    private static Vec3 resolvePos(final CommandSourceStack source, final BlockPos pos) {
        if (pos != null) {
            return new Vec3(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        }
        return source.getPosition();
    }

    private static Component dim(final ServerLevel level) {
        return Component.literal(level.dimension().location().toString());
    }

    private static Component line(final String key, final String value) {
        return Component.literal("  " + key)
                .withStyle(ChatFormatting.GRAY)
                .append(Component.literal("  " + value).withStyle(ChatFormatting.WHITE));
    }

    private static double horizontal(final double x, final double z) {
        return Math.sqrt(x * x + z * z);
    }

    private static String cardinal(final double degrees) {
        final String[] names = {
                "北", "北偏东", "东北", "东偏北",
                "东", "东偏南", "东南", "南偏东",
                "南", "南偏西", "西南", "西偏南",
                "西", "西偏北", "西北", "北偏西"
        };
        final int idx = (int) Math.round(WindMath.wrapDegrees(degrees) / 22.5D) % 16;
        return names[idx];
    }
}
