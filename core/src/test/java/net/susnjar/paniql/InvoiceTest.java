package net.susnjar.paniql;

import graphql.language.Document;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.classgraph.Resource;
import net.susnjar.paniql.pricing.Invoice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Collection;

public class InvoiceTest extends ResourceDrivenTest {
    private TypeDefinitionRegistry typeReg;
    private Environment environment;

    @TestFactory
    Collection<DynamicTest> invoicingTests() throws IOException {
        return discoverTests("graphql");
    }

    @Override
    protected void runTest(final Resource resource) throws IOException {
        typeReg = loadSchema();
        environment = new Environment(typeReg);

        final Request request = environment.request(getResourceAsString(resource));
        final Invoice invoice = request.invoice();

        Assertions.assertNotNull(invoice);
    }
}
