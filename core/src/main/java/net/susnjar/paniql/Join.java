package net.susnjar.paniql;

import net.susnjar.paniql.models.FieldDefModel;

import java.util.HashMap;
import java.util.Map;

public class Join {
    private final HashMap<FieldDefModel, Join> fieldJoins = new HashMap<>();

    public void incorporate(final Join other) {
        for (final Map.Entry<FieldDefModel, Join> entry: other.fieldJoins.entrySet()) {
            fieldJoins.merge(entry.getKey(), entry.getValue(), this::mergeJoins);
        }
    }

    private Join mergeJoins(final Join a, final Join b) {
        a.incorporate(b);
        return a;
    }

    public void joinField(final FieldDefModel field, final Join join) {
        fieldJoins.put(field, join);
    }

    public Join getFieldJoin(final FieldDefModel field) {
        return fieldJoins.get(field);
    }
}
