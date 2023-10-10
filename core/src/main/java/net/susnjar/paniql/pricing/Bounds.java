package net.susnjar.paniql.pricing;

import java.util.Objects;

/**
 * Represents the type of statistical bounds consisting of four values:
 * minimum, average, 95% percentile and maximum and handles basic arithmetic
 * with those.
 */
public class Bounds {
    public static final Bounds ALWAYS_0 = new Bounds(0.0d, 0.0d, 0.0d, 0.0d);
    public static final Bounds LOW_AVERAGE = new Bounds(0.0d, 0.1, 0.5d, 1.0d);
    public static final Bounds LINEAR_0_TO_1 = new Bounds(0.0d, 0.5, 0.95d, 1.0d);
    public static final Bounds HIGH_AVERAGE = new Bounds(0.0d, 0.9, 0.99d, 1.0d);
    public static final Bounds ALWAYS_1 = new Bounds(1.0d, 1.0d, 1.0d, 1.0d);

    private final double minimum;
    private final double average;
    private final double percentile95;
    private final double maximum;

    public Bounds(
        final double minimum,
        final double average,
        final double percentile95,
        final double maximum
    ) {
        if (average < minimum) throw new IllegalArgumentException("Average must not be less than the minimum.");
        if (percentile95 < average) throw new IllegalArgumentException("95% percentile must not be less than the average.");
        if (maximum < percentile95) throw new IllegalArgumentException("Maximum must not be less than the 95% percentile.");

        this.minimum = minimum;
        this.average = average;
        this.percentile95 = percentile95;
        this.maximum = maximum;
    }

    public double getMinimum() {
        return minimum;
    }

    public double getAverage() {
        return average;
    }

    public double getPercentile95() {
        return percentile95;
    }

    public double getMaximum() {
        return maximum;
    }

    public boolean isAlwaysZero() {
        return this.equals(ALWAYS_0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bounds that = (Bounds) o;
        return Double.compare(that.minimum, minimum) == 0
                && Double.compare(that.average, average) == 0
                && Double.compare(that.percentile95, percentile95) == 0
                && Double.compare(that.maximum, maximum) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimum, average, percentile95, maximum);
    }

    /**
     * Returns a type with values that are sums of the
     * corresponding values of this and the specified other tuple.
     */
    public Bounds plus(final Bounds other) {
        return new Bounds(
            this.minimum + other.getMinimum(),
            this.average + other.getAverage(),
            this.percentile95 + other.getPercentile95(),
            this.maximum + other.getMaximum()
        );
    }

    /**
     * Returns a type with values that are products of the
     * corresponding values of this and the specified common factor.
     * 
     * @see #times(double, double, double, double)
     * @see #times(Bounds)
     */
    public Bounds times(final double commonFactor) {
        return times(commonFactor, commonFactor, commonFactor, commonFactor);
    }

    /**
     * Returns a type with values that are products of the
     * corresponding values of this and the specified factor.
     * 
     * @see #times(double) 
     * @see #times(Bounds)
     */
    public Bounds times(
        final double minimumFactor,
        final double averageFactor,
        final double percentile95Factor,
        final double maximumFactor
    ) {
        return new Bounds(
            this.minimum * minimumFactor,
            this.average * averageFactor,
            this.percentile95 * percentile95Factor,
            this.maximum * maximumFactor
        );
    }

    /**
     * Returns a type with values that are products of the
     * corresponding values of this and the specified other tuple.
     */
    public Bounds times(Bounds other) {
        return times(
            other.getMinimum(),
            other.getAverage(),
            other.getPercentile95(),
            other.getMaximum()
        );
    }

    public Bounds floorDiv(final double maxQuantityPerFlatFee) {
        return new Bounds(
                Math.floor(this.minimum / maxQuantityPerFlatFee),
                Math.floor(this.average / maxQuantityPerFlatFee),
                Math.floor(this.percentile95 / maxQuantityPerFlatFee),
                Math.floor(this.maximum / maxQuantityPerFlatFee)
        );
    }

    public Bounds ceilDiv(final double maxQuantityPerFlatFee, final double min) {
        return new Bounds(
                Math.max(Math.ceil(this.minimum / maxQuantityPerFlatFee), min),
                Math.max(Math.ceil(this.average / maxQuantityPerFlatFee), min),
                Math.max(Math.ceil(this.percentile95 / maxQuantityPerFlatFee), min),
                Math.max(Math.ceil(this.maximum / maxQuantityPerFlatFee), min)
        );
    }

    public static Bounds min(final Bounds a, final Bounds b) {
        double max = Math.min(a.getMaximum(), b.getMaximum());
        double p95 = Math.min(Math.min(a.getPercentile95(), b.getPercentile95()), max);
        double avg = Math.min(Math.min(a.getAverage(), b.getAverage()), p95);
        double min = Math.min(Math.min(a.getMinimum(), b.getMinimum()), avg);
        return new Bounds(min, avg, p95, max);
    }

    public static Bounds max(final Bounds a, final Bounds b) {
        double min = Math.max(a.getMinimum(), b.getMinimum());
        double avg = Math.max(Math.max(a.getAverage(), b.getAverage()), min);
        double p95 = Math.max(Math.max(a.getPercentile95(), b.getPercentile95()), avg);
        double max = Math.max(Math.max(a.getMaximum(), b.getMaximum()), p95);
        return new Bounds(min, avg, p95, max);
    }
}
