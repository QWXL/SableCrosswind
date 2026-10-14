package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.Crosswind;
import cn.chumenta.crosswind.instrument.WindReadout;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WindInstrumentPayload(
        BlockPos pos,
        float toDeg,
        float fromDeg,
        float speed,
        float gust,

        float cupSpeed,
        float temp,
        float precip,

        boolean climateAvailable
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<WindInstrumentPayload> TYPE =
            new CustomPacketPayload.Type<>(Crosswind.id("instrument_readout"));

    public static WindInstrumentPayload of(final BlockPos pos, final WindReadout r) {
        return new WindInstrumentPayload(pos, r.toDegrees(), r.fromDegrees(),
                r.speedMps(), r.gustMps(), r.cupSpeedMps(),
                r.temperatureC(), r.precipitation(), r.climateAvailable());
    }

    public WindReadout readout() {
        return new WindReadout(this.toDeg, this.fromDeg, this.speed, this.gust,
                this.cupSpeed, this.temp, this.precip, this.climateAvailable);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, WindInstrumentPayload> CODEC =
            CustomPacketPayload.codec(WindInstrumentPayload::write, WindInstrumentPayload::read);

    private void write(final RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeFloat(this.toDeg);
        buf.writeFloat(this.fromDeg);
        buf.writeFloat(this.speed);
        buf.writeFloat(this.gust);
        buf.writeFloat(this.cupSpeed);
        buf.writeFloat(this.temp);
        buf.writeFloat(this.precip);
        buf.writeBoolean(this.climateAvailable);
    }

    private static WindInstrumentPayload read(final RegistryFriendlyByteBuf buf) {
        return new WindInstrumentPayload(buf.readBlockPos(),
                buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(),
                buf.readFloat(), buf.readFloat(),
                buf.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
