package net.susnjar.paniql.pricing;

import java.util.Collections;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class BoundsCollector implements Collector<Bounds, BoundsCollector, Bounds> {
    private double minimum = 0.0d;
    private double average = 0.0d;
    private double percentile95 = 0.0d;
    private double maximum = 0.0d;

    public void add(final Bounds other) {
        this.minimum += other.getMinimum();
        this.average += other.getAverage();
        this.percentile95 += other.getPercentile95();
        this.maximum += other.getMaximum();
    }

    public void add(final Bounds other, final double commonFactor) {
        this.minimum += other.getMinimum() * commonFactor;
        this.average += other.getAverage() * commonFactor;
        this.percentile95 += other.getPercentile95() * commonFactor;
        this.maximum += other.getMaximum() * commonFactor;
    }

    public void add(final Bounds other, final double minFactor, final double avgFactor, final double p95factor, final double maxFactor) {
        this.minimum += other.getMinimum() * minFactor;
        this.average += other.getAverage() * avgFactor;
        this.percentile95 += other.getPercentile95() * p95factor;
        this.maximum += other.getMaximum() * maxFactor;
    }

    public void add(final Bounds other, final Bounds factor) {
        add(other, factor.getMinimum(), factor.getAverage(), factor.getPercentile95(), factor.getMaximum());
    }

    public void add(final Bounds other, final double commonFactor, final Bounds factor) {
        add(other, commonFactor * factor.getMinimum(), commonFactor * factor.getAverage(), commonFactor * factor.getPercentile95(), commonFactor * factor.getMaximum());
    }

    public Bounds getCurrent() {
        return new Bounds(minimum, average, percentile95, maximum);
    }

    @Override
    public Supplier<BoundsCollector> supplier() {
        return () -> new BoundsCollector();
    }

    @Override
    public BiConsumer<BoundsCollector, Bounds> accumulator() {
        return (a, t) -> a.add(t);
    }

    @Override
    public BinaryOperator<BoundsCollector> combiner() {
        return (a, b) -> { a.add(b.getCurrent()); return a; };
    }

    @Override
    public Function<BoundsCollector, Bounds> finisher() {
        return (a) -> a.getCurrent();
    }

    @Override
    public Set<Characteristics> characteristics() {
        return Collections.emptySet();
    }
}
