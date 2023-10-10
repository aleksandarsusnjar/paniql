package net.susnjar.paniql.print;

import graphql.language.Document;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.github.classgraph.Resource;
import net.susnjar.paniql.CoreResourceDrivenTest;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.Request;
import net.susnjar.paniql.pricing.Invoice;
import net.susnjar.paniql.print.InvoicePrinter;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;


public class InvoicePrinterTest extends CoreResourceDrivenTest {
    private TypeDefinitionRegistry typeReg;
    private Environment environment;

    @TestFactory
    Collection<DynamicTest> printerTests() throws IOException {
        return discoverTests("graphql");
    }

    @Override
    protected void runTest(final Resource resource) throws IOException {
        typeReg = loadSchema();
        environment = new Environment(typeReg);

        final Document document = parseRequest(resource);
        final Request request = new Request(document, environment);

        final Invoice invoice = request.invoice();

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(buffer);

        final InvoicePrinter printer = new InvoicePrinter();
        printer.println(invoice, printStream);
        String capturedOutput = buffer.toString();

        System.out.println(capturedOutput);
        System.out.flush();

        final Path reportPath = Path.of("build", "invoice-printer-outputs", resource.getPathRelativeToClasspathElement().replace(".graphql", ".txt"));
        reportPath.getParent().toFile().mkdirs();
        Files.writeString(reportPath, capturedOutput, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}