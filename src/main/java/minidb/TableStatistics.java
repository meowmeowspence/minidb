package minidb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TableStatistics {

    private final long rowCount;
    private final int pageCount;

    private final Map<String, Integer>
            distinctCounts;

    private TableStatistics(
            long rowCount,
            int pageCount,
            Map<String, Integer> distinctCounts
    ) {

        this.rowCount = rowCount;
        this.pageCount = pageCount;

        this.distinctCounts =
                Map.copyOf(
                        distinctCounts
                );
    }

    public static TableStatistics analyze(
            Table table
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        Schema schema =
                table.getSchema();

        List<Set<Object>> distinctValues =
                new ArrayList<>();

        for (
                int i = 0;
                i < schema.getColumnCount();
                i++
        ) {

            distinctValues.add(
                    new HashSet<>()
            );
        }

        long rowCount = 0;

        HeapFileCursor cursor =
                table.openCursor();

        byte[] bytes;

        while (
                (bytes = cursor.next())
                        != null
        ) {

            Tuple tuple =
                    table.deserialize(
                            bytes
                    );

            rowCount++;

            for (
                    int i = 0;
                    i < schema.getColumnCount();
                    i++
            ) {

                distinctValues
                        .get(i)
                        .add(
                                tuple.getValue(i)
                        );
            }
        }

        Map<String, Integer> counts =
                new HashMap<>();

        for (
                int i = 0;
                i < schema.getColumnCount();
                i++
        ) {

            counts.put(
                    schema
                            .getColumn(i)
                            .getName(),
                    distinctValues
                            .get(i)
                            .size()
            );
        }

        return new TableStatistics(
                rowCount,
                table.getPageCount(),
                counts
        );
    }

    public long getRowCount() {
        return rowCount;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getDistinctCount(
            String columnName
    ) {

        Integer count =
                distinctCounts.get(
                        columnName
                );

        if (count == null) {
            throw new IllegalArgumentException(
                    "Unknown column: "
                            + columnName
            );
        }

        return count;
    }

    public long estimateEqualityRowCount(
            String columnName
    ) {

        if (rowCount == 0) {
            return 0;
        }

        int distinct =
                getDistinctCount(
                        columnName
                );

        if (distinct == 0) {
            return 0;
        }

        return Math.max(
                1,
                (long) Math.ceil(
                        (double) rowCount
                                / distinct
                )
        );
    }
}