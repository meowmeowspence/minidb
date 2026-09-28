package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeapFileCursorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void cursorReadsEveryTupleInOrder() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            byte[] first = {
                    1,
                    2,
                    3
            };

            byte[] second = {
                    10,
                    20
            };

            byte[] third = {
                    50,
                    60,
                    70,
                    80
            };

            heap.insert(first);
            heap.insert(second);
            heap.insert(third);

            HeapFileCursor cursor =
                    heap.openCursor();

            assertArrayEquals(
                    first,
                    cursor.next()
            );

            assertArrayEquals(
                    second,
                    cursor.next()
            );

            assertArrayEquals(
                    third,
                    cursor.next()
            );

            assertNull(
                    cursor.next()
            );
        }
    }

    @Test
    void emptyHeapFileReturnsNoTuple() {

        Path path =
                tempDirectory.resolve(
                        "heap.db"
                );

        try (DiskManager disk =
                     new DiskManager(path)) {

            HeapFile heap =
                    new HeapFile(disk);

            HeapFileCursor cursor =
                    heap.openCursor();

            assertNull(
                    cursor.next()
            );
        }
    }
}