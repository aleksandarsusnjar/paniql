package net.susnjar.paniql.models;

import graphql.language.InterfaceTypeDefinition;
import graphql.language.InterfaceTypeExtensionDefinition;
import graphql.language.Type;
import graphql.language.TypeName;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.pricing.Bounds;
import net.susnjar.paniql.pricing.BoundsCollector;

public class InterfaceModel extends FieldContainerModel<InterfaceTypeDefinition, InterfaceTypeExtensionDefinition> {
    public InterfaceModel(Environment environment, InterfaceTypeDefinition definition) {
        super(environment, definition);
    }

    @Override
    protected Bounds getDefaultCardinality() {
        return getAllObjectTypes().stream().map(OutputTypeModel::getCardinality).collect(new BoundsCollector());
    }

    public boolean isAbstract() {
        return true;
    }

    public boolean isConcrete() {
        return false;
    }


    @Override
    public void processDirectRelations() {
        for (final Type t: getDefinition().getImplements()) {
            if (t instanceof TypeName) {
                addDirectGeneralization(getOutputType(((TypeName)t).getName()));
            }
        }
        for (final InterfaceTypeExtensionDefinition x: getExtensions()) {
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

        for (final InterfaceTypeExtensionDefinition x: getExtensions()) {
            registerFields(x.getFieldDefinitions());
        }
    }
}
