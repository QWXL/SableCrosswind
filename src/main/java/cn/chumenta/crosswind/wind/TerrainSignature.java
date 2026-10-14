package cn.chumenta.crosswind.wind;

public record TerrainSignature(
        int minY,
        int maxY,
        float meanSlope,
        float ridgeAxisDeg,
        float roughness,
        long version
) {
    public static final int EMPTY_HEIGHT = Integer.MIN_VALUE;

    public boolean isEmpty() {
        return this.minY == EMPTY_HEIGHT;
    }

    public int relief() {
        return this.isEmpty() ? 0 : (this.maxY - this.minY);
    }

    public double meanHeight() {
        return this.isEmpty() ? 0.0D : (this.minY + this.maxY) * 0.5D;
    }

    public static TerrainSignature empty(final long version) {
        return new TerrainSignature(EMPTY_HEIGHT, EMPTY_HEIGHT, 0.0F, 0.0F, 0.0F, version);
    }
}
