package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashIndexTest {

    @Test
    void insertedKeyCanBeFound() {

        HashIndex index =
                new HashIndex(8);

        RecordId recordId =
                new RecordId(
                        3,
                        7
                );

        index.insert(
                42,
                recordId
        );

        assertEquals(
                recordId,
                index.search(42)
        );

        assertEquals(
                1,
                index.size()
        );
    }

    @Test
    void missingKeyReturnsNull() {

        HashIndex index =
                new HashIndex(8);

        assertNull(
                index.search(999)
        );
    }

    @Test
    void collisionsAreHandled() {

        /*
         * With four buckets:
         *
         * 1 mod 4 = 1
         * 5 mod 4 = 1
         *
         * so these collide.
         */
        HashIndex index =
                new HashIndex(4);

        RecordId one =
                new RecordId(
                        1,
                        0
                );

        RecordId five =
                new RecordId(
                        2,
                        0
                );

        index.insert(
                1,
                one
        );

        index.insert(
                5,
                five
        );

        assertEquals(
                one,
                index.search(1)
        );

        assertEquals(
                five,
                index.search(5)
        );
    }

    @Test
    void duplicateKeysAreRejected() {

        HashIndex index =
                new HashIndex(8);

        index.insert(
                10,
                new RecordId(
                        0,
                        0
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        index.insert(
                                10,
                                new RecordId(
                                        0,
                                        1
                                )
                        )
        );
    }
}