package cn.chumenta.crosswind.wind;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

public final class WeatherSeedState extends SavedData {
    public static final String ID = "crosswind_wind_state";

    private static final long FALLBACK_CLIENT_SEED = 0L;

    private static final String KEY_SEED = "Seed";
    private static final String KEY_PREVIOUS_SEED = "PreviousSeed";
    private static final String KEY_TRANSITION_START = "TransitionStartTick";
    private static final String KEY_TRANSITION_DURATION = "TransitionDurationTicks";

    private long seed;
    private long previousSeed;
    private long transitionStartTick;
    private int transitionDurationTicks;

    private boolean initialised;

    public WeatherSeedState() {
        this(0L);
    }

    public WeatherSeedState(final long seed) {
        this.seed = seed;
        this.previousSeed = seed;
        this.transitionStartTick = 0L;
        this.transitionDurationTicks = 0;
    }

    public static WeatherSeedState get(final Level level) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            return new WeatherSeedState(FALLBACK_CLIENT_SEED);
        }
        final WeatherSeedState state = serverLevel.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WeatherSeedState::new, WeatherSeedState::load),
                ID);
        if (!state.initialised) {
            state.seed = serverLevel.getSeed();
            state.previousSeed = state.seed;
            state.initialised = true;
            state.setDirty();
        }
        return state;
    }

    public long seed() {
        return this.seed;
    }

    public long previousSeed() {
        return this.previousSeed;
    }

    public double transitionRatio(final long gameTime) {
        if (this.transitionDurationTicks <= 0) {
            return 1.0D;
        }
        final long elapsed = gameTime - this.transitionStartTick;
        if (elapsed <= 0L) {
            return 0.0D;
        }
        if (elapsed >= this.transitionDurationTicks) {
            return 1.0D;
        }
        return elapsed / (double) this.transitionDurationTicks;
    }

    public boolean isTransitioning(final long gameTime) {
        return this.transitionRatio(gameTime) < 1.0D;
    }

    public void setSeed(final long newSeed, final long gameTime, final int durationTicks) {
        if (durationTicks <= 0) {
            this.seed = newSeed;
            this.previousSeed = newSeed;
            this.transitionDurationTicks = 0;
        } else {
            this.previousSeed = this.seed;
            this.seed = newSeed;
            this.transitionStartTick = gameTime;
            this.transitionDurationTicks = durationTicks;
        }
        this.setDirty();
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putLong(KEY_SEED, this.seed);
        tag.putLong(KEY_PREVIOUS_SEED, this.previousSeed);
        tag.putLong(KEY_TRANSITION_START, this.transitionStartTick);
        tag.putInt(KEY_TRANSITION_DURATION, this.transitionDurationTicks);
        return tag;
    }

    private static WeatherSeedState load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final WeatherSeedState state = new WeatherSeedState();

        state.seed = tag.getLong(KEY_SEED);
        state.previousSeed = tag.contains(KEY_PREVIOUS_SEED) ? tag.getLong(KEY_PREVIOUS_SEED) : state.seed;
        state.transitionStartTick = tag.getLong(KEY_TRANSITION_START);
        state.transitionDurationTicks = tag.getInt(KEY_TRANSITION_DURATION);
        state.initialised = tag.contains(KEY_SEED);
        return state;
    }
}
