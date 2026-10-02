package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinOptimizerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void tinyJoinPrefersNestedLoop() {

        JoinTestContext context =
                createTables(
                        "tiny",
                        1,
                        1
                );

        try (context) {

            JoinOptimizer optimizer =
                    new JoinOptimizer(
                            100,
                            4
                    );

            OptimizedJoinPlan plan =
                    optimizer
                            .chooseEqualityJoinPlan(
                                    context.leftTable,
                                    context.rightTable,
                                    "left_id",
                                    "right_ref_id",
                                    TableStatistics.analyze(
                                            context.leftTable
                                    ),
                                    TableStatistics.analyze(
                                            context.rightTable
                                    )
                            );

            assertEquals(
                    JoinAlgorithm.NESTED_LOOP,
                    plan.getAlgorithm()
            );

            /*
             * Actually execute the selected
             * physical plan too.
             */
            Operator operator =
                    plan.getOperator();

            operator.open();

            Tuple result =
                    operator.next();

            assertNotNull(result);

            assertEquals(
                    0,
                    result.getValue(0)
            );

            assertNull(
                    operator.next()
            );

            operator.close();
        }
    }

    @Test
    void mediumJoinPrefersHashJoin() {

        JoinTestContext context =
                createTables(
                        "medium",
                        10,
                        10
                );

        try (context) {

            JoinOptimizer optimizer =
                    new JoinOptimizer(
                            100,
                            4
                    );

            TableStatistics leftStats =
                    TableStatistics.analyze(
                            context.leftTable
                    );

            TableStatistics rightStats =
                    TableStatistics.analyze(
                            context.rightTable
                    );

            OptimizedJoinPlan plan =
                    optimizer
                            .chooseEqualityJoinPlan(
                                    context.leftTable,
                                    context.rightTable,
                                    "left_id",
                                    "right_ref_id",
                                    leftStats,
                                    rightStats
                            );

            assertEquals(
                    JoinAlgorithm.HASH_JOIN,
                    plan.getAlgorithm()
            );

            assertEquals(
                    30,
                    plan.getEstimatedCost()
            );

            Operator operator =
                    plan.getOperator();

            operator.open();

            int resultCount = 0;

            while (
                    operator.next()
                            != null
            ) {

                resultCount++;
            }

            operator.close();

            assertEquals(
                    10,
                    resultCount
            );
        }
    }

    @Test
    void largeJoinUsesSortMergeWhenHashBuildIsTooLarge() {

        JoinTestContext context =
                createTables(
                        "large",
                        40,
                        40
                );

        try (context) {

            /*
             * Right side has 40 tuples,
             * but our pretend hash-build
             * memory limit is only 10.
             */
            JoinOptimizer optimizer =
                    new JoinOptimizer(
                            10,
                            10
                    );

            TableStatistics leftStats =
                    TableStatistics.analyze(
                            context.leftTable
                    );

            TableStatistics rightStats =
                    TableStatistics.analyze(
                            context.rightTable
                    );

            OptimizedJoinPlan plan =
                    optimizer
                            .chooseEqualityJoinPlan(
                                    context.leftTable,
                                    context.rightTable,
                                    "left_id",
                                    "right_ref_id",
                                    leftStats,
                                    rightStats
                            );

            assertEquals(
                    JoinAlgorithm.SORT_MERGE,
                    plan.getAlgorithm()
            );

            assertTrue(
                    plan.getEstimatedCost()
                            < CostModel
                            .nestedLoopJoinCost(
                                    leftStats,
                                    rightStats
                            )
            );

            Operator operator =
                    plan.getOperator();

            operator.open();

            int resultCount = 0;

            while (
                    operator.next()
                            != null
            ) {

                resultCount++;
            }

            operator.close();

            assertEquals(
                    40,
                    resultCount
            );
        }
    }

    private JoinTestContext createTables(
            String prefix,
            int leftRows,
            int rightRows
    ) {

        Schema leftSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "left_id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "left_value",
                                        DataType.STRING
                                )
                        )
                );

        Schema rightSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "right_ref_id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "right_value",
                                        DataType.STRING
                                )
                        )
                );

        DiskManager leftDisk =
                new DiskManager(
                        tempDirectory.resolve(
                                prefix
                                        + "-left.db"
                        )
                );

        DiskManager rightDisk =
                new DiskManager(
                        tempDirectory.resolve(
                                prefix
                                        + "-right.db"
                        )
                );

        Table leftTable =
                new Table(
                        leftSchema,
                        new HeapFile(
                                leftDisk
                        )
                );

        Table rightTable =
                new Table(
                        rightSchema,
                        new HeapFile(
                                rightDisk
                        )
                );

        for (
                int i = 0;
                i < leftRows;
                i++
        ) {

            leftTable.insert(
                    new Tuple(
                            leftSchema,
                            List.of(
                                    i,
                                    "left-" + i
                            )
                    )
            );
        }

        for (
                int i = 0;
                i < rightRows;
                i++
        ) {

            rightTable.insert(
                    new Tuple(
                            rightSchema,
                            List.of(
                                    i,
                                    "right-" + i
                            )
                    )
            );
        }

        return new JoinTestContext(
                leftDisk,
                rightDisk,
                leftTable,
                rightTable
        );
    }

    private static final class JoinTestContext
            implements AutoCloseable {

        private final DiskManager leftDisk;
        private final DiskManager rightDisk;

        private final Table leftTable;
        private final Table rightTable;

        private JoinTestContext(
                DiskManager leftDisk,
                DiskManager rightDisk,
                Table leftTable,
                Table rightTable
        ) {

            this.leftDisk = leftDisk;
            this.rightDisk = rightDisk;

            this.leftTable = leftTable;
            this.rightTable = rightTable;
        }

        @Override
        public void close() {

            leftDisk.close();
            rightDisk.close();
        }
    }
}