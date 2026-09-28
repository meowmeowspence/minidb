package minidb;

import java.util.ArrayList;
import java.util.List;

public class ProjectionOperator
        implements Operator {

    private final Operator child;

    private final List<Integer>
            columnIndexes;

    private final Schema outputSchema;

    public ProjectionOperator(
            Operator child,
            List<String> columnNames
    ) {

        if (child == null) {
            throw new IllegalArgumentException(
                    "Child operator cannot be null"
            );
        }

        if (columnNames == null
                || columnNames.isEmpty()) {

            throw new IllegalArgumentException(
                    "Projection must contain at least one column"
            );
        }

        this.child = child;

        Schema inputSchema =
                child.getOutputSchema();

        List<Integer> indexes =
                new ArrayList<>();

        List<Column> outputColumns =
                new ArrayList<>();

        for (String columnName
                : columnNames) {

            int index =
                    inputSchema.indexOf(
                            columnName
                    );

            if (index < 0) {
                throw new IllegalArgumentException(
                        "Unknown column: "
                                + columnName
                );
            }

            indexes.add(index);

            outputColumns.add(
                    inputSchema.getColumn(
                            index
                    )
            );
        }

        this.columnIndexes =
                List.copyOf(indexes);

        this.outputSchema =
                new Schema(
                        outputColumns
                );
    }

    @Override
    public void open() {
        child.open();
    }

    @Override
    public Tuple next() {

        Tuple inputTuple =
                child.next();

        if (inputTuple == null) {
            return null;
        }

        List<Object> outputValues =
                new ArrayList<>();

        for (int columnIndex
                : columnIndexes) {

            outputValues.add(
                    inputTuple.getValue(
                            columnIndex
                    )
            );
        }

        return new Tuple(
                outputSchema,
                outputValues
        );
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

    @Override
    public void close() {
        child.close();
    }
}