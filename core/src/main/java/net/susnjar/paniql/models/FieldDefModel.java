package net.susnjar.paniql.models;

import graphql.language.*;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.Join;
import net.susnjar.paniql.Request;
import net.susnjar.paniql.pricing.*;

import java.util.*;
import java.util.stream.Collectors;

public class FieldDefModel extends ElementModel<FieldDefinition> {

    private static Bounds DEFAULT_COLLECTION_CARDINALITY = Bounds.HIGH_AVERAGE.times(10.0d);

    private final String identifier;
    private final OutputTypeModel<?,?> target;

    private final boolean toMany;

    private IdentityHashMap<ObjectTypeModel, Double> concreteOptionWeights = new IdentityHashMap<>();

    private double totalOptionWeight = 0.0d;

    private final HashSet<FieldDefModel> directGeneralizations = new HashSet<>();
    private final HashSet<FieldDefModel> allGeneralizations = new HashSet<>();
    private final HashSet<FieldDefModel> directSpecializations = new HashSet<>();
    private final HashSet<FieldDefModel> allSpecializations = new HashSet<>();

    private final Set<FieldDefModel> directGeneralizationsImmutable = Collections.unmodifiableSet(directGeneralizations);
    private final Set<FieldDefModel> allGeneralizationsImmutable = Collections.unmodifiableSet(allGeneralizations);
    private final Set<FieldDefModel> directSpecializationsImmutable = Collections.unmodifiableSet(directSpecializations);
    private final Set<FieldDefModel> allSpecializationsImmutable = Collections.unmodifiableSet(allSpecializations);

    private final FieldContainerModel<?, ?> container;

    FieldDefModel(
            final Environment environment,
            final FieldContainerModel<?, ?> container,
            final FieldDefinition definition
    ) {
        super(environment, definition);
        this.container = container;
        this.identifier = definition.getName();

        this.target = environment.getOutputType(getElementTypeName(definition.getType()).getName());

        this.toMany = isToMany(definition.getType());

        // TODO option weights
        for (ObjectTypeModel option: target.getAllObjectTypes()) {
            concreteOptionWeights.put(option, 1.0d);
        }
        this.totalOptionWeight = target.getAllObjectTypes().size();
    }

    void applyExtension(FieldDefinition extension) {
        // TODO
    }

    private TypeName getElementTypeName(final Type type) {
        if (type instanceof NonNullType) {
            return getElementTypeName(((NonNullType)type).getType());
        } else if (type instanceof ListType) {
            return getElementTypeName(((ListType)type).getType());
        } else if (type instanceof TypeName) {
            return (TypeName) type;
        } else {
            throw new RuntimeException();
        }
    }

    @Override
    protected Bounds getDefaultCardinality() {
        final FieldDefinition definition = getDefinition();
        return getCardinality(definition.getType(), false, false);
    }

    protected void applyPricingDefaults() {
        if (this.pricing == null) {
            if (getRootType().isMarkedAsResource()) {
                this.pricing = StepPricer.of(Price.of(WorkType.EFFICIENT_REMOTE_API_CALL, Bounds.ALWAYS_1));
            } else {
                this.pricing = StepPricer.of(Price.of(WorkType.LOCAL_CALL, Bounds.ALWAYS_1));
            }
        }
    }

    @Override
    public Boolean isShared() {
        Boolean shared = super.isShared();
        if (shared == null) {
            shared = getRootType().isShared();
        }
        return shared;
    }

    private Bounds getCardinality(final Type type, final boolean nonNull, final boolean collection) {
        if (type instanceof NonNullType) {
            return getCardinality(((NonNullType)type).getType(), true, collection);
        } else if (type instanceof ListType) {
            Bounds cardinality = getCardinality(((ListType)type).getType(), false, true);
            if (collection) {
                cardinality = cardinality.times(DEFAULT_COLLECTION_CARDINALITY);
            }
            return cardinality;
        } else if (type instanceof TypeName) {
            if (collection) {
                final TypeName typeName = (TypeName) type;
                final Bounds sourceCardinality = Bounds.max(Bounds.ALWAYS_1, container.getCardinality());
                final Bounds targetCardinality = getEnvironment().getOutputType(typeName.getName()).getCardinality();

                double p95x = sourceCardinality.getPercentile95() - sourceCardinality.getMinimum();
                double avgx = sourceCardinality.getAverage() - sourceCardinality.getMinimum();
                double p05e = (avgx * avgx / p95x) + sourceCardinality.getMinimum();

                Bounds cardinality = new Bounds(
                    targetCardinality.getMinimum() / sourceCardinality.getMaximum(),
                    targetCardinality.getAverage() / sourceCardinality.getAverage(),
                    targetCardinality.getPercentile95() / p05e,
                    targetCardinality.getMaximum() / sourceCardinality.getMinimum()
                );

                if (isShared()) {
                    cardinality = cardinality.times(1.0d, 3.0d, 10.0d, 20.0d);
                }
                return cardinality;
            } else if (nonNull) {
                return Bounds.ALWAYS_1;
            } else {
                return Bounds.HIGH_AVERAGE;
            }
        } else {
            throw new RuntimeException();
        }
    }

    private boolean isToMany(final Type type) {
        if (type instanceof NonNullType) {
            return isToMany(((NonNullType)type).getType());
        } else if (type instanceof ListType) {
            return true;
        } else if (type instanceof TypeName) {
            return false;
        } else {
            throw new RuntimeException();
        }
    }

    public String getSimpleName() {
        return identifier;
    }

    @Override
    public String getFullyQualifiedName() {
        return container.getFullyQualifiedName() + "." + getSimpleName();
    }

    public boolean isToMany() {
        return toMany;
    }

    public boolean isToOne() {
        return !toMany;
    }

    public Set<FieldDefModel> getDirectGeneralizations() {
        return directGeneralizationsImmutable;
    }

    public Set<FieldDefModel> getAllGeneralizations() {
        return allGeneralizationsImmutable;
    }

    public Set<FieldDefModel> getDirectSpecializations() {
        return directSpecializationsImmutable;
    }

    public Set<FieldDefModel> getAllSpecializations() {
        return allSpecializationsImmutable;
    }

    void addOption(final ObjectTypeModel concreteOption, final double weight) {
        final Double previousWeight = concreteOptionWeights.put(concreteOption, weight);
        if (previousWeight != null) {
            totalOptionWeight -= previousWeight;
        }
        totalOptionWeight += weight;
    }

    Invoice invoice(Request request, Join join, Bounds quantities, List<Field> requests) {
        final Invoice invoice = new Invoice();

        if (!isFree()) {
            invoice.add(this, (join != null) ? getJoinedPrice(quantities) : getStandalonePrice(quantities));
        }

        final Join effectiveJoinContext = (join != null) ? join :this.getJoin();

        for (Map.Entry<ObjectTypeModel, Double> optionEntry : concreteOptionWeights.entrySet()) {
            final ObjectTypeModel option = optionEntry.getKey();
            final double optionProbability = optionEntry.getValue() / totalOptionWeight;
            final Bounds optionCardinality = getCardinality().times(optionProbability).times(quantities);

            invoice.add(option.invoice(request, effectiveJoinContext, optionCardinality, requests.stream().map(f -> f.getSelectionSet()).collect(Collectors.toList())));
        }

        return invoice;
    }

    public boolean isJoinable() {
        return getCardinality().getMaximum() <= 1.0d;
    }

    public boolean isScalar() {
        return target == null;
    }

    @Override
    public OutputTypeModel getRootType() {
        return target;
    }

    void addDirectGeneralizations(List<FieldDefModel> fields) {
        directGeneralizations.addAll(fields);
    }

    void addDirectSpecializations(List<FieldDefModel> fields) {
        directSpecializations.addAll(fields);
    }

    void addAllGeneralizations(List<FieldDefModel> fields) {
        allGeneralizations.addAll(fields);
    }

    void addAllSpecializations(List<FieldDefModel> fields) {
        allSpecializations.addAll(fields);
    }
}
