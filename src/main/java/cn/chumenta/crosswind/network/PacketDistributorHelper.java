package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.instrument.WindReadout;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PacketDistributorHelper {
    private PacketDistributorHelper() {
    }

    public static void sendInstrumentReadout(final ServerLevel level, final BlockPos pos,
                                             final WindReadout value) {
        PacketDistributor.sendToPlayersInDimension(level, WindInstrumentPayload.of(pos, value));
    }
}
