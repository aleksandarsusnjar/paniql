package net.susnjar.paniql.models;

import graphql.language.FieldDefinition;
import graphql.language.SDLExtensionDefinition;
import graphql.language.TypeDefinition;
import net.susnjar.paniql.Environment;

import java.util.*;
import java.util.stream.Collectors;

public abstract class FieldContainerModel<T extends TypeDefinition<T>, X extends SDLExtensionDefinition> extends OutputTypeModel<T, X> {
    private final HashMap<String, FieldDefModel> fields = new HashMap<>();

    private Set<FieldDefModel> allFields = null;

    protected FieldContainerModel(final Environment environment, final T definition) {
        super(environment, definition);
    }

    void registerFields(List<FieldDefinition> defs) {
        for (final FieldDefinition def: defs) {
            final String name = def.getName();
            FieldDefModel data = fields.get(name);
            if (data == null) {
                data = new FieldDefModel(getEnvironment(), this, def);
                fields.put(name, data);
            } else {
                data.applyExtension(def);
            }
        }
    }

    @Override
    public boolean hasField(final String name) {
        return fields.containsKey(name);
    }

    @Override
    public FieldDefModel getField(final String name) {
        return fields.get(name);
    }

    @Override
    public void relateFields() {
        allFields = Collections.unmodifiableSet(new HashSet<>(fields.values()));

        for (final FieldDefModel fieldData: fields.values()) {
            final String name = fieldData.getSimpleName();
            fieldData.addDirectGeneralizations(getFields(name, getDirectGeneralizations()));
            fieldData.addAllGeneralizations(getFields(name, getAllGeneralizations()));
            fieldData.addDirectSpecializations(getFields(name, getDirectSpecializations()));
            fieldData.addAllSpecializations(getFields(name, getAllSpecializations()));
        }
    }

    Set<FieldDefModel> getFields() {
        return allFields;
    }

    private List<FieldDefModel> getFields(String name, Set<OutputTypeModel> classifiers) {
        return classifiers.stream()
                .map(c -> c.getField(name))
                .filter(f -> (f != null))
                .collect(Collectors.toList());
    }

    @Override
    public void applyFieldCardinalityDefaults() {
        super.applyFieldCardinalityDefaults();
        for (final FieldDefModel fieldData: fields.values()) {
            fieldData.applyCardinalityDefaults();
        }
    }

    @Override
    public void applyFieldPricingDefaults() {
        super.applyFieldPricingDefaults();
        for (final FieldDefModel fieldData: fields.values()) {
            fieldData.applyPricingDefaults();
        }
    }

    @Override
    public void processJoins() {
        super.processJoins();
        for (final FieldDefModel fieldData: fields.values()) {
            fieldData.processJoins();
        }
    }
}
