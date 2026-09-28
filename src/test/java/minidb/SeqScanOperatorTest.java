package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SeqScanOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void sequentialScanReturnsAllTuples() {

        Schema schema =
                animalSchema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            table.insert(
                    animal(
                            schema,
                            1,
                            "Leopard Gecko",
                            4
                    )
            );

            table.insert(
                    animal(
                            schema,
                            2,
                            "Corn Snake",
                            7
                    )
            );

            SeqScanOperator scan =
                    new SeqScanOperator(
                            table
                    );

            scan.open();

            Tuple first =
                    scan.next();

            Tuple second =
                    scan.next();

            Tuple third =
                    scan.next();

            scan.close();

            assertEquals(
                    "Leopard Gecko",
                    first.getValue(1)
            );

            assertEquals(
                    "Corn Snake",
                    second.getValue(1)
            );

            assertNull(third);
        }
    }

    @Test
    void scanMustBeOpenedBeforeUse() {

        Schema schema =
                animalSchema();

        Path path =
                tempDirectory.resolve(
                        "animals.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            SeqScanOperator scan =
                    new SeqScanOperator(
                            table
                    );

            assertThrows(
                    IllegalStateException.class,
                    scan::next
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

    private Tuple animal(
            Schema schema,
            int id,
            String species,
            int age
    ) {

        return new Tuple(
                schema,
                List.of(
                        id,
                        species,
                        age
                )
        );
    }
}