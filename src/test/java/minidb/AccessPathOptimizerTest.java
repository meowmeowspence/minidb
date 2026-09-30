package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccessPathOptimizerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void smallTablePrefersSequentialScan() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "small.db"
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

            Tuple tuple =
                    tuple(
                            schema,
                            42,
                            "target"
                    );

            RecordId recordId =
                    table.insert(tuple);

            BPlusTree tree =
                    new BPlusTree(3);

            tree.insert(
                    42,
                    recordId
            );

            HashIndex hash =
                    new HashIndex(8);

            hash.insert(
                    42,
                    recordId
            );

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            AccessPathOptimizer optimizer =
                    new AccessPathOptimizer();

            OptimizedPlan plan =
                    optimizer
                            .chooseIntegerEqualityPlan(
                                    table,
                                    "id",
                                    42,
                                    stats,
                                    tree,
                                    hash
                            );

            assertEquals(
                    AccessPath.SEQUENTIAL_SCAN,
                    plan.getAccessPath()
            );

            Operator operator =
                    plan.getOperator();

            operator.open();

            Tuple result =
                    operator.next();

            assertEquals(
                    "target",
                    result.getValue(1)
            );

            assertNull(
                    operator.next()
            );

            operator.close();
        }
    }

    @Test
    void largeTableCanPreferBPlusTree() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "large-tree.db"
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

            BPlusTree tree =
                    new BPlusTree(3);

            String payload =
                    "x".repeat(500);

            for (
                    int i = 0;
                    i < 80;
                    i++
            ) {

                RecordId recordId =
                        table.insert(
                                tuple(
                                        schema,
                                        i,
                                        i == 42
                                                ? "target"
                                                : payload
                                )
                        );

                tree.insert(
                        i,
                        recordId
                );
            }

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            AccessPathOptimizer optimizer =
                    new AccessPathOptimizer();

            OptimizedPlan plan =
                    optimizer
                            .chooseIntegerEqualityPlan(
                                    table,
                                    "id",
                                    42,
                                    stats,
                                    tree,
                                    null
                            );

            assertEquals(
                    AccessPath.B_PLUS_TREE,
                    plan.getAccessPath()
            );

            assertTrue(
                    plan.getEstimatedCost()
                            < CostModel
                            .sequentialScanCost(
                                    stats
                            )
            );

            Operator operator =
                    plan.getOperator();

            operator.open();

            Tuple result =
                    operator.next();

            assertEquals(
                    42,
                    result.getValue(0)
            );

            operator.close();
        }
    }

    @Test
    void largeTableCanPreferHashIndex() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "large-hash.db"
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

            HashIndex hash =
                    new HashIndex(128);

            String payload =
                    "x".repeat(500);

            for (
                    int i = 0;
                    i < 80;
                    i++
            ) {

                RecordId recordId =
                        table.insert(
                                tuple(
                                        schema,
                                        i,
                                        i == 42
                                                ? "target"
                                                : payload
                                )
                        );

                hash.insert(
                        i,
                        recordId
                );
            }

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            AccessPathOptimizer optimizer =
                    new AccessPathOptimizer();

            OptimizedPlan plan =
                    optimizer
                            .chooseIntegerEqualityPlan(
                                    table,
                                    "id",
                                    42,
                                    stats,
                                    null,
                                    hash
                            );

            assertEquals(
                    AccessPath.HASH_INDEX,
                    plan.getAccessPath()
            );

            assertEquals(
                    2,
                    plan.getEstimatedCost()
            );

            Operator operator =
                    plan.getOperator();

            operator.open();

            Tuple result =
                    operator.next();

            assertEquals(
                    "target",
                    result.getValue(1)
            );

            operator.close();
        }
    }

    private Schema schema() {

        return new Schema(
                List.of(
                        new Column(
                                "id",
                                DataType.INTEGER
                        ),
                        new Column(
                                "payload",
                                DataType.STRING
                        )
                )
        );
    }

    private Tuple tuple(
            Schema schema,
            int id,
            String payload
    ) {

        return new Tuple(
                schema,
                List.of(
                        id,
                        payload
                )
        );
    }
}