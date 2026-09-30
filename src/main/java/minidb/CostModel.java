package minidb;

public final class CostModel {

    private CostModel() {
    }

    public static long sequentialScanCost(
            TableStatistics statistics
    ) {

        if (statistics == null) {
            throw new IllegalArgumentException(
                    "Statistics cannot be null"
            );
        }

        /*
         * Simplified model:
         *
         * sequential scan cost =
         * number of data pages read.
         */
        return statistics.getPageCount();
    }

    public static long bPlusTreeEqualityCost(
            BPlusTree tree
    ) {

        if (tree == null) {
            throw new IllegalArgumentException(
                    "B+ tree cannot be null"
            );
        }

        /*
         * Simplified conventional estimate:
         *
         * tree traversal
         * +
         * one heap-page fetch.
         *
         * Our current tree itself is
         * memory-resident, so these are
         * pedagogical cost units rather
         * than literal current disk I/Os.
         */
        return tree.getHeight() + 1L;
    }

    public static long hashEqualityCost() {

        /*
         * Simplified estimate:
         *
         * one hash-index access
         * +
         * one heap-page access.
         *
         * Again, our current hash index is
         * memory-resident.
         */
        return 2;
    }
}