package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HashIndexLookupOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void hashIndexLookupReturnsTuple() {

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

            HashIndex index =
                    new HashIndex(8);

            Tuple gecko =
                    new Tuple(
                            schema,
                            List.of(
                                    1,
                                    "Leopard Gecko"
                            )
                    );

            Tuple snake =
                    new Tuple(
                            schema,
                            List.of(
                                    2,
                                    "Corn Snake"
                            )
                    );

            RecordId geckoId =
                    table.insert(gecko);

            RecordId snakeId =
                    table.insert(snake);

            index.insert(
                    1,
                    geckoId
            );

            index.insert(
                    2,
                    snakeId
            );

            Operator lookup =
                    new HashIndexLookupOperator(
                            table,
                            index,
                            2
                    );

            lookup.open();

            Tuple result =
                    lookup.next();

            assertEquals(
                    "Corn Snake",
                    result.getValue(1)
            );

            assertNull(
                    lookup.next()
            );

            lookup.close();
        }
    }

    @Test
    void missingHashKeyReturnsNothing() {

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

            HashIndex index =
                    new HashIndex(8);

            Operator lookup =
                    new HashIndexLookupOperator(
                            table,
                            index,
                            999
                    );

            lookup.open();

            assertNull(
                    lookup.next()
            );

            lookup.close();
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
                        )
                )
        );
    }
}