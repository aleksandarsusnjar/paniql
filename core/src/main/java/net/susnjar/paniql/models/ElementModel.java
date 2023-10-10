package net.susnjar.paniql.models;

import graphql.language.*;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.Join;
import net.susnjar.paniql.pricing.*;
import net.susnjar.paniql.util.GraphQLParsing;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class ElementModel<D extends DirectivesContainer<D>> {
    public static final double LOG_BASE_0_95 = Math.log(0.95d);
    public static final double LOG_BASE_1_9 = Math.log(1.9d);

    private final Environment environment;
    private final D definition;
    private final Directive directive;
    private Boolean shared;
    private boolean alwaysRecomputed;

    private Bounds cardinality = null;
    protected Pricer pricing = null;
    private Join join = null;

    public ElementModel(final Environment environment, final D definition) {
        this.environment = environment;
        this.definition = definition;

        if (definition == null) {
            // Built-in type.
            this.cardinality = Bounds.ALWAYS_1;
            this.pricing = StepPricer.of(Price.of(WorkType.TRIVIAL_RETURN, Bounds.ALWAYS_1));
            this.join = null;
            this.alwaysRecomputed = false;
            this.shared = false;
            this.directive = null;
        } else {
            if (definition.hasDirective("paniqlFree")) {
                if (definition.hasDirective("paniql")) {
                    throw new IllegalArgumentException("Must not specified @paniqlFree together with @paniql");
                }
                this.cardinality = Bounds.ALWAYS_1;
                this.pricing = StepPricer.of(Price.FREE);
                this.directive = null;
            } else {
                final List<Directive> paniqlDirectives = definition.getDirectives("paniql");

                if ((paniqlDirectives == null) || paniqlDirectives.isEmpty()) {
                    applyDefaultCost();
                    this.directive = null;
                } else if (paniqlDirectives.size() > 1) {
                    throw new IllegalArgumentException("At most one @paniql permitted.");
                } else {

                    this.directive = paniqlDirectives.get(0);
                    applyPaniqlDirective();
                }
            }
        }
    }

    public Boolean isShared() {
        return shared;
    }

    public boolean isFree() {
        return pricing.isFree();
    }

    public Bounds getCardinality() {
        return cardinality;
    }

    protected void applyDefaultCost() {
        cardinality = null;
        pricing = null;
        join = null;
    }

    private static final double log0_95(final double arg) {
        return Math.log(arg) / LOG_BASE_0_95;
    }

    private static final double log1_9(final double arg) {
        return Math.log(arg) / LOG_BASE_1_9;
    }

    protected void applyPaniqlDirective() {
        applyAlways();
        applyShared();
        applyCardinality();
        applyPricing();
    }

    private void applyAlways() {
        Argument alwaysArg = directive.getArgument("always");
        if (alwaysArg == null) {
            this.alwaysRecomputed = false;
        } else {
            Value alwaysValue = alwaysArg.getValue();
            if (alwaysValue instanceof BooleanValue) {
                alwaysRecomputed = ((BooleanValue) alwaysValue).isValue();
            } else {
                this.alwaysRecomputed = false;
            }
        }
    }

    private void applyShared() {
        final Argument sharedArg = directive.getArgument("shared");
        if (sharedArg == null) {
            this.shared = null;
        } else {
            this.shared = ((BooleanValue) sharedArg.getValue()).isValue();
        }
    }

    private void applyCardinality() {
        final Argument qArg = directive.getArgument("q");
        if (qArg == null) {
            this.cardinality = null;
        } else {
            final Value qVal = qArg.getValue();
            if (qVal instanceof ObjectValue) {
                cardinality = parseBounds((ObjectValue) qVal);
            } else {
                this.cardinality = null;
            }
        }

        if (cardinality == null) {
            cardinality = getDefaultCardinality();
        }
    }

    private void applyPricing() {
        final Price base = parsePrice(directive, "base");
        final Price unit = parsePrice(directive, "unit");
        final Double maxUnitsPerBase = GraphQLParsing.getFloatValue(directive.getArgument("maxUnitsPerBase"), () -> null);
        this.pricing = StepPricer.of(base, unit, maxUnitsPerBase);
    }

    private Price parsePrice(final Directive directive, final String argName) {
        Argument argument = directive.getArgument(argName);
        if (argument == null) return null;

        final Value value = argument.getValue();
        if ((value == null) || (value instanceof NullValue)) return null;

        final ObjectValue obj = (ObjectValue) value;

        final Map<String, Value> params = obj.getObjectFields().stream()
                .collect(Collectors.toMap(ObjectField::getName, ObjectField::getValue));

        final Bounds[] bounds = new Bounds[WorkType.values().length];

        for (final WorkType workType: WorkType.values()) {
            final Value paramValue = params.get(workType.getId());
            if ((paramValue != null) && !(paramValue instanceof NullValue)) {
                final ObjectValue paramObj = (ObjectValue)paramValue;
                bounds[workType.ordinal()] = parseBounds(paramObj);
            } else if (workType == WorkType.INSTANCE_ACCESS) {
                bounds[workType.ordinal()] = Bounds.ALWAYS_1;
            } else {
                bounds[workType.ordinal()] = Bounds.ALWAYS_0;
            }
        }

        return Price.of(bounds);
    }

    protected void applyCardinalityDefaults() {
        if (cardinality == null) {
            cardinality = getDefaultCardinality();
        }
    }

    public void processJoins() {
        if (definition == null) return;
        if (directive == null) return;

        final Argument joinsArg = directive.getArgument("joins");
        if (joinsArg == null) {
            this.join = null;
        } else {
            final Value joinsValue = joinsArg.getValue();
            if (joinsValue instanceof ArrayValue) {
                this.join = parseJoins((ArrayValue) joinsValue);
            } else {
                this.join = null;
            }
        }
    }

    private Join parseJoins(final ArrayValue joinsArray) {
        final Join join = new Join();

        for (final Value joinValue: joinsArray.getValues()) {
            if (joinValue instanceof ObjectValue) {
                addJoin(join, null, ((ObjectValue) joinValue));
            }
        }

        return join;
    }

    protected abstract Bounds getDefaultCardinality();


    protected Bounds parseBounds(final ObjectValue boundsValue) {
        final Map<String, Value> params = boundsValue.getObjectFields().stream()
                .collect(Collectors.toMap(ObjectField::getName, ObjectField::getValue));

        final Double specifiedConst = GraphQLParsing.getFloatValue(params.get("const"), null);
        final Double specifiedMin = GraphQLParsing.getFloatValue(params.get("min"), null);
        final Double specifiedAvg = GraphQLParsing.getFloatValue(params.get("avg"), null);
        final Double specifiedP95 = GraphQLParsing.getFloatValue(params.get("p95"), null);
        final Double specifiedMax = GraphQLParsing.getFloatValue(params.get("max"), null);

        if (specifiedConst != null) {
            if ((specifiedMin != null) || (specifiedAvg != null) || (specifiedP95 != null) || (specifiedMax != null)) {
                throw new IllegalArgumentException("Constant value cannot be specified together with others.");
            }
            return new Bounds(specifiedConst, specifiedConst, specifiedConst, specifiedConst);
        } else {
            Double min = (specifiedMin == null) ? 0.0d : specifiedMin;
            Double avg = specifiedAvg;
            Double p95 = specifiedP95;
            Double max = specifiedMax;

            if (specifiedMax != null) {
                if (specifiedP95 != null) {
                    if (specifiedAvg == null) {
                        double span = specifiedMax - specifiedMin;
                        double gamma = log0_95((specifiedP95 - min) / (specifiedMax - min));
                        avg = Math.pow(0.5d, gamma) * span + min;
                    }
                } else {
                    p95 = (max - min) * 0.95d + min;
                    if (specifiedAvg == null) {
                        avg = (max - min) * 0.5d + min;
                    }
                }
            } else if (specifiedP95 != null) {
                if (specifiedAvg != null) {
                    double gamma = log1_9(specifiedP95 / specifiedAvg);
                    max = specifiedP95 / Math.pow(0.95, gamma) + min;
                } else {
                    max = (specifiedP95 - min) / 0.95d + min;
                    avg = (max - min) * 0.5d + min;
                }
            } else if (specifiedAvg != null) {
                max = avg + (avg - min);
                p95 = (max - min) * 0.95d + min;
            } else {
                // We may only have a minimum, maybe not even that.
                return null;
            }

            return new Bounds(min, avg, p95, max);
        }
    }

    public Environment getEnvironment() {
        return environment;
    }

    protected final D getDefinition() {
        return definition;
    }

    protected boolean isAlwaysRecomputed() {
        return alwaysRecomputed;
    }

    public Price getStandalonePrice(final Bounds quantities) {
        return pricing.getCost(quantities);
    }

    public Price getJoinedPrice(final Bounds quantities) {
        return Price.of(WorkType.BULK_JOIN, quantities).plus(Price.of(WorkType.INSTANCE_ACCESS, quantities));
    }

    protected OutputTypeModel getOutputType(final String name) {
        return environment.getOutputType(name);
    }

    private void addJoin(final Join target, Integer remainingAutoDepth, ObjectValue joinValue) {
        final Set<ObjectTypeModel> possibleTypes = getRootType().getAllObjectTypes();
        final Set<ObjectTypeModel> focusedTypes;

        final Map<String, Value> fieldValues = joinValue.getObjectFields().stream()
                .collect(Collectors.toMap(ObjectField::getName, ObjectField::getValue));

        final Value typesValue = fieldValues.get("types");

        final List<OutputTypeModel> types = GraphQLParsing.getArrayValue(
            typesValue,
            null,
            value -> environment.getOutputType(GraphQLParsing.getStringValue(value))
        );

        if (types == null) {
            focusedTypes = possibleTypes;
        } else {
            focusedTypes = new HashSet<>();
            for (final OutputTypeModel t: types) {
                focusedTypes.addAll(t.getAllObjectTypes());
            }
            focusedTypes.removeIf(t -> !possibleTypes.contains(t));
        }

        if (focusedTypes.isEmpty()) return;

        final int autoDepth = (remainingAutoDepth != null) ? remainingAutoDepth : GraphQLParsing.getIntValue(fieldValues.get("autoDepth"), () -> 0);
        final Integer nextDepth = autoDepth - 1;

        final List<String> specifiedFields = GraphQLParsing.getArrayValue(fieldValues.get("fields"), null, GraphQLParsing::getStringValue);
        final List<ObjectValue> sub = GraphQLParsing.getArrayValue(fieldValues.get("sub"), null, GraphQLParsing::getObjectValue);

        for (final ObjectTypeModel concreteType: focusedTypes) {
            if (autoDepth > 0) {
                final Set<FieldDefModel> autoJoinFields = concreteType.getFields().stream()
                        .filter(f -> f.isScalar()|| f.isToOne())
                        .collect(Collectors.toSet());

                for (final FieldDefModel field: autoJoinFields) {
                    target.joinField(field, parseJoins(nextDepth, null));
                }
            }

            if (specifiedFields != null) {
                for (final String specifiedFieldName : specifiedFields) {
                    final FieldDefModel field = concreteType.getField(specifiedFieldName);
                    if (field != null) {
                        final Join explicitJoin = parseJoins(null, sub);
                        final Join existing = target.getFieldJoin(field);
                        if (existing == null) {
                            target.joinField(field, explicitJoin);
                        } else {
                            existing.incorporate(explicitJoin);
                        }
                    }
                }
            }
        }
    }

    private Join parseJoins(Integer remainingAutoDepth, List<ObjectValue> joinValues) {
        if ((joinValues == null) || joinValues.isEmpty()) return null;

        final Join target = new Join();
        for (final ObjectValue joinValue: joinValues) {
            addJoin(target, remainingAutoDepth, joinValue);
        }

        return target;
    }

    protected Join getJoin() {
        return this.join;
    }

    protected abstract OutputTypeModel<?, ?> getRootType();

    public abstract String getSimpleName();

    public abstract String getFullyQualifiedName();

    @Override
    public String toString() {
        return getFullyQualifiedName();
    }

}
