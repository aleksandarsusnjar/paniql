package net.susnjar.paniql.models;

import graphql.language.EnumTypeDefinition;
import graphql.language.EnumTypeExtensionDefinition;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.pricing.Bounds;

public class EnumTypeModel extends OutputTypeModel<EnumTypeDefinition, EnumTypeExtensionDefinition> {
    public EnumTypeModel(Environment environment, final EnumTypeDefinition def) {
        super(environment, def);
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isConcrete() {
        return true;
    }

    public void applyExtension(EnumTypeExtensionDefinition extension) {
    }

    @Override
    protected Bounds getDefaultCardinality() {
        double valueCount = getDefinition().getEnumValueDefinitions().size();
        return new Bounds(
            0.0d,
            Math.max(1.0d, valueCount / 10.0d),
            valueCount * 0.7d,
            valueCount
        );
    }
}
