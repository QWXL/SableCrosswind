package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.instrument.AbstractWindInstrumentBlockEntity;
import cn.chumenta.crosswind.instrument.WindInstrumentAngles;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractRotorRenderer<T extends AbstractWindInstrumentBlockEntity>
        implements BlockEntityRenderer<T> {
    protected AbstractRotorRenderer() {
    }

    protected abstract PartialModel rotorPartial();

    protected abstract float currentAngle(T be, float partialTick);

    protected float warnIntensity(final T be, final float partialTick) {
        return 0.0F;
    }

    protected float[] warnTint(final float intensity) {
        return new float[]{1.0F, 1.0F, 1.0F};
    }

    protected float rotationSign() {
        return -1.0F;
    }

    @Override
    public void render(final T be, final float partialTick, final PoseStack poseStack,
                       final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
        final float degrees = this.rotationSign() * this.currentAngle(be, partialTick);

        final float[] tint = this.warnTint(this.warnIntensity(be, partialTick));

        drawRotor(this.rotorPartial(), be.getBlockState(), degrees, tint,
                poseStack, buffers, packedLight);
    }

    static long renderTicks(final AbstractWindInstrumentBlockEntity be) {
        final var level = be.getLevel();
        return level == null ? 0L : AnimationTickHolder.getTicks(level);
    }

    protected static void drawRotor(final PartialModel partial,
                                    final net.minecraft.world.level.block.state.BlockState state,
                                    final float degrees, final float[] tint,
                                    final PoseStack poseStack, final MultiBufferSource buffers,
                                    final int packedLight) {
        if (partial.get() == null) {
            return;
        }

        final float radians = (float) Math.toRadians(degrees);

        final int argb = 0xFF000000
                | ((int) (tint[0] * 255.0F) << 16)
                | ((int) (tint[1] * 255.0F) << 8)
                | (int) (tint[2] * 255.0F);

        final SuperByteBuffer buffer = CachedBuffers.partial(partial, state);
        buffer.reset();

        buffer.light(packedLight)
                .color(argb)

                .rotateCentered(radians, Direction.Axis.Y);
        buffer.renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
    }
}
