package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilterOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void filterReturnsOnlyMatchingTuples() {

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

            table.insert(
                    animal(
                            schema,
                            3,
                            "Bearded Dragon",
                            10
                    )
            );

            int ageIndex =
                    schema.indexOf("age");

            Operator scan =
                    new SeqScanOperator(
                            table
                    );

            Operator filter =
                    new FilterOperator(
                            scan,
                            tuple ->
                                    (Integer)
                                            tuple.getValue(
                                                    ageIndex
                                            )
                                            > 5
                    );

            filter.open();

            Tuple first =
                    filter.next();

            Tuple second =
                    filter.next();

            Tuple third =
                    filter.next();

            filter.close();

            assertEquals(
                    "Corn Snake",
                    first.getValue(1)
            );

            assertEquals(
                    "Bearded Dragon",
                    second.getValue(1)
            );

            assertNull(third);
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