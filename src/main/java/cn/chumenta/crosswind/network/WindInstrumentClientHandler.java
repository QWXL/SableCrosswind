package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WindInstrumentClientHandler {
    private WindInstrumentClientHandler() {
    }

    public static void handle(final WindInstrumentPayload payload) {
        final ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        final var be = level.getBlockEntity(payload.pos());
        if (be instanceof final AbstractWindInstrumentBlockEntity instrument) {
            instrument.setReadoutFromServer(payload.readout());
        }
    }
}
