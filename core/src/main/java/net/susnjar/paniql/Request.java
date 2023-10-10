package net.susnjar.paniql;

import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import net.susnjar.paniql.models.ObjectTypeModel;
import net.susnjar.paniql.pricing.Bounds;
import net.susnjar.paniql.pricing.Invoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Request {
    private final Document request;
    private final Environment environment;
    private final List<OperationDefinition> operations = new ArrayList<>();
    private final HashMap<String, List<FragmentDefinition>> fragments = new HashMap<>();

    public Request(final Document requestDocument, final Environment environment) {
        this.request = requestDocument;
        this.environment = environment;

        for (final Definition def: requestDocument.getDefinitions()) {
            if (def instanceof OperationDefinition) {
                addOperation((OperationDefinition) def);
            } else if (def instanceof FragmentDefinition) {
                addFragment((FragmentDefinition) def);
            }
        }
    }

    private void addFragment(FragmentDefinition fragment) {
        fragments.computeIfAbsent(fragment.getName(), n -> new ArrayList<>()).add(fragment);
    }

    private void addOperation(OperationDefinition operation) {
        operations.add(operation);
    }

    public List<FragmentDefinition> getFragment(String name) {
        return fragments.get(name);
    }

    public Invoice invoice() {
        final Invoice total = new Invoice();

        for (final OperationDefinition op: operations) {
            final ObjectTypeModel opType = getOperationType(op);

            total.add(opType.invoice(this, null, Bounds.ALWAYS_1, List.of(op.getSelectionSet())));
        }
        return total;
    }

    private ObjectTypeModel getOperationType(OperationDefinition operationDefinition) {
        final ObjectTypeModel opType;

        switch (operationDefinition.getOperation()) {
            case QUERY:
                opType = environment.getQueryType();
                break;

            case MUTATION:
                opType = environment.getMutationType();
                break;

            case SUBSCRIPTION:
                opType = environment.getSubscriptionType();
                break;

            default:
                throw new RuntimeException("Unrecognized operation: " + operationDefinition.getOperation());
        }
        return opType;
    }
}
