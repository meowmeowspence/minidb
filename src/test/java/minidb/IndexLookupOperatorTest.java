package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexLookupOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void indexLookupReturnsMatchingTuple() {

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

            BPlusTree index =
                    new BPlusTree(3);

            Tuple first =
                    animal(
                            schema,
                            1,
                            "Leopard Gecko",
                            4
                    );

            Tuple second =
                    animal(
                            schema,
                            2,
                            "Corn Snake",
                            7
                    );

            Tuple third =
                    animal(
                            schema,
                            3,
                            "Bearded Dragon",
                            10
                    );

            RecordId firstId =
                    table.insert(first);

            RecordId secondId =
                    table.insert(second);

            RecordId thirdId =
                    table.insert(third);

            index.insert(
                    1,
                    firstId
            );

            index.insert(
                    2,
                    secondId
            );

            index.insert(
                    3,
                    thirdId
            );

            Operator lookup =
                    new IndexLookupOperator(
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
    void missingIndexKeyReturnsNoTuple() {

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

            BPlusTree index =
                    new BPlusTree(3);

            Operator lookup =
                    new IndexLookupOperator(
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