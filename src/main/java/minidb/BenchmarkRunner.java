package minidb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class BenchmarkRunner {

    private static final int ROW_COUNT = 2_000;

    private static final int LOOKUP_KEY =
            ROW_COUNT - 1;

    private static final int ITERATIONS = 100;

    public static void main(String[] args)
            throws IOException {

        Path tempDirectory =
                Files.createTempDirectory(
                        "minidb-benchmark-"
                );

        Path databasePath =
                tempDirectory.resolve(
                        "benchmark.db"
                );

        try (
                DiskManager disk =
                        new DiskManager(
                                databasePath
                        )
        ) {

            Schema schema =
                    new Schema(
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

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            BPlusTree bPlusTree =
                    new BPlusTree(32);

            HashIndex hashIndex =
                    new HashIndex(256);

            System.out.println(
                    "Creating benchmark table..."
            );

            for (
                    int i = 0;
                    i < ROW_COUNT;
                    i++
            ) {

                Tuple tuple =
                        new Tuple(
                                schema,
                                List.of(
                                        i,
                                        "value-" + i
                                )
                        );

                RecordId recordId =
                        table.insert(
                                tuple
                        );

                bPlusTree.insert(
                        i,
                        recordId
                );

                hashIndex.insert(
                        i,
                        recordId
                );
            }

            System.out.println(
                    "Rows: "
                            + ROW_COUNT
            );

            System.out.println(
                    "Pages: "
                            + table.getPageCount()
            );

            System.out.println(
                    "Lookup key: "
                            + LOOKUP_KEY
            );

            /*
             * Warm up the JVM before timing.
             */
            for (
                    int i = 0;
                    i < 20;
                    i++
            ) {

                runSequentialLookup(
                        table,
                        LOOKUP_KEY
                );

                runBPlusTreeLookup(
                        table,
                        bPlusTree,
                        LOOKUP_KEY
                );

                runHashLookup(
                        table,
                        hashIndex,
                        LOOKUP_KEY
                );
            }

            long sequentialTime =
                    benchmark(
                            () ->
                                    runSequentialLookup(
                                            table,
                                            LOOKUP_KEY
                                    )
                    );

            long treeTime =
                    benchmark(
                            () ->
                                    runBPlusTreeLookup(
                                            table,
                                            bPlusTree,
                                            LOOKUP_KEY
                                    )
                    );

            long hashTime =
                    benchmark(
                            () ->
                                    runHashLookup(
                                            table,
                                            hashIndex,
                                            LOOKUP_KEY
                                    )
                    );

            System.out.println();

            System.out.println(
                    "Average lookup time"
            );

            System.out.println(
                    "-------------------"
            );

            printResult(
                    "Sequential scan",
                    sequentialTime
            );

            printResult(
                    "B+ tree",
                    treeTime
            );

            printResult(
                    "Hash index",
                    hashTime
            );

            System.out.println();

            System.out.println(
                    "Note: indexes are currently memory-resident."
            );

            System.out.println(
                    "Heap records remain disk-backed."
            );
        }

        deleteDirectory(
                tempDirectory
        );
    }

    private static long benchmark(
            Runnable operation
    ) {

        long start =
                System.nanoTime();

        for (
                int i = 0;
                i < ITERATIONS;
                i++
        ) {

            operation.run();
        }

        long elapsed =
                System.nanoTime()
                        - start;

        return elapsed
                / ITERATIONS;
    }

    private static void runSequentialLookup(
            Table table,
            int key
    ) {

        int idIndex =
                table
                        .getSchema()
                        .indexOf("id");

        Operator operator =
                new FilterOperator(
                        new SeqScanOperator(
                                table
                        ),
                        tuple ->
                                ((Integer)
                                        tuple.getValue(
                                                idIndex
                                        ))
                                        == key
                );

        consumeExactlyOne(
                operator
        );
    }

    private static void runBPlusTreeLookup(
            Table table,
            BPlusTree index,
            int key
    ) {

        Operator operator =
                new IndexLookupOperator(
                        table,
                        index,
                        key
                );

        consumeExactlyOne(
                operator
        );
    }

    private static void runHashLookup(
            Table table,
            HashIndex index,
            int key
    ) {

        Operator operator =
                new HashIndexLookupOperator(
                        table,
                        index,
                        key
                );

        consumeExactlyOne(
                operator
        );
    }

    private static void consumeExactlyOne(
            Operator operator
    ) {

        operator.open();

        Tuple tuple =
                operator.next();

        operator.close();

        if (tuple == null) {
            throw new IllegalStateException(
                    "Benchmark lookup returned no tuple"
            );
        }
    }

    private static void printResult(
            String name,
            long nanoseconds
    ) {

        double microseconds =
                nanoseconds
                        / 1_000.0;

        System.out.printf(
                "%-18s %.2f microseconds%n",
                name + ":",
                microseconds
        );
    }

    private static void deleteDirectory(
            Path directory
    ) throws IOException {

        try (
                var paths =
                        Files.walk(
                                directory
                        )
        ) {

            for (
                    Path path
                    : paths
                    .sorted(
                            java.util.Comparator
                                    .reverseOrder()
                    )
                    .toList()
            ) {

                Files.deleteIfExists(
                        path
                );
            }
        }
    }
}