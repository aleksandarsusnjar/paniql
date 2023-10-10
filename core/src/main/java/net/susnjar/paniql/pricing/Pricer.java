package net.susnjar.paniql.pricing;

public interface Pricer {
    default Price getCost(final double quantity) {
        return getCost(new Bounds(quantity, quantity, quantity, quantity));
    }

    Price getCost(final Bounds quantities);

    boolean isFree();
}
