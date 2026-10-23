package cn.chumenta.crosswind.wind;

import java.util.Map;

public record WindEnvironment(double x, double y, double z, ClimateSample climate,
                              Map<Long, TerrainSignature> terrainSignatures) {
    public WindEnvironment {
        terrainSignatures = Map.copyOf(terrainSignatures);
    }

    public record ClimateSample(String biomeId, double rawBiomeTemperature, double convertedC,
                                boolean isOcean) {
    }
}
