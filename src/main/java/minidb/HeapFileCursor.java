package minidb;

public class HeapFileCursor {

    private final DiskManager diskManager;

    private int currentPageId;
    private int currentSlotId;

    private SlottedPage currentPage;

    public HeapFileCursor(
            DiskManager diskManager
    ) {

        if (diskManager == null) {
            throw new IllegalArgumentException(
                    "Disk manager cannot be null"
            );
        }

        this.diskManager = diskManager;

        this.currentPageId = 0;
        this.currentSlotId = 0;
        this.currentPage = null;
    }

    public byte[] next() {

        while (
                currentPageId
                        < diskManager.getPageCount()
        ) {

            /*
             * Load the current page if we
             * have not loaded it yet.
             */
            if (currentPage == null) {

                Page page =
                        diskManager.readPage(
                                currentPageId
                        );

                currentPage =
                        new SlottedPage(page);
            }

            /*
             * If another tuple exists on
             * this page, return it.
             */
            if (currentSlotId
                    < currentPage.getTupleCount()) {

                byte[] tuple =
                        currentPage.readTuple(
                                currentSlotId
                        );

                currentSlotId++;

                return tuple;
            }

            /*
             * Current page is exhausted.
             * Move to the next page.
             */
            currentPageId++;
            currentSlotId = 0;
            currentPage = null;
        }

        /*
         * Returning null means:
         * no records remain.
         */
        return null;
    }
}