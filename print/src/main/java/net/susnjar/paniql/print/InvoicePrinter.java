package net.susnjar.paniql.print;

import net.susnjar.paniql.models.ElementModel;
import net.susnjar.paniql.pricing.Bounds;
import net.susnjar.paniql.pricing.Invoice;
import net.susnjar.paniql.pricing.Price;
import net.susnjar.paniql.pricing.WorkType;

import java.io.PrintStream;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class InvoicePrinter {

    private int nameWidth = 10;
    private int columnWidth = 14;

    private int lineWidth;
    private String doubleHorizontalRule;
    private String singleHorizontalRule;

    public InvoicePrinter() {
        recalculate();
    }

    public void setNameWidth(final int width) {
        nameWidth = width;
        recalculate();
    }

    public void setValueColumnWidth(final int width) {
        columnWidth = width;
        recalculate();
    }

    private void recalculate() {
        lineWidth = nameWidth + 1 + 3 + WorkType.values().length * columnWidth;
        doubleHorizontalRule = "=".repeat(lineWidth);
        singleHorizontalRule = "-".repeat(lineWidth);
    }

    public void println(final Invoice invoice) {
        println(invoice, System.out);
    }

    public void println(final Invoice invoice, final PrintStream out) {
        Price total = Price.FREE;

        nameWidth = Math.max(
            nameWidth,
            1 + invoice.getResourceCosts().keySet().stream().map(t -> t.getFullyQualifiedName().length()).max(Integer::compareTo).orElse(0)
        );
        nameWidth = Math.max(
                nameWidth,
                1 + invoice.getPartCosts().keySet().stream().map(t -> t.getFullyQualifiedName().length()).max(Integer::compareTo).orElse(0)
        );
        nameWidth = Math.max(
                nameWidth,
                1 + invoice.getFieldCosts().keySet().stream().map(t -> t.getFullyQualifiedName().length()).max(Integer::compareTo).orElse(0)
        );

        recalculate();

        total = total.plus(println(out, "Resource", invoice.getResourceCosts()));
        out.println();
        total = total.plus(println(out, "Part", invoice.getPartCosts()));
        out.println();
        total = total.plus(println(out, "Field", invoice.getFieldCosts()));

        out.println();
        out.println(doubleHorizontalRule);

        printTableHeader(out, "Grand Total".toUpperCase(Locale.ROOT));
        printRow(out, "", total, true);
        out.println();

        out.println("LEGEND:");
        final int headingWidth = WorkType.getMaxHeadingLength() + 2;

        for (final WorkType workType: WorkType.values()) {
            out.println(" - " + pad(workType.getHeading() + ":", headingWidth) + workType.getDescription());
        }
        out.println(" - " + pad("Min:", headingWidth) + "Minimum quantity in normal conditions.");
        out.println(" - " + pad("Avg:", headingWidth) + "Average quantity in normal conditions.");
        out.println(" - " + pad("95%:", headingWidth) + "95% percentile, >= 95% of expected values.");
        out.println(" - " + pad("Max:", headingWidth) + "Maximum, accounting for built-in constraints.");
    }

    private <T extends ElementModel> Price println(final PrintStream out, final String title, final Map<T, Price> items) {
        final TreeMap<T, Price> sorted = new TreeMap<>((a, b) -> a.getFullyQualifiedName().compareTo(b.getFullyQualifiedName()));
        sorted.putAll(items);

        Price subtotal = Price.FREE;

        out.println(doubleHorizontalRule);

        printTableHeader(out, pad(title.toUpperCase(Locale.ROOT), nameWidth));
        if (items.isEmpty()) {
            out.println("(none)");
            return subtotal;
        }

        for (final Map.Entry<T, Price> entry: sorted.entrySet()) {
            final Price q = entry.getValue();
            subtotal = subtotal.plus(q);
            printRow(out,  entry.getKey().getFullyQualifiedName(), q, items.size() == 1);
        }
        if (items.size() > 1) {
            printRow(out, "TOTAL", subtotal, true);
        }
        return subtotal;
    }

    private void printTableHeader(PrintStream out, String column1Heading) {
        out.print(pad(column1Heading, nameWidth));

        out.print("    ");

        for (final WorkType workType: WorkType.values()) {
            out.print(padLeft(workType.getHeading(), columnWidth));
        }

        out.println();
        out.println(singleHorizontalRule);
    }

    private <T extends ElementModel> void printRow(PrintStream out, String name, Price price, boolean bottomRow) {
        out.print(pad(name, nameWidth));
        out.print(" Min");
        printSubRow(out, price, percentiles -> percentiles.getMinimum());
        out.print(pad("", nameWidth));
        out.print(" Avg");
        printSubRow(out, price, percentiles -> percentiles.getAverage());
        out.print(pad("", nameWidth));
        out.print(" 95%");
        printSubRow(out, price, percentiles -> percentiles.getPercentile95());
        out.print(pad("", nameWidth));
        out.print(" Max");
        printSubRow(out, price, percentiles -> percentiles.getMaximum());
        out.println(bottomRow ? doubleHorizontalRule : singleHorizontalRule);
    }

    private void printSubRow(final PrintStream out, Price price, final Function<Bounds, Double> boundGetter) {
        final Function<Bounds, String> x = (p) ->
                padLeft(p.isAlwaysZero() ? "" : boundGetter.apply(p), columnWidth);

        for (final WorkType workType: WorkType.values()) {
            out.print(x.apply(price.get(workType)));
        }

        out.println();
    }

    final String pad(final Object value, final int width) {
        String valueText = getValueText(value);
        return valueText + " ".repeat(Math.max(0, width - valueText.length()));
    }

    private String getValueText(Object value) {
        if (value instanceof Number) {
            NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);
            numberFormat.setGroupingUsed(true);
            numberFormat.setMaximumFractionDigits(0);
            numberFormat.setRoundingMode(RoundingMode.HALF_UP);
            return numberFormat.format(value);
        } else {
            return String.valueOf(value);
        }
    }

    final String padLeft(final Object value, final int width) {
        String valueText = getValueText(value);
        return " ".repeat(Math.max(0, width - valueText.length())) + valueText;
    }
}
