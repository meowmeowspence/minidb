package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IndexedQueryPipelineTest {

    @TempDir
    Path tempDirectory;

    @Test
    void indexedLookupCanFeedProjection() {

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

            BPlusTree index =
                    new BPlusTree(3);

            Tuple gecko =
                    new Tuple(
                            schema,
                            List.of(
                                    1,
                                    "Leopard Gecko",
                                    4
                            )
                    );

            Tuple snake =
                    new Tuple(
                            schema,
                            List.of(
                                    2,
                                    "Corn Snake",
                                    7
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

            Operator plan =
                    new ProjectionOperator(
                            new IndexLookupOperator(
                                    table,
                                    index,
                                    2
                            ),
                            List.of(
                                    "species"
                            )
                    );

            plan.open();

            Tuple result =
                    plan.next();

            assertEquals(
                    "Corn Snake",
                    result.getValue(0)
            );

            assertNull(
                    plan.next()
            );

            plan.close();
        }
    }
}