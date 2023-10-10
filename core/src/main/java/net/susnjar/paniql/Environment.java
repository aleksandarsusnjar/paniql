package net.susnjar.paniql;

import graphql.language.*;
import graphql.schema.idl.TypeDefinitionRegistry;
import net.susnjar.paniql.models.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Environment {
    private final TypeDefinitionRegistry typeRegistry;

    private final HashMap<String, OutputTypeModel> outputTypes = new HashMap<>();

    private final ObjectTypeModel queryType;
    private final ObjectTypeModel mutationType;
    private final ObjectTypeModel subscriptionType;

    public Environment(final TypeDefinitionRegistry typeRegistry) {
        this.typeRegistry = typeRegistry;

        ScalarModel.registerStandardTypes(this);
        registerCustomTypes();

        processTypeExtensions();
        initializeDirectRelations();
        establishIndirectRelations();

        discoverFields();
        relateFields();
        applyTypeCardinalityDefaults();
        applyFieldCardinalityDefaults();
        applyTypePricingDefaults();
        applyFieldPricingDefaults();
        processJoins();

        this.queryType = getOutputType("Query");
        this.mutationType = getOutputType("Mutation");
        this.subscriptionType = getOutputType("Subscription");
    }

    private void registerCustomTypes() {
        for (final TypeDefinition typeDef: typeRegistry.getTypes(TypeDefinition.class)) {
            OutputTypeModel typeModel = null;
            if (typeDef instanceof InterfaceTypeDefinition) {
                final InterfaceTypeDefinition actual = (InterfaceTypeDefinition)typeDef;
                typeModel = new InterfaceModel(this, actual);
            } else if (typeDef instanceof SDLExtensionDefinition) {
                // we'll handle this separately and expect to find the type to extend first.
                continue;
            } else if (typeDef instanceof EnumTypeDefinition) {
                final EnumTypeDefinition actual = (EnumTypeDefinition)typeDef;
                typeModel = new EnumTypeModel(this, actual);
            } else if (typeDef instanceof ScalarTypeDefinition) {
                final ScalarTypeDefinition actual = (ScalarTypeDefinition)typeDef;
                typeModel = new ScalarModel(this, actual);
            } else if (typeDef instanceof UnionTypeDefinition) {
                final UnionTypeDefinition actual = (UnionTypeDefinition)typeDef;
                typeModel = new UnionModel(this, actual);
            } else if (typeDef instanceof ObjectTypeDefinition) {
                final ObjectTypeDefinition actual = (ObjectTypeDefinition)typeDef;
                typeModel = new ObjectTypeModel(this, actual);
            }

            if (typeModel != null) {
                registerType(typeModel);
            }
        }
    }

    public void registerType(final OutputTypeModel typeModel) {
        outputTypes.put(typeModel.getSimpleName(), typeModel);
    }

    private void processTypeExtensions() {
        for (final TypeDefinition t: typeRegistry.getTypes(TypeDefinition.class)) {
            if (t instanceof EnumTypeExtensionDefinition) {
                final EnumTypeExtensionDefinition extension = (EnumTypeExtensionDefinition)t;
                final EnumTypeModel extendedType = (EnumTypeModel) outputTypes.get(extension.getName());
                extendedType.applyExtension(extension);
            } else if (t instanceof ScalarTypeExtensionDefinition) {
                final ScalarTypeExtensionDefinition extension = (ScalarTypeExtensionDefinition)t;
                final ScalarModel extendedType = (ScalarModel) outputTypes.get(extension.getName());
                extendedType.applyExtension(extension);
            } else if (t instanceof UnionTypeExtensionDefinition) {
                final UnionTypeExtensionDefinition extension = (UnionTypeExtensionDefinition)t;
                final UnionModel extendedType = (UnionModel) outputTypes.get(extension.getName());
                extendedType.applyExtension(extension);
            } else if (t instanceof ObjectTypeExtensionDefinition) {
                final ObjectTypeExtensionDefinition extension = (ObjectTypeExtensionDefinition)t;
                final ObjectTypeModel extendedType = (ObjectTypeModel) outputTypes.get(extension.getName());
                extendedType.applyExtension(extension);
            } else if (t instanceof InterfaceTypeExtensionDefinition) {
                final InterfaceTypeExtensionDefinition extension = (InterfaceTypeExtensionDefinition)t;
                final InterfaceModel extendedType = (InterfaceModel) outputTypes.get(extension.getName());
                extendedType.applyExtension(extension);
            }
        }
    }

    private void initializeDirectRelations() {
        outputTypes.forEach((name, type) -> type.processDirectRelations());
    }

    private void establishIndirectRelations() {
        outputTypes.forEach((name, type) -> type.processIndirectRelations());
    }

    private void discoverFields() {
        outputTypes.forEach((name, type) -> type.discoverFields());
    }

    private void relateFields() {
        outputTypes.forEach((name, type) -> type.relateFields());
    }

    private void applyTypeCardinalityDefaults() {
        outputTypes.forEach((name, type) -> type.applyTypeCardinalityDefaults());
    }

    private void applyFieldCardinalityDefaults() {
        outputTypes.forEach((name, type) -> type.applyFieldCardinalityDefaults());
    }

    private void applyTypePricingDefaults() {
        outputTypes.forEach((name, type) -> type.applyTypePricingDefaults());
    }

    private void applyFieldPricingDefaults() {
        outputTypes.forEach((name, type) -> type.applyFieldPricingDefaults());
    }

    private void processJoins() {
        outputTypes.forEach((name, type) -> type.processJoins());
    }

    public ObjectTypeModel getQueryType() {
        return queryType;
    }

    public ObjectTypeModel getMutationType() {
        return mutationType;
    }

    public ObjectTypeModel getSubscriptionType() {
        return subscriptionType;
    }

    public <T extends OutputTypeModel<?, ?>> T getOutputType(String name) {
        return (T)this.outputTypes.get(name);
    }

    public static String getPaniqlSchema() throws IOException {
        final String packagePath = Environment.class.getPackageName().replaceAll("\\.", "/");
        final String resourcePath = packagePath + "/PaniqlSchema.graphqls";
        try (
            final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
            final BufferedReader bufferedReader = new BufferedReader(reader);
        ) {
            return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
