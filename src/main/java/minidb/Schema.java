package minidb;

import java.util.List;

public class Schema {

    private final List<Column> columns;

    public Schema(List<Column> columns) {

        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Schema must contain at least one column"
            );
        }

        this.columns = List.copyOf(columns);
    }

    public int getColumnCount() {
        return columns.size();
    }

    public Column getColumn(int index) {
        return columns.get(index);
    }

    public int indexOf(String columnName) {

        for (int i = 0; i < columns.size(); i++) {

            if (columns.get(i).getName().equals(columnName)) {
                return i;
            }

        }

        return -1;
    }

}