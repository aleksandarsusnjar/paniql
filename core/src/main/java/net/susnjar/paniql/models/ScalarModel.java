package net.susnjar.paniql.models;

import graphql.language.ScalarTypeDefinition;
import graphql.language.ScalarTypeExtensionDefinition;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.pricing.Bounds;

public class ScalarModel extends OutputTypeModel<ScalarTypeDefinition, ScalarTypeExtensionDefinition> {
    public ScalarModel(Environment environment, final ScalarTypeDefinition def) {
        super(environment, def);
    }

    @Override
    protected Bounds getDefaultCardinality() {
        return Bounds.ALWAYS_1;
    }

    protected ScalarModel(Environment environment, final String identifier) {
        super(environment, identifier);
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isConcrete() {
        return true;
    }

    public void applyExtension(ScalarTypeExtensionDefinition extension) {
    }

    public static void registerStandardTypes(final Environment environment) {
        environment.registerType(new ScalarModel(environment, "ID"));
        environment.registerType(new ScalarModel(environment, "Boolean"));
        environment.registerType(new ScalarModel(environment, "Int"));
        environment.registerType(new ScalarModel(environment, "Float"));
        environment.registerType(new ScalarModel(environment, "String"));
    }
}
