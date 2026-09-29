package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HashJoinOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void hashJoinReturnsMatchingTuples() {

        Schema animalSchema =
                animalSchema();

        Schema observationSchema =
                observationSchema();

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

            Operator join =
                    new HashJoinOperator(
                            new SeqScanOperator(
                                    animals
                            ),
                            new SeqScanOperator(
                                    observations
                            ),
                            "animal_id",
                            "animal_ref_id"
                    );

            join.open();

            Tuple first =
                    join.next();

            Tuple second =
                    join.next();

            Tuple third =
                    join.next();

            join.close();

            assertEquals(
                    "Leopard Gecko",
                    first.getValue(1)
            );

            assertEquals(
                    "Vaughan",
                    first.getValue(4)
            );

            assertEquals(
                    "Corn Snake",
                    second.getValue(1)
            );

            assertEquals(
                    "Toronto",
                    second.getValue(4)
            );

            assertNull(third);
        }
    }

    @Test
    void hashJoinHandlesMultipleMatches() {

        Schema animalSchema =
                animalSchema();

        Schema observationSchema =
                observationSchema();

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
                                    2,
                                    "Markham"
                            )
                    )
            );

            Operator join =
                    new HashJoinOperator(
                            new SeqScanOperator(
                                    animals
                            ),
                            new SeqScanOperator(
                                    observations
                            ),
                            "animal_id",
                            "animal_ref_id"
                    );

            join.open();

            Tuple first =
                    join.next();

            Tuple second =
                    join.next();

            Tuple third =
                    join.next();

            join.close();

            assertEquals(
                    "Toronto",
                    first.getValue(4)
            );

            assertEquals(
                    "Markham",
                    second.getValue(4)
            );

            assertNull(third);
        }
    }

    @Test
    void unknownJoinColumnIsRejected() {

        Schema animalSchema =
                animalSchema();

        Schema observationSchema =
                observationSchema();

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

            Operator left =
                    new SeqScanOperator(
                            animals
                    );

            Operator right =
                    new SeqScanOperator(
                            observations
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () ->
                            new HashJoinOperator(
                                    left,
                                    right,
                                    "does_not_exist",
                                    "animal_ref_id"
                            )
            );
        }
    }

    private Schema animalSchema() {

        return new Schema(
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
    }

    private Schema observationSchema() {

        return new Schema(
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
    }
}