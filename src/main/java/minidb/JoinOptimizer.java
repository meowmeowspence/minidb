package minidb;

public class JoinOptimizer {

    private final int hashBuildTupleLimit;

    private final int sortRunCapacity;

    public JoinOptimizer(
            int hashBuildTupleLimit,
            int sortRunCapacity
    ) {

        if (hashBuildTupleLimit < 1) {
            throw new IllegalArgumentException(
                    "Hash build limit must be at least 1"
            );
        }

        if (sortRunCapacity < 1) {
            throw new IllegalArgumentException(
                    "Sort run capacity must be at least 1"
            );
        }

        this.hashBuildTupleLimit =
                hashBuildTupleLimit;

        this.sortRunCapacity =
                sortRunCapacity;
    }

    public OptimizedJoinPlan chooseEqualityJoinPlan(
            Table leftTable,
            Table rightTable,
            String leftColumnName,
            String rightColumnName,
            TableStatistics leftStatistics,
            TableStatistics rightStatistics
    ) {

        validateArguments(
                leftTable,
                rightTable,
                leftStatistics,
                rightStatistics
        );

        Schema leftSchema =
                leftTable.getSchema();

        Schema rightSchema =
                rightTable.getSchema();

        int leftColumnIndex =
                leftSchema.indexOf(
                        leftColumnName
                );

        int rightColumnIndex =
                rightSchema.indexOf(
                        rightColumnName
                );

        if (leftColumnIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown left join column: "
                            + leftColumnName
            );
        }

        if (rightColumnIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown right join column: "
                            + rightColumnName
            );
        }

        DataType leftType =
                leftSchema
                        .getColumn(
                                leftColumnIndex
                        )
                        .getType();

        DataType rightType =
                rightSchema
                        .getColumn(
                                rightColumnIndex
                        )
                        .getType();

        if (leftType != rightType) {
            throw new IllegalArgumentException(
                    "Join columns must have matching types"
            );
        }

        long estimatedRows =
                CostModel
                        .estimateEqualityJoinRowCount(
                                leftStatistics,
                                leftColumnName,
                                rightStatistics,
                                rightColumnName
                        );

        /*
         * Candidate 1:
         *
         * Nested-loop join.
         */
        long bestCost =
                CostModel
                        .nestedLoopJoinCost(
                                leftStatistics,
                                rightStatistics
                        );

        JoinAlgorithm bestAlgorithm =
                JoinAlgorithm.NESTED_LOOP;

        Operator bestOperator =
                createNestedLoop(
                        leftTable,
                        rightTable,
                        leftColumnIndex,
                        rightColumnIndex
                );

        /*
         * Candidate 2:
         *
         * In-memory hash join.
         *
         * Our current implementation builds
         * a hash table from the RIGHT input.
         *
         * Therefore, only consider it when
         * that input is within our configured
         * memory limit.
         */
        if (
                rightStatistics.getRowCount()
                        <= hashBuildTupleLimit
        ) {

            long hashCost =
                    CostModel.hashJoinCost(
                            leftStatistics,
                            rightStatistics
                    );

            if (hashCost < bestCost) {

                bestCost =
                        hashCost;

                bestAlgorithm =
                        JoinAlgorithm.HASH_JOIN;

                bestOperator =
                        new HashJoinOperator(
                                new SeqScanOperator(
                                        leftTable
                                ),
                                new SeqScanOperator(
                                        rightTable
                                ),
                                leftColumnName,
                                rightColumnName
                        );
            }
        }

        /*
         * Candidate 3:
         *
         * External sort-merge join.
         */
        long sortMergeCost =
                CostModel
                        .sortMergeJoinCost(
                                leftStatistics,
                                rightStatistics,
                                sortRunCapacity
                        );

        if (sortMergeCost < bestCost) {

            bestCost =
                    sortMergeCost;

            bestAlgorithm =
                    JoinAlgorithm.SORT_MERGE;

            bestOperator =
                    new SortMergeJoinOperator(
                            new SeqScanOperator(
                                    leftTable
                            ),
                            new SeqScanOperator(
                                    rightTable
                            ),
                            leftColumnName,
                            rightColumnName,
                            sortRunCapacity
                    );
        }

        return new OptimizedJoinPlan(
                bestOperator,
                bestAlgorithm,
                bestCost,
                estimatedRows
        );
    }

    private Operator createNestedLoop(
            Table leftTable,
            Table rightTable,
            int leftColumnIndex,
            int rightColumnIndex
    ) {

        return new NestedLoopJoinOperator(
                new SeqScanOperator(
                        leftTable
                ),
                new SeqScanOperator(
                        rightTable
                ),
                (leftTuple, rightTuple) ->
                        leftTuple
                                .getValue(
                                        leftColumnIndex
                                )
                                .equals(
                                        rightTuple
                                                .getValue(
                                                        rightColumnIndex
                                                )
                                )
        );
    }

    private void validateArguments(
            Table leftTable,
            Table rightTable,
            TableStatistics leftStatistics,
            TableStatistics rightStatistics
    ) {

        if (
                leftTable == null
                        || rightTable == null
        ) {

            throw new IllegalArgumentException(
                    "Tables cannot be null"
            );
        }

        if (
                leftStatistics == null
                        || rightStatistics == null
        ) {

            throw new IllegalArgumentException(
                    "Statistics cannot be null"
            );
        }
    }
}