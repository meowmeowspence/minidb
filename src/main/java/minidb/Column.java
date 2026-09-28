package minidb;

public class Column {

    private final String name;
    private final DataType type;

    public Column(String name, DataType type) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Column name cannot be empty");
        }

        if (type == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }

        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public DataType getType() {
        return type;
    }
}