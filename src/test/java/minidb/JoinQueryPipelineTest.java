package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JoinQueryPipelineTest {

    @TempDir
    Path tempDirectory;

    @Test
    void hashJoinCanFeedProjection() {

        Schema animalSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "animal_id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "species",
                                        DataType.STRING
                                )
                        )
                );

        Schema observationSchema =
                new Schema(
                        List.of(
                                new Column(
                                        "observation_id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "animal_ref_id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "location",
                                        DataType.STRING
                                )
                        )
                );

        Path animalPath =
                tempDirectory.resolve(
                        "animals.db"
                );

        Path observationPath =
                tempDirectory.resolve(
                        "observations.db"
                );

        try (
                DiskManager animalDisk =
                        new DiskManager(
                                animalPath
                        );

                DiskManager observationDisk =
                        new DiskManager(
                                observationPath
                        )
        ) {

            Table animals =
                    new Table(
                            animalSchema,
                            new HeapFile(
                                    animalDisk
                            )
                    );

            Table observations =
                    new Table(
                            observationSchema,
                            new HeapFile(
                                    observationDisk
                            )
                    );

            animals.insert(
                    new Tuple(
                            animalSchema,
                            List.of(
                                    1,
                                    "Leopard Gecko"
                            )
                    )
            );

            animals.insert(
                    new Tuple(
                            animalSchema,
                            List.of(
                                    2,
                                    "Corn Snake"
                            )
                    )
            );

            observations.insert(
                    new Tuple(
                            observationSchema,
                            List.of(
                                    101,
                                    2,
                                    "Toronto"
                            )
                    )
            );

            observations.insert(
                    new Tuple(
                            observationSchema,
                            List.of(
                                    102,
                                    1,
                                    "Vaughan"
                            )
                    )
            );

            Operator plan =
                    new ProjectionOperator(
                            new HashJoinOperator(
                                    new SeqScanOperator(
                                            animals
                                    ),
                                    new SeqScanOperator(
                                            observations
                                    ),
                                    "animal_id",
                                    "animal_ref_id"
                            ),
                            List.of(
                                    "species",
                                    "location"
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
                    "Leopard Gecko",
                    first.getValue(0)
            );

            assertEquals(
                    "Vaughan",
                    first.getValue(1)
            );

            assertEquals(
                    "Corn Snake",
                    second.getValue(0)
            );

            assertEquals(
                    "Toronto",
                    second.getValue(1)
            );

            assertNull(third);

            assertEquals(
                    2,
                    plan
                            .getOutputSchema()
                            .getColumnCount()
            );
        }
    }
}