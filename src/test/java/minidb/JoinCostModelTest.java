package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinCostModelTest {

    @TempDir
    Path tempDirectory;

    @Test
    void nestedLoopCostUsesCartesianComparisonCount() {

        TableStatistics left =
                statistics(
                        "left.db",
                        10
                );

        TableStatistics right =
                statistics(
                        "right.db",
                        20
                );

        assertEquals(
                200,
                CostModel.nestedLoopJoinCost(
                        left,
                        right
                )
        );
    }

    @Test
    void hashJoinUsesBuildAndProbeEstimate() {

        TableStatistics left =
                statistics(
                        "left.db",
                        10
                );

        TableStatistics right =
                statistics(
                        "right.db",
                        20
                );

        assertEquals(
                40,
                CostModel.hashJoinCost(
                        left,
                        right
                )
        );
    }

    @Test
    void externalSortCostReflectsMergePasses() {

        TableStatistics statistics =
                statistics(
                        "sort.db",
                        10
                );

        /*
         * Capacity 4 gives 3 initial runs.
         *
         * 3 -> 2 -> 1
         *
         * Two merge passes.
         *
         * Run generation:
         * 2 * 10 = 20
         *
         * Merge pass 1:
         * 20
         *
         * Merge pass 2:
         * 20
         *
         * Total = 60.
         */
        assertEquals(
                60,
                CostModel.externalSortCost(
                        statistics,
                        4
                )
        );
    }

    @Test
    void equalityJoinCardinalityCanBeEstimated() {

        Schema leftSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "left_id",
                                        DataType.INTEGER
                                )
                        )
                );

        Schema rightSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "right_id",
                                        DataType.INTEGER
                                )
                        )
                );

        Path leftPath =
                tempDirectory.resolve(
                        "card-left.db"
                );

        Path rightPath =
                tempDirectory.resolve(
                        "card-right.db"
                );

        try (
                DiskManager leftDisk =
                        new DiskManager(
                                leftPath
                        );

                DiskManager rightDisk =
                        new DiskManager(
                                rightPath
                        )
        ) {

            Table left =
                    new Table(
                            leftSchema,
                            new HeapFile(
                                    leftDisk
                            )
                    );

            Table right =
                    new Table(
                            rightSchema,
                            new HeapFile(
                                    rightDisk
                            )
                    );

            /*
             * Left:
             *
             * 4 rows
             * 2 distinct values.
             */
            left.insert(
                    new Tuple(
                            leftSchema,
                            List.of(1)
                    )
            );

            left.insert(
                    new Tuple(
                            leftSchema,
                            List.of(1)
                    )
            );

            left.insert(
                    new Tuple(
                            leftSchema,
                            List.of(2)
                    )
            );

            left.insert(
                    new Tuple(
                            leftSchema,
                            List.of(2)
                    )
            );

            /*
             * Right:
             *
             * 6 rows
             * 3 distinct values.
             */
            for (int value
                    : List.of(
                    1,
                    1,
                    2,
                    2,
                    3,
                    3
            )) {

                right.insert(
                        new Tuple(
                                rightSchema,
                                List.of(value)
                        )
                );
            }

            TableStatistics leftStats =
                    TableStatistics.analyze(
                            left
                    );

            TableStatistics rightStats =
                    TableStatistics.analyze(
                            right
                    );

            /*
             * Estimate:
             *
             * 4 * 6
             * -----
             * max(2, 3)
             *
             * =
             *
             * 24 / 3
             *
             * =
             *
             * 8
             */
            assertEquals(
                    8,
                    CostModel
                            .estimateEqualityJoinRowCount(
                                    leftStats,
                                    "left_id",
                                    rightStats,
                                    "right_id"
                            )
            );
        }
    }

    private TableStatistics statistics(
            String filename,
            int rows
    ) {

        Schema schema =
                new Schema(
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INTEGER
                                )
                        )
                );

        Path path =
                tempDirectory.resolve(
                        filename
                );

        try (
                DiskManager disk =
                        new DiskManager(path)
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            for (
                    int i = 0;
                    i < rows;
                    i++
            ) {

                table.insert(
                        new Tuple(
                                schema,
                                List.of(i)
                        )
                );
            }

            return TableStatistics.analyze(
                    table
            );
        }
    }
}