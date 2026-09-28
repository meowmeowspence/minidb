package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DiskManagerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void newDatabaseStartsWithNoPages() {

        Path databasePath =
                tempDirectory.resolve(
                        "test.db"
                );

        try (DiskManager disk =
                     new DiskManager(databasePath)) {

            assertEquals(
                    0,
                    disk.getPageCount()
            );
        }
    }

    @Test
    void pageCanBeAllocated() {

        Path databasePath =
                tempDirectory.resolve(
                        "test.db"
                );

        try (DiskManager disk =
                     new DiskManager(databasePath)) {

            Page page =
                    disk.allocatePage();

            assertEquals(
                    0,
                    page.getPageId()
            );

            assertEquals(
                    1,
                    disk.getPageCount()
            );
        }
    }

    @Test
    void pageCanBeWrittenAndReadBack() {

        Path databasePath =
                tempDirectory.resolve(
                        "test.db"
                );

        try (DiskManager disk =
                     new DiskManager(databasePath)) {

            Page page =
                    disk.allocatePage();

            byte[] data = {
                    11,
                    22,
                    33,
                    44
            };

            page.write(
                    100,
                    data
            );

            disk.writePage(page);

            Page restored =
                    disk.readPage(0);

            assertArrayEquals(
                    data,
                    restored.read(100, 4)
            );
        }
    }

}