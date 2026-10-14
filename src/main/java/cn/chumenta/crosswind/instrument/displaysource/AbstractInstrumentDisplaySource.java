package cn.chumenta.crosswind.instrument.displaysource;

import cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity;
import cn.chumenta.crosswind.instrument.WindReadout;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;

public abstract class AbstractInstrumentDisplaySource extends SingleLineDisplaySource {
    protected AbstractInstrumentDisplaySource() {
    }

    protected static WindReadout readoutOf(final DisplayLinkContext context) {
        return context.getSourceBlockEntity() instanceof AbstractWindInstrumentBlockEntity instrument
                ? instrument.readout()
                : null;
    }

    @Override
    protected boolean allowsLabeling(final DisplayLinkContext context) {
        return true;
    }

    @Override
    protected String getFlapDisplayLayoutName(final DisplayLinkContext context) {
        return this.getTranslationKey();
    }

    public void registerFor(final net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK_ENTITY.add(type, this);
    }
}
