package net.susnjar.paniql.models;

import graphql.language.Type;
import graphql.language.TypeName;
import graphql.language.UnionTypeDefinition;
import graphql.language.UnionTypeExtensionDefinition;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.pricing.Bounds;
import net.susnjar.paniql.pricing.BoundsCollector;

public class UnionModel extends OutputTypeModel<UnionTypeDefinition, UnionTypeExtensionDefinition> {
    public UnionModel(Environment environment, UnionTypeDefinition definition) {
        super(environment, definition);
    }

    protected Bounds getDefaultCardinality() {
        return getAllObjectTypes().stream().map(OutputTypeModel::getCardinality).collect(new BoundsCollector());
    }

    public boolean isAbstract() {
        return true;
    }

    public boolean isConcrete() {
        return false;
    }

    public void processDirectRelations() {
        for (final Type t: getDefinition().getMemberTypes()) {
            if (t instanceof TypeName) {
                getOutputType(((TypeName)t).getName()).addDirectGeneralization(this);
            }
        }
        for (final UnionTypeExtensionDefinition x: getExtensions()) {
            for (final Type t : x.getMemberTypes()) {
                if (t instanceof TypeName) {
                    getOutputType(((TypeName) t).getName()).addDirectGeneralization(this);
                }
            }
        }
    }



}
