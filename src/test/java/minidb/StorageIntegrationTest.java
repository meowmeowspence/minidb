package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StorageIntegrationTest {

    @TempDir
    Path tempDirectory;

    @Test
    void tupleCanRoundTripThroughHeapFile() {

        Schema schema = new Schema(
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

        Tuple original =
                new Tuple(
                        schema,
                        List.of(
                                1,
                                "Leopard Gecko",
                                4
                        )
                );

        TupleSerializer serializer =
                new TupleSerializer();

        byte[] tupleBytes =
                serializer.serialize(
                        original
                );

        Path path =
                tempDirectory.resolve(
                        "animals.db"
                );

        RecordId recordId;

        /*
         * First database session:
         * write the tuple.
         */
        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            recordId =
                    heap.insert(
                            tupleBytes
                    );
        }

        /*
         * Second database session:
         * reopen the file and read it.
         */
        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            byte[] restoredBytes =
                    heap.read(recordId);

            Tuple restored =
                    serializer.deserialize(
                            schema,
                            restoredBytes
                    );

            assertEquals(
                    1,
                    restored.getValue(0)
            );

            assertEquals(
                    "Leopard Gecko",
                    restored.getValue(1)
            );

            assertEquals(
                    4,
                    restored.getValue(2)
            );
        }
    }
}