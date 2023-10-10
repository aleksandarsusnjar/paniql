package net.susnjar.paniql.commandline;

import graphql.language.Document;
import graphql.parser.Parser;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import net.susnjar.paniql.Environment;
import net.susnjar.paniql.Request;
import net.susnjar.paniql.pricing.Invoice;
import net.susnjar.paniql.print.InvoicePrinter;

import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Paniql implements Runnable {
    private final Path schemaPath;
    private final Path requestPath;
    private final Environment environment;

    private Paniql(final String schemaArg, final String requestArg) throws IOException {
        schemaPath = Path.of(schemaArg);
        requestPath = Path.of(requestArg);

        final String paniqlSchema = Environment.getPaniqlSchema();
        final StringBuilder schemaBuilder = new StringBuilder(paniqlSchema.length() + 2 + (int)schemaPath.toFile().length());

        schemaBuilder.append(paniqlSchema);
        schemaBuilder.append(System.lineSeparator());
        schemaBuilder.append(Files.readString(schemaPath, StandardCharsets.UTF_8));

        SchemaParser parser = new SchemaParser();
        TypeDefinitionRegistry typeReg = parser.parse(schemaBuilder.toString());

        environment = new Environment(typeReg);
    }

    public static void main(final String... args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: <path-to-schema-file> <path-to-request-file>");
        } else {
            final Paniql instance = new Paniql(args[0], args[1]);
            instance.run();
        }
    }

    @Override
    public void run() {
        try {
            Parser parser = new Parser();
            final Document document = parser.parseDocument(new FileReader(requestPath.toFile(), StandardCharsets.UTF_8));
            final Request request = new Request(document, environment);

            final Invoice invoice = request.invoice();

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            PrintStream printStream = new PrintStream(buffer);

            final InvoicePrinter printer = new InvoicePrinter();
            printer.println(invoice, System.out);
            System.out.flush();
        } catch (IOException x) {
            throw new RuntimeException(x);
        }
    }
}