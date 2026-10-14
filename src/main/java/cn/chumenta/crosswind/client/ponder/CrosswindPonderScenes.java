package cn.chumenta.crosswind.client.ponder;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.WindInstruments;
import cn.chumenta.crosswind.instrument.WindReadout;
import cn.chumenta.crosswind.registry.CrosswindRegistry;
import cn.chumenta.crosswind.wind.WindMath;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class CrosswindPonderScenes {
    public static final String SCENE_ID = "instruments";

    private static final BlockPos POS_VANE = new BlockPos(1, 2, 3);
    private static final BlockPos POS_ANEMOMETER = new BlockPos(3, 2, 3);
    private static final BlockPos POS_STATION = new BlockPos(5, 2, 3);

    private static final BlockPos POS_ROD_VANE = new BlockPos(1, 1, 3);
    private static final BlockPos POS_ROD_ANEMOMETER = new BlockPos(3, 1, 3);
    private static final BlockPos POS_ROD_STATION = new BlockPos(5, 1, 3);

    private static final int BASE_PLATE_SIZE = 7;

    private static final float SCENE_WIND_FROM_DEGREES = 225.0F;

    private static final int STREAK_DOTS = 10;

    private static final double STREAK_DOT_SPACING = 0.20D;

    private static final int STREAK_WAVE_GAP_TICKS = 14;

    private static final int STREAK_WAVES_PER_TEXT = 3;

    private static final double[][] INTRO_STREAKS = {
            {3.5D, 1.6D, 3.5D},
    };

    private static final double[][] BACKGROUND_STREAKS = {
            {9.5D, 2.5D, 5.0D},
            {3.0D, 2.5D, 9.5D},
            {0.5D, 1.2D, 8.0D},
    };

    private CrosswindPonderScenes() {
    }

    public static void register(final PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(
                        Crosswind.id("wind_vane"),
                        Crosswind.id("anemometer"),
                        Crosswind.id("weather_station"),
                        Crosswind.id("instrument_rod"))

                .addStoryBoard(Crosswind.id(SCENE_ID), CrosswindPonderScenes::instruments,
                        CrosswindPonderTags.INSTRUMENTS);
    }

    public static void instruments(final SceneBuilder scene, final SceneBuildingUtil util) {
        scene.title(SCENE_ID, "The Air That Brushes Past");

        scene.configureBasePlate(0, 0, BASE_PLATE_SIZE);
        scene.showBasePlate();
        scene.idle(10);

        scene.overlay().showText(100)
                .independent(20)
                .text("Wind is everywhere in the world.");
        emitStreakWaves(scene, INTRO_STREAKS, STREAK_WAVES_PER_TEXT);
        scene.idle(30);

        scene.rotateCameraY(-20.0F);
        scene.idle(25);

        scene.overlay().showText(100)
                .independent(20)
                .text("Wind is represented in terms of chunks; in addition to the random seed, "
                        + "it is influenced by multiple factors such as air pressure and terrain.");
        emitStreakWaves(scene, INTRO_STREAKS, STREAK_WAVES_PER_TEXT);
        scene.idle(30);

        scene.addKeyframe();

        final Selection rods = util.select().position(POS_ROD_VANE)
                .add(util.select().position(POS_ROD_ANEMOMETER))
                .add(util.select().position(POS_ROD_STATION));
        final Selection instruments = util.select().position(POS_VANE)
                .add(util.select().position(POS_ANEMOMETER))
                .add(util.select().position(POS_STATION));

        scene.world().showSection(rods, Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(instruments, Direction.DOWN);
        scene.idle(8);

        feedAllInstruments(scene);
        scene.idle(12);

        scene.overlay().showText(90)
                .independent(20)
                .text("To make sense of the wind, we can use meteorological instruments.");

        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(81);

        scene.addKeyframe();

        scene.overlay().showText(90)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(POS_VANE))
                .text("The wind vane reports direction: its arrow points into the wind - "
                        + "the direction it comes from.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(81);

        scene.addKeyframe();

        scene.overlay().showText(95)
                .colored(PonderPalette.GREEN)
                .pointAt(util.vector().topOf(POS_ANEMOMETER))
                .text("The anemometer reports speed. Its cups turn with the wind they catch.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(86);

        scene.overlay().showText(95)
                .independent(20)
                .text("Note that an anemometer made of andesite alloy cannot withstand "
                        + "strong wind.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(86);

        feedAnemometer(scene, 20.0F, 3.5F);
        scene.overlay().showText(85)
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(POS_ANEMOMETER))
                .text("It gradually turns red to show the load it is under.");

        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(61);

        feedAnemometer(scene, 32.0F, 4.0F);
        scene.idle(25);
        scene.world().destroyBlock(POS_ANEMOMETER);
        scene.world().createItemEntity(util.vector().centerOf(POS_ANEMOMETER),
                new Vec3(0.0D, 0.2D, 0.0D),
                new ItemStack(CrosswindRegistry.ANEMOMETER_ITEM.get()));
        scene.overlay().showText(95)
                .colored(PonderPalette.RED)
                .independent(20)
                .text("When the wind gets too strong - Peng - it is wrecked by the gale.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(86);

        scene.overlay().showText(85)
                .independent(20)
                .text("It is not an instrument suited to windy places such as high mountains.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(76);

        scene.addKeyframe();

        feedStation(scene, 40.0F, 6.0F);
        scene.rotateCameraY(25.0F);
        scene.overlay().showText(95)
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(POS_STATION))
                .text("The weather station reports five things: direction, per-second wind, "
                        + "exact speed, temperature and precipitation.");

        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(71);

        scene.overlay().showText(85)
                .independent(20)
                .text("It is rated for far stronger wind, so it can be used safely in windy "
                        + "places without being destroyed.");

        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(61);

        scene.overlay().showText(85)
                .independent(20)
                .text("It has no drawbacks - apart from the price.");
        emitStreakWaves(scene, BACKGROUND_STREAKS, 1);
        scene.idle(76);

        scene.markAsFinished();
    }

    private static void emitStreakWaves(final SceneBuilder scene, final double[][] centres,
                                        final int waves) {
        final ParticleEmitter streaks = scene.effects().simpleParticleEmitter(
                CrosswindRegistry.WIND_STREAK.get(), Vec3.ZERO);
        final Vec3 dir = flowDirection();

        final double half = STREAK_DOT_SPACING * (STREAK_DOTS - 1) / 2.0D;

        for (int wave = 0; wave < waves; wave++) {
            for (int i = 0; i < STREAK_DOTS; i++) {
                final Vec3 step = dir.scale(i * STREAK_DOT_SPACING - half);
                for (final double[] centre : centres) {
                    scene.effects().emitParticles(
                            new Vec3(centre[0], centre[1], centre[2]).add(step),
                            streaks, 1.0F, 1);
                }

                scene.idle(1);
            }
            scene.idle(STREAK_WAVE_GAP_TICKS);
        }
    }

    private static Vec3 flowDirection() {
        final double toDegrees = WindMath.wrapDegrees(SCENE_WIND_FROM_DEGREES + 180.0D);
        final double radians = Math.toRadians(toDegrees);
        return new Vec3(Math.sin(radians), 0.0D, -Math.cos(radians));
    }

    private static WindReadout readout(final float fromDegrees, final float speedMps,
                                       final float gustMps, final float cupSpeedMps,
                                       final float temperatureC, final float precipitation) {
        return new WindReadout(
                (float) WindMath.wrapDegrees(fromDegrees + 180.0D),
                (float) WindMath.wrapDegrees(fromDegrees),
                speedMps, gustMps, cupSpeedMps,
                temperatureC, precipitation,

                true);
    }

    private static void feedAllInstruments(final SceneBuilder scene) {
        feedVane(scene, SCENE_WIND_FROM_DEGREES);

        feedAnemometer(scene, 8.0F, 1.5F);

        feedStation(scene, 0.0F, 0.0F);
    }

    private static void feedVane(final SceneBuilder scene, final float fromDegrees) {
        final WindReadout value = readout(fromDegrees, 15.0F, 3.0F, 15.0F, 16.5F, 0.0F);
        scene.world().modifyBlockEntity(POS_VANE, WindInstruments.WindVaneBlockEntity.class,
                be -> be.setReadoutFromServer(value));
    }

    private static void feedAnemometer(final SceneBuilder scene, final float cupSpeedMps,
                                       final float gustMps) {
        final WindReadout value =
                readout(SCENE_WIND_FROM_DEGREES, cupSpeedMps, gustMps, cupSpeedMps, 16.5F, 0.0F);
        scene.world().modifyBlockEntity(POS_ANEMOMETER, WindInstruments.AnemometerBlockEntity.class,
                be -> be.setReadoutFromServer(value));
    }

    private static void feedStation(final SceneBuilder scene, final float cupSpeedMps,
                                    final float gustMps) {
        final WindReadout value =
                readout(SCENE_WIND_FROM_DEGREES, cupSpeedMps, gustMps, cupSpeedMps, 12.0F, 0.35F);
        scene.world().modifyBlockEntity(POS_STATION, WindInstruments.WeatherStationBlockEntity.class,
                be -> be.setReadoutFromServer(value));
    }
}
