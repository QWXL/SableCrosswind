package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.Crosswind;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;

public record WindMotionPayload(long gameTime, List<Entry> updates) implements CustomPacketPayload {
    public record Entry(int entityId, float dvx, float dvy, float dvz) {
    }

    public static final CustomPacketPayload.Type<WindMotionPayload> TYPE =
            new CustomPacketPayload.Type<>(Crosswind.id("wind_motion"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WindMotionPayload> CODEC =
            CustomPacketPayload.codec(WindMotionPayload::write, WindMotionPayload::read);

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeLong(this.gameTime);
        buf.writeVarInt(this.updates.size());
        for (final Entry e : this.updates) {
            buf.writeVarInt(e.entityId());
            buf.writeFloat(e.dvx());
            buf.writeFloat(e.dvy());
            buf.writeFloat(e.dvz());
        }
    }

    private static WindMotionPayload read(final RegistryFriendlyByteBuf buf) {
        final long gameTime = buf.readLong();
        final int n = buf.readVarInt();

        if (n < 0 || n > 8192) {
            throw new io.netty.handler.codec.DecoderException("Crosswind: 非法的实体增量条数 " + n);
        }
        final List<Entry> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(new Entry(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat()));
        }
        return new WindMotionPayload(gameTime, list);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
