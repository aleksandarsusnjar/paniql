package net.susnjar.paniql.commandline;

import com.google.common.io.CharStreams;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.classgraph.Resource;
import net.susnjar.paniql.CoreResourceDrivenTest;
import net.susnjar.paniql.Environment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;


public class PaniqlMainTest extends CoreResourceDrivenTest {
    private TypeDefinitionRegistry typeReg;
    private Environment environment;

    @TestFactory
    Collection<DynamicTest> printerTests() throws IOException {
        return discoverTests("graphql");
    }

    @Override
    protected void runTest(final Resource resource) throws IOException {
        String name = resource.getPathRelativeToClasspathElement();
        final int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) name = name.substring(lastSlash + 1);

        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        final String schema;
        try (final InputStream stream = loader.getResourceAsStream("net/susnjar/paniql/TestSchema.graphqls")) {
            schema = CharStreams.toString(
                new InputStreamReader(
                    stream,
                    StandardCharsets.UTF_8
                )
            );
        }

        final String request = getResourceAsString(resource);

        File tempSchema = File.createTempFile(name, ".graphqls");
        File tempRequest = File.createTempFile(name, ".graphql");

        try {
            Files.writeString(tempSchema.toPath(), schema, StandardCharsets.UTF_8);
            Files.writeString(tempRequest.toPath(), request, StandardCharsets.UTF_8);

            try {
                Paniql.main(tempSchema.getAbsolutePath(), tempRequest.getAbsolutePath());
            } catch (Exception x) {
                Assertions.fail(x);
            }
        } finally {
            deleteTempFile(tempSchema);
            deleteTempFile(tempRequest);
        }
    }

    private void deleteTempFile(File tempSchema) {
        try {
            tempSchema.delete();
        } catch (Exception x) {
            tempSchema.deleteOnExit();
        }
    }
}