package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableStatisticsTest {

    @TempDir
    Path tempDirectory;

    @Test
    void statisticsCountRowsPagesAndDistinctValues() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
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

            insert(
                    table,
                    schema,
                    1,
                    "Gecko",
                    4
            );

            insert(
                    table,
                    schema,
                    2,
                    "Snake",
                    4
            );

            insert(
                    table,
                    schema,
                    3,
                    "Dragon",
                    7
            );

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            assertEquals(
                    3,
                    stats.getRowCount()
            );

            assertEquals(
                    1,
                    stats.getPageCount()
            );

            assertEquals(
                    3,
                    stats.getDistinctCount(
                            "id"
                    )
            );

            assertEquals(
                    2,
                    stats.getDistinctCount(
                            "age"
                    )
            );
        }
    }

    @Test
    void equalityCardinalityCanBeEstimated() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
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

            insert(
                    table,
                    schema,
                    1,
                    "Gecko",
                    4
            );

            insert(
                    table,
                    schema,
                    2,
                    "Snake",
                    4
            );

            insert(
                    table,
                    schema,
                    3,
                    "Dragon",
                    7
            );

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            /*
             * 3 rows / 2 distinct ages
             * = 1.5
             * rounded upward = 2.
             */
            assertEquals(
                    2,
                    stats.estimateEqualityRowCount(
                            "age"
                    )
            );
        }
    }

    @Test
    void unknownStatisticsColumnIsRejected() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
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

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            stats.getDistinctCount(
                                    "missing"
                            )
            );
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
                                "species",
                                DataType.STRING
                        ),
                        new Column(
                                "age",
                                DataType.INTEGER
                        )
                )
        );
    }

    private void insert(
            Table table,
            Schema schema,
            int id,
            String species,
            int age
    ) {

        table.insert(
                new Tuple(
                        schema,
                        List.of(
                                id,
                                species,
                                age
                        )
                )
        );
    }
}