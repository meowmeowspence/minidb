package minidb;

public class HeapFile {

    private final DiskManager diskManager;

    public HeapFile(
            DiskManager diskManager
    ) {

        if (diskManager == null) {
            throw new IllegalArgumentException(
                    "Disk manager cannot be null"
            );
        }

        this.diskManager =
                diskManager;
    }

    public RecordId insert(
            byte[] tupleBytes
    ) {

        if (tupleBytes == null) {
            throw new IllegalArgumentException(
                    "Tuple bytes cannot be null"
            );
        }

        if (tupleBytes.length
                + 8
                > Page.PAGE_SIZE - 8) {

            throw new IllegalArgumentException(
                    "Tuple is too large to fit on a page"
            );
        }

        /*
         * Search existing pages first.
         */
        for (
                int pageId = 0;
                pageId < diskManager.getPageCount();
                pageId++
        ) {

            Page page =
                    diskManager.readPage(pageId);

            SlottedPage slottedPage =
                    new SlottedPage(page);

            if (slottedPage.canFit(
                    tupleBytes.length
            )) {

                int slotId =
                        slottedPage.insertTuple(
                                tupleBytes
                        );

                diskManager.writePage(
                        slottedPage.getPage()
                );

                return new RecordId(
                        pageId,
                        slotId
                );
            }
        }

        /*
         * No existing page had enough room,
         * so allocate a new one.
         */
        Page page =
                diskManager.allocatePage();

        SlottedPage slottedPage =
                new SlottedPage(page);

        int slotId =
                slottedPage.insertTuple(
                        tupleBytes
                );

        diskManager.writePage(
                slottedPage.getPage()
        );

        return new RecordId(
                page.getPageId(),
                slotId
        );
    }

    public byte[] read(
            RecordId recordId
    ) {

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Record ID cannot be null"
            );
        }

        Page page =
                diskManager.readPage(
                        recordId.getPageId()
                );

        SlottedPage slottedPage =
                new SlottedPage(page);

        return slottedPage.readTuple(
                recordId.getSlotId()
        );
    }

    public HeapFileCursor openCursor() {
        return new HeapFileCursor(
                diskManager
        );
    }

    public int getPageCount() {
        return diskManager.getPageCount();
    }
}