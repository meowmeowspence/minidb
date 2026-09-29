package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SortMergeJoinOperatorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void sortMergeJoinReturnsMatchingTuples() {

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

            /*
             * Deliberately insert out of
             * join-key order.
             */
            animals.insert(
                    animal(
                            animalSchema,
                            2,
                            "Corn Snake"
                    )
            );

            animals.insert(
                    animal(
                            animalSchema,
                            1,
                            "Leopard Gecko"
                    )
            );

            animals.insert(
                    animal(
                            animalSchema,
                            3,
                            "Ball Python"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            103,
                            2,
                            "Markham"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            102,
                            1,
                            "Vaughan"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            101,
                            2,
                            "Toronto"
                    )
            );

            Operator join =
                    new SortMergeJoinOperator(
                            new SeqScanOperator(
                                    animals
                            ),
                            new SeqScanOperator(
                                    observations
                            ),
                            "animal_id",
                            "animal_ref_id",
                            1
                    );

            join.open();

            List<String> results =
                    new ArrayList<>();

            Tuple tuple;

            while (
                    (tuple = join.next())
                            != null
            ) {

                results.add(
                        tuple.getValue(1)
                                + "|"
                                + tuple.getValue(4)
                );
            }

            join.close();

            /*
             * Ignore ordering between equal
             * join keys for this assertion.
             */
            Collections.sort(results);

            assertEquals(
                    List.of(
                            "Corn Snake|Markham",
                            "Corn Snake|Toronto",
                            "Leopard Gecko|Vaughan"
                    ),
                    results
            );
        }
    }

    @Test
    void duplicateKeysProduceCartesianProduct() {

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
                    animal(
                            animalSchema,
                            2,
                            "Corn Snake A"
                    )
            );

            animals.insert(
                    animal(
                            animalSchema,
                            2,
                            "Corn Snake B"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            101,
                            2,
                            "Toronto"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            102,
                            2,
                            "Markham"
                    )
            );

            Operator join =
                    new SortMergeJoinOperator(
                            new SeqScanOperator(
                                    animals
                            ),
                            new SeqScanOperator(
                                    observations
                            ),
                            "animal_id",
                            "animal_ref_id",
                            1
                    );

            join.open();

            int resultCount = 0;

            while (
                    join.next()
                            != null
            ) {

                resultCount++;
            }

            join.close();

            assertEquals(
                    4,
                    resultCount
            );
        }
    }

    @Test
    void joinWithNoMatchingKeysReturnsNothing() {

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
                    animal(
                            animalSchema,
                            1,
                            "Leopard Gecko"
                    )
            );

            observations.insert(
                    observation(
                            observationSchema,
                            100,
                            99,
                            "Toronto"
                    )
            );

            Operator join =
                    new SortMergeJoinOperator(
                            new SeqScanOperator(
                                    animals
                            ),
                            new SeqScanOperator(
                                    observations
                            ),
                            "animal_id",
                            "animal_ref_id",
                            1
                    );

            join.open();

            assertNull(
                    join.next()
            );

            join.close();
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

    private Tuple animal(
            Schema schema,
            int id,
            String species
    ) {

        return new Tuple(
                schema,
                List.of(
                        id,
                        species
                )
        );
    }

    private Tuple observation(
            Schema schema,
            int observationId,
            int animalId,
            String location
    ) {

        return new Tuple(
                schema,
                List.of(
                        observationId,
                        animalId,
                        location
                )
        );
    }
}