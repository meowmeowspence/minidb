package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExternalSortOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void tuplesAreSortedAcrossMultipleRuns() {

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
                    5,
                    "Five"
            );

            insert(
                    table,
                    schema,
                    1,
                    "One"
            );

            insert(
                    table,
                    schema,
                    4,
                    "Four"
            );

            insert(
                    table,
                    schema,
                    2,
                    "Two"
            );

            insert(
                    table,
                    schema,
                    3,
                    "Three"
            );

            /*
             * Capacity 2 guarantees that
             * several disk runs are created.
             */
            Operator sort =
                    new ExternalSortOperator(
                            new SeqScanOperator(
                                    table
                            ),
                            "id",
                            2
                    );

            sort.open();

            List<Integer> ids =
                    new ArrayList<>();

            Tuple tuple;

            while (
                    (tuple = sort.next())
                            != null
            ) {

                ids.add(
                        (Integer)
                                tuple.getValue(0)
                );
            }

            sort.close();

            assertEquals(
                    List.of(
                            1,
                            2,
                            3,
                            4,
                            5
                    ),
                    ids
            );
        }
    }

    @Test
    void duplicateSortKeysArePreserved() {

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
                    3,
                    "First Three"
            );

            insert(
                    table,
                    schema,
                    1,
                    "One"
            );

            insert(
                    table,
                    schema,
                    3,
                    "Second Three"
            );

            insert(
                    table,
                    schema,
                    2,
                    "Two"
            );

            Operator sort =
                    new ExternalSortOperator(
                            new SeqScanOperator(
                                    table
                            ),
                            "id",
                            1
                    );

            sort.open();

            List<Integer> ids =
                    new ArrayList<>();

            Tuple tuple;

            while (
                    (tuple = sort.next())
                            != null
            ) {

                ids.add(
                        (Integer)
                                tuple.getValue(0)
                );
            }

            sort.close();

            assertEquals(
                    List.of(
                            1,
                            2,
                            3,
                            3
                    ),
                    ids
            );
        }
    }

    @Test
    void unknownSortColumnIsRejected() {

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

            Operator scan =
                    new SeqScanOperator(
                            table
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new ExternalSortOperator(
                                    scan,
                                    "missing_column",
                                    2
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
                                "name",
                                DataType.STRING
                        )
                )
        );
    }

    private void insert(
            Table table,
            Schema schema,
            int id,
            String name
    ) {

        table.insert(
                new Tuple(
                        schema,
                        List.of(
                                id,
                                name
                        )
                )
        );
    }
}