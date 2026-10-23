package cn.chumenta.crosswind.client;

import cn.chumenta.crosswind.rat.RatBlock;
import cn.chumenta.crosswind.rat.RatBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class RatRenderer implements BlockEntityRenderer<RatBlockEntity> {
    private static final float FAN_PIVOT_X = 8.0F / 16.0F;
    private static final float FAN_PIVOT_Y = 4.0F / 16.0F;
    private static final float FAN_PIVOT_Z = 6.475F / 16.0F;

    private static final float SHAFT_PIVOT_X = 8.0F / 16.0F;
    private static final float SHAFT_PIVOT_Y = 8.0F / 16.0F;
    private static final float SHAFT_PIVOT_Z = 8.0F / 16.0F;

    public RatRenderer(final BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(final RatBlockEntity be, final float partialTick, final PoseStack poseStack,
                       final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
        final BlockState state = be.getBlockState();
        final Direction facing = state.getValue(RatBlock.HORIZONTAL_FACING);
        final float facingDegrees = facingY(facing);
        final long ticks = AnimationTickHolder.getTicks(be.getLevel());
        final float fanDegrees = be.fanAngle().spinBy(
                cn.chumenta.crosswind.rat.RatAirspeed.fanRpmFor(be.airspeedMps()) * 6.0F,
                partialTick, ticks);
        final float shaftDegrees = be.shaftAngle().spinBy(be.getSpeed() * 6.0F, partialTick, ticks);

        draw(RotorModels.RAT_FAN, state, facingDegrees, fanDegrees, Direction.Axis.Z,
                FAN_PIVOT_X, FAN_PIVOT_Y, FAN_PIVOT_Z, poseStack, buffers, packedLight);
        draw(RotorModels.RAT_SHAFT, state, facingDegrees, shaftDegrees, Direction.Axis.Y,
                SHAFT_PIVOT_X, SHAFT_PIVOT_Y, SHAFT_PIVOT_Z, poseStack, buffers, packedLight);
    }

    private static void draw(final PartialModel partial, final BlockState state,
                             final float facingDegrees, final float rotationDegrees,
                             final Direction.Axis axis,
                             final float pivotX, final float pivotY, final float pivotZ,
                             final PoseStack poseStack, final MultiBufferSource buffers,
                             final int packedLight) {
        if (partial.get() == null) {
            return;
        }

        final SuperByteBuffer buffer = CachedBuffers.partial(partial, state);
        buffer.reset();
        buffer.light(packedLight).color(0xFFFFFF);

        if (facingDegrees != 0.0F) {
            buffer.translate(0.5F, 0.5F, 0.5F)
                    .rotateYDegrees(-facingDegrees)
                    .translateBack(0.5F, 0.5F, 0.5F);
        }

        buffer.translate(pivotX, pivotY, pivotZ)
                .rotateDegrees(rotationDegrees, axis)
                .translateBack(pivotX, pivotY, pivotZ);

        buffer.renderInto(poseStack, buffers.getBuffer(RenderType.cutout()));
    }

    private static float facingY(final Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
