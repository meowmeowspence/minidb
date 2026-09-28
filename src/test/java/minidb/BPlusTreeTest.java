package minidb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BPlusTreeTest {

    @Test
    void emptyTreeReturnsNoMatch() {

        BPlusTree tree =
                new BPlusTree(3);

        assertNull(
                tree.search(10)
        );

        assertEquals(
                0,
                tree.size()
        );

        assertEquals(
                1,
                tree.getHeight()
        );
    }

    @Test
    void insertedKeysCanBeFound() {

        BPlusTree tree =
                new BPlusTree(3);

        RecordId ten =
                new RecordId(1, 0);

        RecordId twenty =
                new RecordId(1, 1);

        RecordId thirty =
                new RecordId(2, 0);

        /*
         * Insert deliberately out of order.
         */
        tree.insert(30, thirty);
        tree.insert(10, ten);
        tree.insert(20, twenty);

        assertEquals(
                ten,
                tree.search(10)
        );

        assertEquals(
                twenty,
                tree.search(20)
        );

        assertEquals(
                thirty,
                tree.search(30)
        );

        assertNull(
                tree.search(99)
        );

        assertEquals(
                3,
                tree.size()
        );
    }

    @Test
    void overflowingLeafCreatesNewTreeLevel() {

        BPlusTree tree =
                new BPlusTree(3);

        tree.insert(
                10,
                new RecordId(0, 0)
        );

        tree.insert(
                20,
                new RecordId(0, 1)
        );

        tree.insert(
                30,
                new RecordId(0, 2)
        );

        assertEquals(
                1,
                tree.getHeight()
        );

        /*
         * Fourth key overflows the leaf,
         * forcing the first split.
         */
        tree.insert(
                40,
                new RecordId(0, 3)
        );

        assertEquals(
                2,
                tree.getHeight()
        );

        assertNotNull(
                tree.search(10)
        );

        assertNotNull(
                tree.search(40)
        );
    }

    @Test
    void manyInsertionsSurviveInternalSplits() {

        BPlusTree tree =
                new BPlusTree(3);

        for (int key = 1;
             key <= 50;
             key++) {

            tree.insert(
                    key,
                    new RecordId(
                            key / 10,
                            key
                    )
            );
        }

        assertEquals(
                50,
                tree.size()
        );

        /*
         * With only 3 keys allowed per
         * node, 50 values should require
         * multiple levels.
         */
        assertTrue(
                tree.getHeight() >= 3
        );

        for (int key = 1;
             key <= 50;
             key++) {

            RecordId result =
                    tree.search(key);

            assertNotNull(result);

            assertEquals(
                    key,
                    result.getSlotId()
            );
        }
    }

    @Test
    void rangeSearchCrossesLeafBoundaries() {

        BPlusTree tree =
                new BPlusTree(3);

        RecordId ten =
                new RecordId(1, 0);

        RecordId twenty =
                new RecordId(1, 1);

        RecordId thirty =
                new RecordId(2, 0);

        RecordId forty =
                new RecordId(2, 1);

        RecordId fifty =
                new RecordId(3, 0);

        tree.insert(10, ten);
        tree.insert(20, twenty);
        tree.insert(30, thirty);
        tree.insert(40, forty);
        tree.insert(50, fifty);

        List<RecordId> results =
                tree.rangeSearch(
                        20,
                        40
                );

        assertEquals(
                List.of(
                        twenty,
                        thirty,
                        forty
                ),
                results
        );
    }

    @Test
    void duplicateKeysAreRejected() {

        BPlusTree tree =
                new BPlusTree(3);

        tree.insert(
                10,
                new RecordId(0, 0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> tree.insert(
                        10,
                        new RecordId(0, 1)
                )
        );
    }
}