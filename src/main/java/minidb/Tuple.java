package minidb;

import java.util.List;

public class Tuple {

    private final Schema schema;
    private final List<Object> values;

    public Tuple(Schema schema, List<Object> values) {

        if (schema == null) {
            throw new IllegalArgumentException("Schema cannot be null");
        }

        if (values == null) {
            throw new IllegalArgumentException("Values cannot be null");
        }

        if (schema.getColumnCount() != values.size()) {
            throw new IllegalArgumentException(
                    "Tuple value count must match schema column count"
            );
        }

        for (int i = 0; i < values.size(); i++) {
            validateValue(
                    schema.getColumn(i).getType(),
                    values.get(i)
            );
        }

        this.schema = schema;
        this.values = List.copyOf(values);
    }

    private void validateValue(DataType type, Object value) {
        // check value against the corresponding MiniDB DataType
        if (value == null) {
            throw new IllegalArgumentException(
                    "Null values are not supported yet"
            );
        }

        boolean valid = switch (type) {
            case INTEGER -> value instanceof Integer;
            case LONG -> value instanceof Long;
            case BOOLEAN -> value instanceof Boolean;
            case STRING -> value instanceof String;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Value " + value + " does not match type " + type
            );
        }
    }

    public Schema getSchema() {
        return schema;
    }

    public Object getValue(int index) {
        return values.get(index);
    }

    public int getValueCount() {
        return values.size();
    }

}
