package net.susnjar.paniql.pricing;

import net.susnjar.paniql.models.FieldDefModel;
import net.susnjar.paniql.models.OutputTypeModel;

import java.util.IdentityHashMap;
import java.util.Map;

public class Invoice {

    private final IdentityHashMap<OutputTypeModel, Price> resourceCosts;
    private final IdentityHashMap<OutputTypeModel, Price> partCosts;
    private final IdentityHashMap<FieldDefModel, Price> fieldCosts;

    public Invoice() {
        this(new IdentityHashMap<>(), new IdentityHashMap<>(), new IdentityHashMap<>());
    }

    private Invoice(
            IdentityHashMap<OutputTypeModel, Price> resourceCostsSeed,
            IdentityHashMap<OutputTypeModel, Price> partCostsSeed,
            IdentityHashMap<FieldDefModel, Price> fieldCostsSeed
    ) {
        this.resourceCosts = resourceCostsSeed;
        this.partCosts = partCostsSeed;
        this.fieldCosts = fieldCostsSeed;
    }

    public Invoice plus(final Invoice other) {
        return new Invoice(
                add(this.resourceCosts, other.resourceCosts),
                add(this.partCosts, other.partCosts),
                add(this.fieldCosts, other.fieldCosts)
        );
    }

    public void add(final OutputTypeModel type, final Price extraQuotations) {
        if ((extraQuotations == null) || type.isFree()) return;
        if (type.isMarkedAsResource()) {
            this.resourceCosts.compute(type, (key, existing) -> (existing == null) ? extraQuotations : existing.plus(extraQuotations));
        } else {
            this.partCosts.compute(type, (key, existing) -> (existing == null) ? extraQuotations : existing.plus(extraQuotations));
        }
    }

    public void add(final FieldDefModel field, final Price extraQuotations) {
        if (extraQuotations == null) return;
        this.fieldCosts.compute(field, (key, existing) -> (existing == null) ? extraQuotations : existing.plus(extraQuotations));
    }
    
    public void add(Invoice other) {
        addInPlace(this.resourceCosts, other.resourceCosts);
        addInPlace(this.partCosts, other.partCosts);
        addInPlace(this.fieldCosts, other.fieldCosts);
    }

    private <T> IdentityHashMap<T, Price> add(Map<T, Price> a, Map<T, Price> b) {
        final IdentityHashMap<T, Price> sum = new IdentityHashMap<>(a);
        addInPlace(sum, b);
        return sum;
    }

    private <T> void addInPlace(Map<T, Price> sum, Map<T, Price> b) {
        for (final Map.Entry<T, Price> bEntry: b.entrySet()) {
            final T key = bEntry.getKey();
            final Price currentQuotation = sum.get(key);
            final Price bQuotation = b.get(key);
            sum.put(key, (currentQuotation == null) ? bQuotation : currentQuotation.plus(bQuotation));
        }
    }

    public Invoice times(final Price operationFactors) {
        return new Invoice(
                multiply(this.resourceCosts, operationFactors),
                multiply(this.partCosts, operationFactors),
                multiply(this.fieldCosts, operationFactors)
        );
    }

    private <T> IdentityHashMap<T, Price>  multiply(Map<T, Price> a, Price operationFactors) {
        final IdentityHashMap<T, Price> product = new IdentityHashMap<>(a.size());

        for (final Map.Entry<T, Price> aEntry: a.entrySet()) {
            final T key = aEntry.getKey();
            final Price aQuotation = a.get(key);
            product.put(key, aQuotation.times(operationFactors));
        }
        return product;
    }

    public Invoice times(final Bounds percentilesFactor) {
        return new Invoice(
                multiply(this.resourceCosts, percentilesFactor),
                multiply(this.partCosts, percentilesFactor),
                multiply(this.fieldCosts, percentilesFactor)
        );
    }

    private <T> IdentityHashMap<T, Price>  multiply(Map<T, Price> a, Bounds percentilesFactor) {
        final IdentityHashMap<T, Price> product = new IdentityHashMap<>(a.size());

        for (final Map.Entry<T, Price> aEntry: a.entrySet()) {
            final T key = aEntry.getKey();
            final Price aQuotation = a.get(key);
            product.put(key, aQuotation.times(percentilesFactor));
        }
        return product;
    }

    public Invoice times(final double commonFactor) {
        return new Invoice(
                multiply(this.resourceCosts, commonFactor),
                multiply(this.partCosts, commonFactor),
                multiply(this.fieldCosts, commonFactor)
        );
    }

    private <T> IdentityHashMap<T, Price>  multiply(Map<T, Price> a, double commonFactor) {
        final IdentityHashMap<T, Price> product = new IdentityHashMap<>(a.size());

        for (final Map.Entry<T, Price> aEntry: a.entrySet()) {
            final T key = aEntry.getKey();
            final Price aQuotation = a.get(key);
            product.put(key, aQuotation.times(commonFactor));
        }
        return product;
    }

    public Map<OutputTypeModel, Price> getResourceCosts() {
        return resourceCosts;
    }

    public Map<OutputTypeModel, Price> getPartCosts() {
        return partCosts;
    }

    public Map<FieldDefModel, Price> getFieldCosts() {
        return fieldCosts;
    }
}
