package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectionOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void projectionReturnsRequestedColumns() {

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
                    new Tuple(
                            schema,
                            List.of(
                                    1,
                                    "Leopard Gecko",
                                    4
                            )
                    )
            );

            Operator scan =
                    new SeqScanOperator(
                            table
                    );

            Operator projection =
                    new ProjectionOperator(
                            scan,
                            List.of(
                                    "species",
                                    "age"
                            )
                    );

            projection.open();

            Tuple result =
                    projection.next();

            projection.close();

            assertEquals(
                    2,
                    result.getValueCount()
            );

            assertEquals(
                    "Leopard Gecko",
                    result.getValue(0)
            );

            assertEquals(
                    4,
                    result.getValue(1)
            );

            assertEquals(
                    "species",
                    projection
                            .getOutputSchema()
                            .getColumn(0)
                            .getName()
            );

            assertEquals(
                    "age",
                    projection
                            .getOutputSchema()
                            .getColumn(1)
                            .getName()
            );
        }
    }

    @Test
    void unknownProjectionColumnIsRejected() {

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

            Operator scan =
                    new SeqScanOperator(
                            table
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new ProjectionOperator(
                                    scan,
                                    List.of(
                                            "does_not_exist"
                                    )
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
}