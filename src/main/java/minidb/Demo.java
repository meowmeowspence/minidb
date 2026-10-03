package minidb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Demo {

    public static void main(String[] args)
            throws IOException {

        Path tempDirectory =
                Files.createTempDirectory(
                        "minidb-demo-"
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
                                            "animal_ref_id",
                                            DataType.INTEGER
                                    ),
                                    new Column(
                                            "location",
                                            DataType.STRING
                                    )
                            )
                    );

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

            insertAnimal(
                    animals,
                    animalSchema,
                    1,
                    "Leopard Gecko"
            );

            insertAnimal(
                    animals,
                    animalSchema,
                    2,
                    "Corn Snake"
            );

            insertAnimal(
                    animals,
                    animalSchema,
                    3,
                    "Ball Python"
            );

            insertObservation(
                    observations,
                    observationSchema,
                    1,
                    "Vaughan"
            );

            insertObservation(
                    observations,
                    observationSchema,
                    2,
                    "Toronto"
            );

            insertObservation(
                    observations,
                    observationSchema,
                    2,
                    "Markham"
            );

            TableStatistics animalStats =
                    TableStatistics.analyze(
                            animals
                    );

            TableStatistics observationStats =
                    TableStatistics.analyze(
                            observations
                    );

            JoinOptimizer optimizer =
                    new JoinOptimizer(
                            100,
                            2
                    );

            OptimizedJoinPlan optimized =
                    optimizer
                            .chooseEqualityJoinPlan(
                                    animals,
                                    observations,
                                    "animal_id",
                                    "animal_ref_id",
                                    animalStats,
                                    observationStats
                            );

            System.out.println(
                    "MiniDB Demo"
            );

            System.out.println(
                    "-----------"
            );

            System.out.println(
                    "Animals: "
                            + animalStats.getRowCount()
            );

            System.out.println(
                    "Observations: "
                            + observationStats.getRowCount()
            );

            System.out.println();

            System.out.println(
                    "Optimizer selected: "
                            + optimized.getAlgorithm()
            );

            System.out.println(
                    "Estimated cost: "
                            + optimized.getEstimatedCost()
            );

            System.out.println(
                    "Estimated result rows: "
                            + optimized.getEstimatedRowCount()
            );

            System.out.println();

            System.out.println(
                    "Query results:"
            );

            Operator plan =
                    new ProjectionOperator(
                            optimized.getOperator(),
                            List.of(
                                    "species",
                                    "location"
                            )
                    );

            plan.open();

            Tuple tuple;

            while (
                    (tuple = plan.next())
                            != null
            ) {

                System.out.println(
                        tuple.getValue(0)
                                + " | "
                                + tuple.getValue(1)
                );
            }

            plan.close();
        }

        deleteDirectory(
                tempDirectory
        );
    }

    private static void insertAnimal(
            Table table,
            Schema schema,
            int id,
            String species
    ) {

        table.insert(
                new Tuple(
                        schema,
                        List.of(
                                id,
                                species
                        )
                )
        );
    }

    private static void insertObservation(
            Table table,
            Schema schema,
            int animalId,
            String location
    ) {

        table.insert(
                new Tuple(
                        schema,
                        List.of(
                                animalId,
                                location
                        )
                )
        );
    }

    private static void deleteDirectory(
            Path directory
    ) throws IOException {

        try (
                var paths =
                        Files.walk(
                                directory
                        )
        ) {

            for (
                    Path path
                    : paths
                    .sorted(
                            java.util.Comparator
                                    .reverseOrder()
                    )
                    .toList()
            ) {

                Files.deleteIfExists(
                        path
                );
            }
        }
    }
}