package minidb;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

public class DiskManager implements AutoCloseable {

    private final RandomAccessFile file;

    public DiskManager(Path path) {

        try {
            this.file =
                    new RandomAccessFile(
                            path.toFile(),
                            "rw"
                    );

            if (file.length() % Page.PAGE_SIZE != 0) {
                throw new IllegalStateException(
                        "Database file size is not aligned to page size"
                );
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to open database file",
                    e
            );
        }
    }

    public synchronized Page allocatePage() {

        try {
            long currentLength =
                    file.length();

            int pageId =
                    Math.toIntExact(
                            currentLength
                                    / Page.PAGE_SIZE
                    );

            file.setLength(
                    currentLength
                            + Page.PAGE_SIZE
            );

            return new Page(pageId);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to allocate page",
                    e
            );
        }
    }

    public synchronized void writePage(Page page) {

        try {
            long offset =
                    (long) page.getPageId()
                            * Page.PAGE_SIZE;

            file.seek(offset);

            file.write(
                    page.toByteArray()
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to write page",
                    e
            );
        }
    }

    public synchronized Page readPage(int pageId) {

        try {
            if (pageId < 0) {
                throw new IllegalArgumentException(
                        "Page ID cannot be negative"
                );
            }

            long offset =
                    (long) pageId
                            * Page.PAGE_SIZE;

            if (offset + Page.PAGE_SIZE
                    > file.length()) {

                throw new IllegalArgumentException(
                        "Page does not exist: "
                                + pageId
                );
            }

            byte[] data =
                    new byte[Page.PAGE_SIZE];

            file.seek(offset);
            file.readFully(data);

            return new Page(
                    pageId,
                    data
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read page",
                    e
            );
        }
    }

    public synchronized int getPageCount() {

        try {
            return Math.toIntExact(
                    file.length()
                            / Page.PAGE_SIZE
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to determine page count",
                    e
            );
        }
    }

    @Override
    public synchronized void close() {

        try {
            file.close();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to close database file",
                    e
            );
        }
    }

    public synchronized void flush() {

        try {

            file.getFD().sync();

        } catch (java.io.IOException e) {

            throw new IllegalStateException(
                    "Failed to flush database file",
                    e
            );
        }
    }

}