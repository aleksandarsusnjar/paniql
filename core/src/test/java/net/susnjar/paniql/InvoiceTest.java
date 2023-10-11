package net.susnjar.paniql;

import graphql.language.Document;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.classgraph.Resource;
import net.susnjar.paniql.models.OutputTypeModel;
import net.susnjar.paniql.pricing.Invoice;
import net.susnjar.paniql.pricing.Price;
import net.susnjar.paniql.pricing.WorkType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

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

        final Map<OutputTypeModel, Price> resourceCosts = invoice.getResourceCosts();
        for (final Map.Entry<OutputTypeModel, Price> entry: resourceCosts.entrySet()) {
            final String resourceTypeName = entry.getKey().getFullyQualifiedName();
            final Price price = entry.getValue();
            System.out.println(resourceTypeName + ":");
            for (final WorkType workType: WorkType.values()) {
                System.out.println("  - " + workType.getHeading() + ": " + price.get(workType).getMaximum());
            }
        }

        Assertions.assertNotNull(invoice);
    }
}
