package net.susnjar.paniql.pricing;

public class StepPricer implements Pricer {
    private final Price baseCost;
    private final Price unitCost;
    private final Double maxUnitsPerBase;

    protected StepPricer(final Price unitCost) {
        this((Price)null, unitCost, (Double)null);
    }

    protected StepPricer(final Price baseCost, final Price unitCost) {
        this(baseCost, unitCost, (Double)null);
    }

    protected StepPricer(final Price baseCost, final Price unitCost, final Double maxUnitsPerBase) {
        this.baseCost = (baseCost != null) ? baseCost : Price.FREE;
        this.unitCost = (unitCost != null) ? unitCost : Price.FREE;
        this.maxUnitsPerBase = maxUnitsPerBase;
    }
    
    public static StepPricer of(final Price unitCost) {
        return new StepPricer(unitCost);
    }

    public static StepPricer of(final Price baseCost, final Price unitCost) {
        return new StepPricer(baseCost, unitCost);
    }

    public static StepPricer of(final Price baseCost, final Price unitCost, final Double maxUnitsPerBase) {
        return new StepPricer(baseCost, unitCost, maxUnitsPerBase);
    }

    public Price getbaseCost() {
        return baseCost;
    }

    public Price getUnitCost() {
        return unitCost;
    }

    public Double getMaxUnitsPerBase() {
        return maxUnitsPerBase;
    }

    public boolean isFree() {
        return baseCost.isFree() && unitCost.isFree();
    }

    @Override
    public Price getCost(final Bounds quantities) {
        final Price base = getbaseCost();
        final Price baseExtended = (maxUnitsPerBase == null) ? base : base.times(quantities.ceilDiv(maxUnitsPerBase, 1.0d));
        return baseExtended.plus(getUnitCost().times(quantities));
    }
}
