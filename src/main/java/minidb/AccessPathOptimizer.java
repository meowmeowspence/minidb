package minidb;

public class AccessPathOptimizer {

    public OptimizedPlan chooseIntegerEqualityPlan(
            Table table,
            String columnName,
            int key,
            TableStatistics statistics,
            BPlusTree bPlusTree,
            HashIndex hashIndex
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        if (statistics == null) {
            throw new IllegalArgumentException(
                    "Statistics cannot be null"
            );
        }

        Schema schema =
                table.getSchema();

        int columnIndex =
                schema.indexOf(
                        columnName
                );

        if (columnIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown column: "
                            + columnName
            );
        }

        if (
                schema
                        .getColumn(columnIndex)
                        .getType()
                        != DataType.INTEGER
        ) {

            throw new IllegalArgumentException(
                    "This optimizer currently supports integer equality predicates only"
            );
        }

        /*
         * PLAN A:
         *
         * Filter
         *   ↓
         * Sequential Scan
         */
        long bestCost =
                CostModel.sequentialScanCost(
                        statistics
                );

        AccessPath bestAccessPath =
                AccessPath.SEQUENTIAL_SCAN;

        Operator bestOperator =
                new FilterOperator(
                        new SeqScanOperator(
                                table
                        ),
                        tuple ->
                                ((Integer)
                                        tuple.getValue(
                                                columnIndex
                                        ))
                                        == key
                );

        /*
         * PLAN B:
         *
         * B+ tree exact lookup.
         */
        if (bPlusTree != null) {

            long treeCost =
                    CostModel
                            .bPlusTreeEqualityCost(
                                    bPlusTree
                            );

            if (treeCost < bestCost) {

                bestCost =
                        treeCost;

                bestAccessPath =
                        AccessPath.B_PLUS_TREE;

                bestOperator =
                        new IndexLookupOperator(
                                table,
                                bPlusTree,
                                key
                        );
            }
        }

        /*
         * PLAN C:
         *
         * Hash-index exact lookup.
         */
        if (hashIndex != null) {

            long hashCost =
                    CostModel
                            .hashEqualityCost();

            if (hashCost < bestCost) {

                bestCost =
                        hashCost;

                bestAccessPath =
                        AccessPath.HASH_INDEX;

                bestOperator =
                        new HashIndexLookupOperator(
                                table,
                                hashIndex,
                                key
                        );
            }
        }

        return new OptimizedPlan(
                bestOperator,
                bestAccessPath,
                bestCost
        );
    }
}