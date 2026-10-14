package cn.chumenta.crosswind.wind;

import org.joml.Vector3d;

public record WindStages(
        Vector3d baseVelocity,
        double terrainFactor,
        boolean terrainApplied,

        double terrainReferenceHeight,
        double gustSpeed,
        Vector3d finalVelocity,
        double temperatureC,
        double temperatureBiomeBaseC,
        double temperatureDiurnalC,
        double temperatureFluctuationC,
        double precipitation,
        double precipitationBase,
        double pressure,
        double pressureForceScale,
        String biomeId,
        double rawBiomeTemperature,
        boolean isOcean,

        boolean climateAvailable
) {
    public WindSample toSample() {

        return new WindSample(
                new net.minecraft.world.phys.Vec3(this.finalVelocity.x, this.finalVelocity.y,
                        this.finalVelocity.z),
                Math.max(0.0D, this.gustSpeed),
                this.temperatureC,
                this.precipitation,
                this.pressure,
                this.terrainApplied,

                this.climateAvailable);
    }
}
