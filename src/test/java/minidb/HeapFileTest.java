package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeapFileTest {

    @TempDir
    Path tempDirectory;

    @Test
    void tupleCanBeInsertedAndRead() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            byte[] tuple = {
                    1,
                    2,
                    3,
                    4,
                    5
            };

            RecordId recordId =
                    heap.insert(tuple);

            byte[] restored =
                    heap.read(recordId);

            assertArrayEquals(
                    tuple,
                    restored
            );
        }
    }

    @Test
    void smallTuplesShareTheSamePage() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            RecordId first =
                    heap.insert(
                            new byte[100]
                    );

            RecordId second =
                    heap.insert(
                            new byte[100]
                    );

            assertEquals(
                    first.getPageId(),
                    second.getPageId()
            );

            assertNotEquals(
                    first.getSlotId(),
                    second.getSlotId()
            );

            assertEquals(
                    1,
                    disk.getPageCount()
            );
        }
    }

    @Test
    void additionalPageIsAllocatedWhenNeeded() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            /*
             * Each record consumes:
             *
             * 2000 bytes of tuple data
             * + 8 bytes for its slot.
             *
             * Two fit on one page,
             * but three do not.
             */
            RecordId first =
                    heap.insert(
                            new byte[2000]
                    );

            RecordId second =
                    heap.insert(
                            new byte[2000]
                    );

            RecordId third =
                    heap.insert(
                            new byte[2000]
                    );

            assertEquals(
                    0,
                    first.getPageId()
            );

            assertEquals(
                    0,
                    second.getPageId()
            );

            assertEquals(
                    1,
                    third.getPageId()
            );

            assertEquals(
                    2,
                    disk.getPageCount()
            );
        }
    }

    @Test
    void oversizedTupleIsRejected() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            byte[] hugeTuple =
                    new byte[5000];

            assertThrows(
                    IllegalArgumentException.class,
                    () -> heap.insert(hugeTuple)
            );
        }
    }

    @Test
    void recordCanBeUpdatedWithoutChangingRecordId() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (
                DiskManager disk =
                        new DiskManager(path)
        ) {

            HeapFile heap =
                    new HeapFile(disk);

            RecordId recordId =
                    heap.insert(
                            new byte[] {
                                    1,
                                    2,
                                    3
                            }
                    );

            byte[] replacement = {
                    10,
                    20,
                    30,
                    40,
                    50
            };

            heap.update(
                    recordId,
                    replacement
            );

            assertArrayEquals(
                    replacement,
                    heap.read(recordId)
            );

            /*
             * The original RID remains valid.
             */
            assertEquals(
                    0,
                    recordId.getPageId()
            );

            assertEquals(
                    0,
                    recordId.getSlotId()
            );
        }
    }

}