package cn.chumenta.crosswind.network;

import cn.chumenta.crosswind.Crosswind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class WindMotionClientHandler {
    private WindMotionClientHandler() {
    }

    private static final Map<Integer, float[]> LATEST = new HashMap<>();

    private static boolean registered;

    private static long lastAppliedGameTime = Long.MIN_VALUE;

    public static synchronized void register() {
        if (registered) {
            return;
        }

        NeoForge.EVENT_BUS.addListener(WindMotionClientHandler::onClientTick);
        registered = true;
        Crosswind.LOGGER.info("客户端风力位移插帧已启用");
    }

    public static void handle(final WindMotionPayload payload, final IPayloadContext context) {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (final WindMotionPayload.Entry e : payload.updates()) {
            if (Float.isNaN(e.dvx())) {
                LATEST.remove(e.entityId());
                continue;
            }
            LATEST.put(e.entityId(), new float[] { e.dvx(), e.dvy(), e.dvz() });
        }

    }

    private static void onClientTick(final ClientTickEvent.Post event) {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null) {
            LATEST.clear();
            lastAppliedGameTime = Long.MIN_VALUE;
            return;
        }

        final long now = level.getGameTime();
        if (now == lastAppliedGameTime) {
            return;
        }
        lastAppliedGameTime = now;


        for (final Map.Entry<Integer, float[]> entry : LATEST.entrySet()) {
            final Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            final float[] d = entry.getValue();
            final Vec3 vel = entity.getDeltaMovement();

            entity.setDeltaMovement(vel.x + d[0], vel.y + d[1], vel.z + d[2]);
        }


    }

    public static void clear() {
        LATEST.clear();

        lastAppliedGameTime = Long.MIN_VALUE;
    }
}
