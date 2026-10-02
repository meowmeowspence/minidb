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

    public static long nestedLoopJoinCost(
            TableStatistics left,
            TableStatistics right
    ) {

        validateStatistics(
                left,
                right
        );

        return safeMultiply(
                left.getRowCount(),
                right.getRowCount()
        );
    }

    public static long hashJoinCost(
            TableStatistics left,
            TableStatistics right
    ) {

        validateStatistics(
                left,
                right
        );

        /*
         * Simplified model:
         *
         * read/build right side
         * +
         * probe with left side
         * +
         * fixed hash-table setup overhead.
         */
        return safeAdd(
                safeAdd(
                        left.getRowCount(),
                        right.getRowCount()
                ),
                10
        );
    }

    public static long externalSortCost(
            TableStatistics statistics,
            int maxTuplesPerRun
    ) {

        if (statistics == null) {
            throw new IllegalArgumentException(
                    "Statistics cannot be null"
            );
        }

        if (maxTuplesPerRun < 1) {
            throw new IllegalArgumentException(
                    "Run capacity must be at least 1"
            );
        }

        long rows =
                statistics.getRowCount();

        if (rows == 0) {
            return 0;
        }

        /*
         * Number of initial sorted runs.
         *
         * Example:
         *
         * 10 rows
         * capacity 4
         *
         * creates:
         *
         * 4 rows
         * 4 rows
         * 2 rows
         *
         * = 3 runs.
         */
        long runCount =
                (rows + maxTuplesPerRun - 1)
                        / maxTuplesPerRun;

        int mergePasses = 0;

        /*
         * Our ExternalSortOperator performs
         * pairwise merges:
         *
         * 4 runs -> 2 -> 1
         *
         * therefore two merge passes.
         */
        while (runCount > 1) {

            runCount =
                    (runCount + 1)
                            / 2;

            mergePasses++;
        }

        /*
         * Run generation:
         *
         * read rows + write rows
         * = 2R
         *
         * Each merge pass:
         *
         * read rows + write rows
         * = another 2R
         */
        long passes =
                1L + mergePasses;

        return safeMultiply(
                safeMultiply(
                        2,
                        rows
                ),
                passes
        );
    }

    public static long sortMergeJoinCost(
            TableStatistics left,
            TableStatistics right,
            int maxTuplesPerRun
    ) {

        validateStatistics(
                left,
                right
        );

        long leftSort =
                externalSortCost(
                        left,
                        maxTuplesPerRun
                );

        long rightSort =
                externalSortCost(
                        right,
                        maxTuplesPerRun
                );

        /*
         * After sorting, the merge itself
         * scans each input roughly once.
         */
        long merge =
                safeAdd(
                        left.getRowCount(),
                        right.getRowCount()
                );

        return safeAdd(
                safeAdd(
                        leftSort,
                        rightSort
                ),
                merge
        );
    }

    public static long estimateEqualityJoinRowCount(
            TableStatistics left,
            String leftColumnName,
            TableStatistics right,
            String rightColumnName
    ) {

        validateStatistics(
                left,
                right
        );

        if (
                left.getRowCount() == 0
                        || right.getRowCount() == 0
        ) {
            return 0;
        }

        int leftDistinct =
                left.getDistinctCount(
                        leftColumnName
                );

        int rightDistinct =
                right.getDistinctCount(
                        rightColumnName
                );

        int largestDistinctCount =
                Math.max(
                        leftDistinct,
                        rightDistinct
                );

        if (largestDistinctCount == 0) {
            return 0;
        }

        long possiblePairs =
                safeMultiply(
                        left.getRowCount(),
                        right.getRowCount()
                );

        /*
         * Standard simple uniformity estimate:
         *
         * |R join S|
         *
         * ≈
         *
         * |R| × |S|
         * ---------------------
         * max(V(R,a), V(S,b))
         */
        return Math.max(
                1,
                (long) Math.ceil(
                        (double) possiblePairs
                                / largestDistinctCount
                )
        );
    }

    private static void validateStatistics(
            TableStatistics left,
            TableStatistics right
    ) {

        if (left == null || right == null) {
            throw new IllegalArgumentException(
                    "Statistics cannot be null"
            );
        }
    }

    private static long safeAdd(
            long left,
            long right
    ) {

        try {
            return Math.addExact(
                    left,
                    right
            );

        } catch (ArithmeticException e) {

            return Long.MAX_VALUE;
        }
    }

    private static long safeMultiply(
            long left,
            long right
    ) {

        try {
            return Math.multiplyExact(
                    left,
                    right
            );

        } catch (ArithmeticException e) {

            return Long.MAX_VALUE;
        }
    }

}