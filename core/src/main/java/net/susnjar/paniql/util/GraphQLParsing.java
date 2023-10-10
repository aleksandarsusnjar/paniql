package net.susnjar.paniql.util;

import graphql.language.*;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GraphQLParsing {
    public static ObjectValue getObjectValue(final Argument argument, Supplier<ObjectValue> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof ObjectValue) {
                return (ObjectValue) value;
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static String getStringValue(final Argument argument, Supplier<String> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof StringValue) {
                return ((StringValue) value).getValue();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Double getFloatValue(final Argument argument, Supplier<Double> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof FloatValue) {
                return ((FloatValue) value).getValue().doubleValue();
            } else if (value instanceof IntValue) {
                return ((IntValue) value).getValue().doubleValue();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Integer getIntValue(final Argument argument, Supplier<Integer> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof IntValue) {
                return ((IntValue) value).getValue().intValueExact();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Boolean getBooleanValue(final Argument argument, Supplier<Boolean> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof BooleanValue) {
                return ((BooleanValue) value).isValue();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static String getEnumValue(final Argument argument, Supplier<String> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof EnumValue) {
                return ((EnumValue) value).getName();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static List<Value> getArrayValue(final Argument argument, Supplier<List<Value>> defaultValue) {
        if (argument != null) {
            final Value value = argument.getValue();
            if (value instanceof ArrayValue) {
                return ((ArrayValue) value).getValues();
            }
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static ObjectValue getObjectValue(final Value value) {
        return getObjectValue(value, null);
    }

    public static ObjectValue getObjectValue(final Value value, Supplier<ObjectValue> defaultValue) {
        if (value instanceof ObjectValue) {
            return (ObjectValue) value;
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static String getStringValue(final Value value) {
        return getStringValue(value, null);
    }

    public static String getStringValue(final Value value, Supplier<String> defaultValue) {
        if (value instanceof StringValue) {
            return ((StringValue) value).getValue();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Double getFloatValue(final Value value) {
        return getFloatValue(value, null);
    }

    public static Double getFloatValue(final Value value, Supplier<Double> defaultValue) {
        if (value instanceof FloatValue) {
            return ((FloatValue) value).getValue().doubleValue();
        } else if (value instanceof IntValue) {
            return ((IntValue) value).getValue().doubleValue();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Integer getIntValue(final Value value) {
        return getIntValue(value, null);
    }

    public static Integer getIntValue(final Value value, Supplier<Integer> defaultValue) {
        if (value instanceof IntValue) {
            return ((IntValue) value).getValue().intValueExact();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static Boolean getBooleanValue(final Value value, Supplier<Boolean> defaultValue) {
        if (value instanceof BooleanValue) {
            return ((BooleanValue) value).isValue();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static String getEnumValue(final Value value) {
        return getEnumValue(value, null);
    }

    public static String getEnumValue(final Value value, Supplier<String> defaultValue) {
        if (value instanceof EnumValue) {
            return ((EnumValue) value).getName();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static List<Value> getArrayValue(final Value value, Supplier<List<Value>> defaultValue) {
        if (value instanceof ArrayValue) {
            return ((ArrayValue) value).getValues();
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static <T> List<T> getArrayValue(final Argument argument, Supplier<List<T>> defaultValue, Function<Value, T> mapper) {
        List<Value> values = getArrayValue(argument, null);
        if (values != null) {
            return values.stream().map(mapper).collect(Collectors.toList());
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }

    public static <T> List<T> getArrayValue(final Value value, Supplier<List<T>> defaultValue, Function<Value, T> mapper) {
        List<Value> values = getArrayValue(value, null);
        if (values != null) {
            return values.stream().map(mapper).collect(Collectors.toList());
        }
        return (defaultValue == null) ? null : defaultValue.get();
    }
}
