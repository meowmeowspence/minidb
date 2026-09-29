package minidb;

import java.util.ArrayList;
import java.util.List;

final class JoinUtils {

    private JoinUtils() {
    }

    static Schema combineSchemas(
            Schema left,
            Schema right
    ) {

        List<Column> columns =
                new ArrayList<>();

        for (
                int i = 0;
                i < left.getColumnCount();
                i++
        ) {

            columns.add(
                    left.getColumn(i)
            );
        }

        for (
                int i = 0;
                i < right.getColumnCount();
                i++
        ) {

            columns.add(
                    right.getColumn(i)
            );
        }

        return new Schema(columns);
    }

    static Tuple combineTuples(
            Tuple left,
            Tuple right,
            Schema outputSchema
    ) {

        List<Object> values =
                new ArrayList<>();

        for (
                int i = 0;
                i < left.getValueCount();
                i++
        ) {

            values.add(
                    left.getValue(i)
            );
        }

        for (
                int i = 0;
                i < right.getValueCount();
                i++
        ) {

            values.add(
                    right.getValue(i)
            );
        }

        return new Tuple(
                outputSchema,
                values
        );
    }
}