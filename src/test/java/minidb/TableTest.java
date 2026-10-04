package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableTest {

    @TempDir
    Path tempDirectory;

    @Test
    void tupleCanBeInsertedAndReadThroughTable() {

        Schema schema =
                animalSchema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            Table table =
                    new Table(
                            schema,
                            heap
                    );

            Tuple tuple =
                    new Tuple(
                            schema,
                            List.of(
                                    1,
                                    "Leopard Gecko",
                                    4
                            )
                    );

            RecordId recordId =
                    table.insert(tuple);

            Tuple restored =
                    table.read(recordId);

            assertEquals(
                    1,
                    restored.getValue(0)
            );

            assertEquals(
                    "Leopard Gecko",
                    restored.getValue(1)
            );

            assertEquals(
                    4,
                    restored.getValue(2)
            );
        }
    }

    @Test
    void tableRejectsTupleWithWrongSchema() {

        Schema tableSchema =
                animalSchema();

        Schema wrongSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "name",
                                        DataType.STRING
                                )
                        )
                );

        Path path =
                tempDirectory.resolve(
                        "animals.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            Table table =
                    new Table(
                            tableSchema,
                            new HeapFile(disk)
                    );

            Tuple wrongTuple =
                    new Tuple(
                            wrongSchema,
                            List.of("Gecko")
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> table.insert(
                            wrongTuple
                    )
            );
        }
    }

    private Schema animalSchema() {

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

    @Test
    void tupleCanBeUpdatedThroughTable() {

        Schema schema =
                animalSchema();

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

            RecordId recordId =
                    table.insert(
                            new Tuple(
                                    schema,
                                    List.of(
                                            1,
                                            "Gecko",
                                            4
                                    )
                            )
                    );

            table.update(
                    recordId,
                    new Tuple(
                            schema,
                            List.of(
                                    1,
                                    "Leopard Gecko",
                                    5
                            )
                    )
            );

            Tuple updated =
                    table.read(
                            recordId
                    );

            assertEquals(
                    "Leopard Gecko",
                    updated.getValue(1)
            );

            assertEquals(
                    5,
                    updated.getValue(2)
            );
        }
    }
}