package net.susnjar.paniql.models;

import graphql.language.*;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.Join;
import net.susnjar.paniql.Request;
import net.susnjar.paniql.pricing.Bounds;
import net.susnjar.paniql.pricing.Invoice;
import net.susnjar.paniql.pricing.Price;
import net.susnjar.paniql.pricing.WorkType;

import java.util.*;

public class ObjectTypeModel extends FieldContainerModel<ObjectTypeDefinition, ObjectTypeExtensionDefinition> {

    private final HashMap<String, FieldDefModel> fieldCosts = new HashMap<>();

    public ObjectTypeModel(Environment environment, ObjectTypeDefinition definition) {
        super(environment, definition);
    }

    @Override
    protected Bounds getDefaultCardinality() {
        return Bounds.LOW_AVERAGE.times(500);
    }

    void addField(final FieldDefModel fieldData) {
        fieldCosts.put (fieldData.getSimpleName(), fieldData);
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isConcrete() {
        return true;
    }

    @Override
    public void processDirectRelations() {
        for (final Type t: getDefinition().getImplements()) {
            if (t instanceof TypeName) {
                addDirectGeneralization(getOutputType(((TypeName)t).getName()));
            }
        }
        for (final ObjectTypeExtensionDefinition x: getExtensions()) {
            for (final Type t : x.getImplements()) {
                if (t instanceof TypeName) {
                    addDirectGeneralization(getOutputType(((TypeName) t).getName()));
                }
            }
        }
    }


    @Override
    public void discoverFields() {
        registerFields(getDefinition().getFieldDefinitions());

        for (final ObjectTypeExtensionDefinition x: getExtensions()) {
            registerFields(x.getFieldDefinitions());
        }
    }

    public Invoice invoice(final Request request, final Join joinContext, Bounds quantities, final Collection<? extends SelectionSet> selectionSets) {
        final Invoice invoice = new Invoice();

        if (isFree()) {
            invoice.add(this, Price.of(WorkType.INSTANCE_ACCESS, quantities));
        } else {
            invoice.add(this, (joinContext != null) ? getJoinedPrice(quantities) : getStandalonePrice(quantities));
        }

        final Join effectiveJoinContext = (joinContext != null) ? joinContext :this.getJoin();

        // Map of field data -> alias -> complete stated field requests
        final HashMap<FieldDefModel, Map<String, List<Field>>> fieldRequests = new HashMap<>();

        addFieldSelections(request, fieldRequests, selectionSets);

        for (final Map.Entry<FieldDefModel, Map<String, List<Field>>> entry: fieldRequests.entrySet()) {
            final FieldDefModel fieldData = entry.getKey();
            final Join nestedJoinContext = (effectiveJoinContext == null) ? null : effectiveJoinContext.getFieldJoin(fieldData);
            Map<String, List<Field>> aliasesToRequests = entry.getValue();
            for (final List<Field> aliasRequests: aliasesToRequests.values()) {
                invoice.add(fieldData.invoice(request, nestedJoinContext, quantities, aliasRequests));
            }
        }

        return invoice;
    }

    private void addFieldSelections(
            final Request request,
            final Map<FieldDefModel, Map<String, List<Field>>> fieldRequests,
            final Collection<? extends SelectionSet> selectionSets
    ) {
        for (final SelectionSet selectionSet : selectionSets) {
            addFieldSelections(request, fieldRequests, selectionSet);
        }
    }

    private void addFieldSelections(
            final Request request,
            final Map<FieldDefModel, Map<String, List<Field>>> fieldRequests,
            final SelectionSet selectionSet
    ) {
        for (final Node selection : selectionSet.getChildren()) {
            final Class<?> selectionType = selection.getClass();
            if (Field.class.isAssignableFrom(selectionType)) {
                final Field field = (Field) selection;
                final String fieldName = field.getName();
                final FieldDefModel annotatedField = getField(fieldName);
                final String alias = annotatedField.isAlwaysRecomputed() ? field.getAlias() : fieldName;
                fieldRequests
                        .computeIfAbsent(annotatedField, fd -> new HashMap<>())
                        .computeIfAbsent(alias, a -> new ArrayList<>())
                        .add(field);
            } else if (InlineFragment.class.isAssignableFrom(selectionType)) {
                final InlineFragment inlineFragment = (InlineFragment) selection;
                final OutputTypeModel subcontextType = getOutputType(inlineFragment.getTypeCondition().getName());
                if ((subcontextType == this) && this.isAssignableTo(subcontextType)) {
                    addFieldSelections(request, fieldRequests, inlineFragment.getSelectionSet());
                }
            } else if (FragmentSpread.class.isAssignableFrom(selectionType)) {
                final FragmentSpread spread = (FragmentSpread) selection;
                final List<FragmentDefinition> fragmentDefs = request.getFragment(spread.getName());

                for (final FragmentDefinition fragment : fragmentDefs) {
                    final OutputTypeModel subcontextType = getOutputType(fragment.getTypeCondition().getName());
                    if (this.isAssignableTo(subcontextType)) {
                        addFieldSelections(request, fieldRequests, fragment.getSelectionSet());
                    }
                }
            } else {
                throw new RuntimeException("Unsupported selection type: " + selection.getClass());
            }
        }
    }
}
