package cn.chumenta.crosswind.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class WindStreakParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private boolean spriteResolved;

    private static int spriteFailures;

    private final float baseAlpha;
    private final float baseSize;

    private WindStreakParticle(final ClientLevel level,
                               final double x, final double y, final double z,
                               final SpriteSet sprites, final float alpha, final float size,
                               final int lifetime) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D);
        this.sprites = sprites;

        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.baseAlpha = Mth.clamp(alpha, 0.0F, 1.0F);
        this.baseSize = size;
        this.quadSize = size;
        this.gravity = 0.0F;
        this.friction = 1.0F;
        this.hasPhysics = false;
        this.lifetime = lifetime;

        this.setAlpha(this.baseAlpha);
    }

    private void ensureSprite() {
        if (this.spriteResolved) {
            return;
        }
        try {
            final var sprite = this.sprites.get(this.random);
            if (sprite != null) {
                this.setSprite(sprite);
            }
            this.spriteResolved = true;
        } catch (final Throwable t) {
            if (spriteFailures++ < 5) {
                cn.chumenta.crosswind.Crosswind.LOGGER.warn(

                        "[crosswind] 风纹粒子取贴图失败", t);
            }
            this.remove();
        }
    }

    @Override
    public void tick() {
        this.ensureSprite();
        if (this.removed) {
            return;
        }
        super.tick();
        final float t = this.lifetime <= 0 ? 1.0F : (float) this.age / (float) this.lifetime;

        final float fade = t < 0.25F ? t / 0.25F
                : (t > 0.65F ? (1.0F - t) / 0.35F : 1.0F);
        this.setAlpha(this.baseAlpha * Mth.clamp(fade, 0.0F, 1.0F));

        this.quadSize = this.baseSize * (1.0F - 0.10F * t);
    }

    @Override
    public void render(final VertexConsumer buffer, final Camera camera, final float partialTick) {
        if (this.sprite == null) {
            return;
        }
        super.render(buffer, camera, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class Provider
            implements net.minecraft.client.particle.ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public net.minecraft.client.particle.Particle createParticle(
                final SimpleParticleType type, final ClientLevel level,
                final double x, final double y, final double z,
                final double vx, final double vy, final double vz) {
            final float alpha = 0.40F;

            final float size = cn.chumenta.crosswind.CrosswindClientConfig
                    .WIND_PARTICLE_SIZE.get().floatValue();

            final double seconds = cn.chumenta.crosswind.CrosswindClientConfig
                    .WIND_PARTICLE_LIFETIME_SECONDS.get();
            final int lifetime = Math.max(4, (int) Math.round(seconds * 20.0D));
            return new WindStreakParticle(level, x, y, z, this.sprites, alpha, size, lifetime);
        }
    }
}
