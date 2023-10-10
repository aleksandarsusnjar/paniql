package net.susnjar.paniql;

import com.google.common.io.CharStreams;
import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.junit.jupiter.api.DynamicTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;

public abstract class ResourceDrivenTest {
    private static final String[] BASE_PATH = ResourceDrivenTest.class.getPackageName().split("\\.");

    public String getResourcePath() {
        return getResourcePath(this);
    }

    public ResourceList getResourcesWithExtension(final String extension) throws IOException {
        return listResources(extension, getResourcePath());
    }

    public static final String getResourcePath(final Object obj) {
        return getResourcePath(obj.getClass());
    }

    public static final String getResourcePath(final Class<?> clazz) {
        return clazz.getPackageName().replaceAll("\\.", "/");
    }

    public static ResourceList listResources(final String extension, String... paths) throws IOException {
        final ClassGraph classGraph = new ClassGraph();
        try (ScanResult result = classGraph.acceptPaths(paths).scan()) {
            return result.getResourcesWithExtension(extension);
        }
    }

    public static InputStream openResourceAsStream(final Resource resource) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(resource.getPathRelativeToClasspathElement());
    }

    public static Reader openResourceAsReader(final Resource resource) {
        return new InputStreamReader(openResourceAsStream(resource), StandardCharsets.UTF_8);
    }

    public static String getResourceAsString(final Resource resource) throws IOException {
        try (final Reader reader = openResourceAsReader(resource)) {
            return CharStreams.toString(reader);
        }
    }

    private TypeDefinitionRegistry typeReg;
    private Environment environment;

    protected Collection<DynamicTest> discoverTests(final String extension) throws IOException {
        return getResourcesWithExtension(extension).stream()
                .sorted((a, b) -> a.getPath().compareTo(b.getPath()))
                .map(resource -> DynamicTest.dynamicTest(resource.getPath(), () -> {
                    try {
                        runTest(resource);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }))
                .collect(Collectors.toList());
    }

    public Document parseRequest(final Resource resource) throws IOException {
        Parser parser = new Parser();
        return parser.parseDocument(getResourceAsString(resource));
    }

    public Document parseRequest(final Path file) throws IOException {
        Parser parser = new Parser();
        return parser.parseDocument(new FileReader(file.toFile(), StandardCharsets.UTF_8));
    }

    public TypeDefinitionRegistry parseSchema(final Path file) {
        SchemaParser parser = new SchemaParser();
        return parser.parse(file.toFile());
    }

    public TypeDefinitionRegistry parseSchema(final InputStream inputStream) {
        SchemaParser parser = new SchemaParser();
        return parser.parse(inputStream);
    }

    public TypeDefinitionRegistry loadSchema() throws IOException {
        final StringBuilder schemaBuilder = new StringBuilder(16384);

        for (final Resource resource: getResourcesWithExtension("graphqls")) {
            if (schemaBuilder.length() > 0) {
                schemaBuilder.append("\n\n");
            }

            schemaBuilder.append(getResourceAsString(resource));
        }

        SchemaParser parser = new SchemaParser();
        return parser.parse(schemaBuilder.toString());
    }

    protected abstract void runTest(final Resource resource) throws IOException;
}
