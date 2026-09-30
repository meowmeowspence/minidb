package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CostModelTest {

    @TempDir
    Path tempDirectory;

    @Test
    void sequentialScanCostUsesPageCount() {

        Schema schema =
                new Schema(
                        List.of(
                                new Column(
                                        "id",
                                        DataType.INTEGER
                                ),
                                new Column(
                                        "payload",
                                        DataType.STRING
                                )
                        )
                );

        Path path =
                tempDirectory.resolve(
                        "table.db"
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

            String payload =
                    "x".repeat(500);

            for (
                    int i = 0;
                    i < 30;
                    i++
            ) {

                table.insert(
                        new Tuple(
                                schema,
                                List.of(
                                        i,
                                        payload
                                )
                        )
                );
            }

            TableStatistics stats =
                    TableStatistics.analyze(
                            table
                    );

            assertEquals(
                    stats.getPageCount(),
                    CostModel
                            .sequentialScanCost(
                                    stats
                            )
            );

            assertTrue(
                    stats.getPageCount() > 1
            );
        }
    }

    @Test
    void indexCostsCanBeEstimated() {

        BPlusTree tree =
                new BPlusTree(3);

        for (
                int i = 0;
                i < 20;
                i++
        ) {

            tree.insert(
                    i,
                    new RecordId(
                            i,
                            0
                    )
            );
        }

        assertEquals(
                tree.getHeight() + 1L,
                CostModel
                        .bPlusTreeEqualityCost(
                                tree
                        )
        );

        assertEquals(
                2,
                CostModel.hashEqualityCost()
        );
    }
}