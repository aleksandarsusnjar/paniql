package net.susnjar.paniql.models;

import graphql.language.SDLExtensionDefinition;
import graphql.language.TypeDefinition;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.util.IdentityHashSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class OutputTypeModel<T extends TypeDefinition<T>, X extends SDLExtensionDefinition> extends ElementModel<T> {

    private final List<X> extensions = new ArrayList<>();

    private final String identifier;

    private final IdentityHashSet<OutputTypeModel> directGeneralizations = new IdentityHashSet<>();
    private final IdentityHashSet<OutputTypeModel> directSpecializations = new IdentityHashSet<>();
    private final IdentityHashSet<OutputTypeModel> allGeneralizations = new IdentityHashSet<>();
    private final IdentityHashSet<OutputTypeModel> allSpecializations = new IdentityHashSet<>();
    private final IdentityHashSet<OutputTypeModel> allAssignableTo = new IdentityHashSet<>();
    private final IdentityHashSet<OutputTypeModel> allAssignableFrom = new IdentityHashSet<>();

    private boolean indirectGeneralizationsProcessed = false;
    private boolean indirectSpecializationsProcessed = false;

    private final IdentityHashSet<ObjectTypeModel> allObjectTypes = new IdentityHashSet<>();

    private final boolean markedAsResource;

    protected OutputTypeModel(final Environment environment, final T definition) {
        this(environment, definition, definition.getName());
    }

    protected OutputTypeModel(final Environment environment, String identifier) {
        this(environment, null, identifier);
    }

    protected OutputTypeModel(final Environment environment, final T definition, final String identifier) {
        super(environment, definition);
        this.identifier = identifier;
        this.markedAsResource = (definition != null) && definition.hasDirective("paniqlResource");
    }

    public void applyExtension(X extension) {
        this.extensions.add(extension);
    }

    @Override
    protected OutputTypeModel<?, ?> getRootType() {
        return this;
    }

    protected List<X> getExtensions() {
        return extensions;
    }

    void addDirectGeneralization(OutputTypeModel generalization) {
        directGeneralizations.add(generalization);
        generalization.directSpecializations.add(this);
    }

    public Set<OutputTypeModel> getDirectGeneralizations() {
        return directGeneralizations;
    }

    public Set<OutputTypeModel> getDirectSpecializations() {
        return directSpecializations;
    }

    public Set<OutputTypeModel> getAllGeneralizations() {
        return allGeneralizations;
    }

    public Set<OutputTypeModel> getAllSpecializations() {
        return allSpecializations;
    }

    public Set<OutputTypeModel> getAllAssignableTo() {
        return allAssignableTo;
    }

    public Set<OutputTypeModel> getAllAssignableFrom() {
        return allAssignableFrom;
    }

    public Set<ObjectTypeModel> getAllObjectTypes() {
        return allObjectTypes;
    }

    public boolean is(final OutputTypeModel classifier) {
        return allAssignableTo.contains(classifier);
    }

    @Override
    public String getSimpleName() {
        return identifier;
    }

    @Override
    public String getFullyQualifiedName() {
        return getSimpleName();
    }

    public abstract boolean isAbstract();

    public abstract boolean isConcrete();

    public boolean hasField(final String name) {
        return false;
    }

    public FieldDefModel getField(final String name) {
        return null;
    }

    public boolean isAssignableTo(OutputTypeModel other) {
        return allAssignableTo.contains(other);
    }

    public void processDirectRelations() {
    }

    public void processIndirectRelations() {
        if (!indirectGeneralizationsProcessed) {
            addAsGeneralizationTo(this);
        }
        if (!indirectSpecializationsProcessed) {
            addAsSpecializationTo(this);
        }
    }

    private void addAsGeneralizationTo(OutputTypeModel<T,X> origin) {
        if (!indirectGeneralizationsProcessed) {
            for (OutputTypeModel generalization: directGeneralizations) {
                generalization.addAsGeneralizationTo(this);
            }
            allAssignableTo.add(this);
            allAssignableTo.addAll(allGeneralizations);
            indirectGeneralizationsProcessed = true;
        }
        if (origin != this) origin.allGeneralizations.add(this);
        origin.allGeneralizations.addAll(allGeneralizations);
    }

    private void addAsSpecializationTo(OutputTypeModel<T,X> origin) {
        if (!indirectSpecializationsProcessed) {
            for (OutputTypeModel specialization: directSpecializations) {
                specialization.addAsSpecializationTo(this);
            }
            allAssignableFrom.add(this);
            allAssignableFrom.addAll(allSpecializations);

            for (final OutputTypeModel<?, ?> spec: allAssignableFrom) {
                if (spec instanceof ObjectTypeModel) {
                    final ObjectTypeModel objectType = (ObjectTypeModel)spec;
                    allObjectTypes.add(objectType);
                }
            }

            indirectSpecializationsProcessed = true;
        }
        if (origin != this) origin.allSpecializations.add(this);
        origin.allSpecializations.addAll(allAssignableFrom);
    }

    public void discoverFields() {
    }

    public void relateFields() {
    }

    public void applyTypeCardinalityDefaults() {
        super.applyCardinalityDefaults();
    }

    public void applyFieldCardinalityDefaults() {
    }

    public void applyTypePricingDefaults() {
        // nothing for now
    }

    public void applyFieldPricingDefaults() {
    }

    public boolean isMarkedAsResource() {
        return markedAsResource || allGeneralizations.stream().anyMatch(OutputTypeModel::isMarkedAsResource);
    }
}