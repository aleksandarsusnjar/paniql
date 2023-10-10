package net.susnjar.paniql.pricing;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a part cost for single invocation scenarios to be combined into {@link StepPricer}
 * to account for non-linear volume scaling.
 */
public class Price {
    public static final Price FREE = new Price();

    private final Bounds[] bounds = new Bounds[WorkType.values().length];

    private Price() {
        Arrays.fill(bounds, Bounds.ALWAYS_0);
    }

    protected Price(final Map<WorkType, Bounds> boundsMap) {
        Arrays.fill(bounds, Bounds.ALWAYS_0);

        for (final Map.Entry<WorkType, Bounds> entry: boundsMap.entrySet()) {
            bounds[entry.getKey().ordinal()] = entry.getValue();
        }
    }
    protected Price(final WorkType workType, final Bounds bounds) {
        if (workType == null) throw new IllegalArgumentException();
        if (bounds == null) throw new IllegalArgumentException();

        for (int i = 0; i < this.bounds.length; i++) {
            this.bounds[i] = (workType.ordinal() == i) ? bounds : Bounds.ALWAYS_0;
        }
    }

    protected Price(final Bounds... bounds) {
        if ((bounds == null) || (bounds.length > this.bounds.length)) throw new IllegalArgumentException();

        for (int i = 0; i < bounds.length; i++) {
            if (bounds[i] == null) throw new IllegalArgumentException("bounds[" + i + "]==null!");
            this.bounds[i] = bounds[i];
        }
    }

    public static Price of(final WorkType workType, final Bounds bounds) {
        return new Price(workType, bounds);
    }

    public static Price of(final Bounds... bounds) {
        return new Price(bounds);
    }

    public static Price of(final Map<WorkType, Bounds> boundsMaps) {
        return new Price(boundsMaps);
    }

    private Price(final Price a, final Price b, final BiFunction<Bounds, Bounds, Bounds> mergeOperator) {
        this(a.bounds, b.bounds, mergeOperator);
    }

    private Price(final Bounds[] a, final Bounds[] b, final BiFunction<Bounds, Bounds, Bounds> mergeOperator) {
        for (int i = 0; i < bounds.length; i++) {
            this.bounds[i] = mergeOperator.apply(a[i], b[i]);
        }
    }

    private Price(final Price a, final Function<Bounds, Bounds> transform) {
        for (int i = 0; i < bounds.length; i++) {
            this.bounds[i] = transform.apply(a.bounds[i]);
        }
    }

    public Bounds get(WorkType workType) {
        return bounds[workType.ordinal()];
    }

    public Price plus(final Price other) {
        return new Price(this, other, (a, b) -> a.plus(b));
    }

    public Price times(final Price other) {
        return new Price(this, other, (a, b) -> a.times(b));
    }

    public Price times(final Bounds commonPercentileFactors) {
        return new Price(this, commonPercentileFactors::times);
    }

    public Price times(final double commonFactor) {
        return new Price(this, b -> b.times(commonFactor));
    }

    public Price with(final WorkType workType, final Bounds newBounds) {
        final Bounds[] allBounds = Arrays.copyOf(this.bounds, this.bounds.length);
        allBounds[workType.ordinal()] = newBounds;
        return Price.of(allBounds);
    }

    public boolean isFree() {
        for (final Bounds b: bounds) {
            if (!b.isAlwaysZero()) return false;
        }
        return true;
    }

}
