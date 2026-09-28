package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryPipelineTest {

    @TempDir
    Path tempDirectory;

    @Test
    void filterAndProjectionCanBeCombined() {

        Schema schema =
                new Schema(
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

            table.insert(
                    animal(
                            schema,
                            4,
                            "Ball Python",
                            3
                    )
            );

            int ageIndex =
                    schema.indexOf("age");

            Operator plan =
                    new ProjectionOperator(
                            new FilterOperator(
                                    new SeqScanOperator(
                                            table
                                    ),
                                    tuple ->
                                            (Integer)
                                                    tuple.getValue(
                                                            ageIndex
                                                    )
                                                    > 5
                            ),
                            List.of(
                                    "species"
                            )
                    );

            plan.open();

            Tuple first =
                    plan.next();

            Tuple second =
                    plan.next();

            Tuple third =
                    plan.next();

            plan.close();

            assertEquals(
                    "Corn Snake",
                    first.getValue(0)
            );

            assertEquals(
                    "Bearded Dragon",
                    second.getValue(0)
            );

            assertNull(third);

            assertEquals(
                    1,
                    plan
                            .getOutputSchema()
                            .getColumnCount()
            );
        }
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