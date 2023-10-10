package net.susnjar.paniql;

import graphql.language.*;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import net.susnjar.paniql.models.*;
import net.susnjar.paniql.pricing.Invoice;

import javax.print.Doc;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Representation of the API schema and environment, main entry point.
 *
 * Use:
 *
 * <ol>
 *     <li>Use one of the constructors to instantiate the environment,
 *         passing the GraphQL schema in any of the supported ways, i.e.:<ul>
 *        <li>{@link #Environment(String...)}  }</li>
 *        <li>{@link #Environment(File...)}  }</li>
 *        <li>{@link #Environment(Path...)}  }</li>
 *        <li>{@link #Environment(Collection)}  }</li>
 *        <li>{@link #Environment(TypeDefinitionRegistry)}  }</li>
 *     </ul></li>
 *
 *     <li>Reuse that environment to get {@linkplain Invoice invoices} for each
 *         {@linkplain #request(String) request} by invoking either the
 *         {@link #invoice(String)} or {@link #invoice(Document)} method.
 *     </li>
 *
 *     <li>Inspect and react to the data reported in the resulting {@linkplain Invoice invoice}.</li>
 * </ol>
 *
 * Example:
 *
 * <code>
 *     final Environment environment = new Environment(Path.of("/some/dir/api-schema.graphqls"));
 *     final Invoice invoice1 = environment.invoice("{ folder(id: 123) { id } }");
 *     final Invoice invoice2 = environment.invoice(someRequestString);
 *     final Invoice invoice3 = environment.invoice(parsedRequestDocument);
 * </code>
 */
public class Environment {
    private static final String SCHEMA_SEPARATOR = System.lineSeparator() + System.lineSeparator();

    private final TypeDefinitionRegistry typeRegistry;

    private final HashMap<String, OutputTypeModel> outputTypes = new HashMap<>();

    private final ObjectTypeModel queryType;
    private final ObjectTypeModel mutationType;
    private final ObjectTypeModel subscriptionType;

    public Environment(final File... schemaFiles) throws IOException {
        this(Arrays.asList(schemaFiles).stream().map(File::toPath).collect(Collectors.toList()));
    }

    public Environment(final Path... schemaPaths) throws IOException {
        this(Arrays.asList(schemaPaths));
    }

    public Environment(final Collection<Path> schemaPaths) throws IOException {
        this(parsePathSchemas(schemaPaths));
    }

    public Environment(final String... schemas) {
        this(parseTextSchemas(Arrays.asList(schemas)));
    }

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

    public Request request(final String graphQLRequest) {
        Parser parser = new Parser();
        final Document document = parser.parseDocument(graphQLRequest);
        return request(document);
    }

    public Request request(final Document document) {
        return new Request(document, this);
    }

    public Invoice invoice(final String document) {
        return request(document).invoice();
    }

    public Invoice invoice(final Document document) {
        return request(document).invoice();
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

    public static String getPaniqlSchema() {
        try {
            final String packagePath = Environment.class.getPackageName().replaceAll("\\.", "/");
            final String resourcePath = packagePath + "/PaniqlSchema.graphqls";
            try (
                    final InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
                    final Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                    final BufferedReader bufferedReader = new BufferedReader(reader);
            ) {
                return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException x) {
            throw new RuntimeException("Unexpected I/O error while reading Paniql schema.");
        }
    }

    private static TypeDefinitionRegistry parseTextSchemas(final Collection<String> schemas) {
        final String paniqlSchema = getPaniqlSchema();
        int schemaSize = paniqlSchema.length();
        for (final String schema: schemas) {
            schemaSize += SCHEMA_SEPARATOR.length() + schema.length();
        }
        final StringBuilder schemaBuilder = new StringBuilder(schemaSize);
        schemaBuilder.append(paniqlSchema);
        for (final String schema: schemas) {
            schemaBuilder.append(SCHEMA_SEPARATOR);
            schemaBuilder.append(schema);
        }
        SchemaParser parser = new SchemaParser();
        return parser.parse(schemaBuilder.toString());
    }

    private static TypeDefinitionRegistry parsePathSchemas(Collection<Path> schemaPaths) throws IOException {
        final String paniqlSchema = getPaniqlSchema();
        final StringBuilder schemaBuilder = new StringBuilder(65536);
        schemaBuilder.append(paniqlSchema);
        for (final Path path: schemaPaths) {
            schemaBuilder.append(SCHEMA_SEPARATOR);
            schemaBuilder.append(Files.readString(path));
        }
        SchemaParser parser = new SchemaParser();
        return parser.parse(schemaBuilder.toString());
    }

}
