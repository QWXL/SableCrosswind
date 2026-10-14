package cn.chumenta.crosswind.instrument;

public final class CupWindow {
    private final double[] samples;

    private int cursor;

    private int filled;

    private double sum;

    public CupWindow(final int windowTicks) {
        this.samples = new double[Math.max(1, windowTicks)];
    }

    public int windowTicks() {
        return this.samples.length;
    }

    public boolean hasSamples() {
        return this.filled > 0;
    }

    public double push(final double speedMps) {
        if (!Double.isFinite(speedMps)) {
            return this.average();
        }

        final double clamped = Math.max(0.0D, speedMps);

        if (this.filled == this.samples.length) {
            this.sum -= this.samples[this.cursor];
        } else {
            this.filled++;
        }
        this.samples[this.cursor] = clamped;
        this.sum += clamped;
        this.cursor = (this.cursor + 1) % this.samples.length;

        return this.average();
    }

    public double average() {
        return this.filled == 0 ? 0.0D : this.sum / this.filled;
    }

    public void clear() {
        java.util.Arrays.fill(this.samples, 0.0D);
        this.cursor = 0;
        this.filled = 0;
        this.sum = 0.0D;
    }
}
