package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlottedPageTest {

    @Test
    void newSlottedPageIsEmpty() {

        Page page = new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        assertEquals(
                0,
                slottedPage.getTupleCount()
        );

        assertEquals(
                Page.PAGE_SIZE - 8,
                slottedPage.getFreeSpace()
        );
    }

    @Test
    void tupleCanBeInsertedAndRead() {

        Page page = new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        byte[] tuple = {
                10,
                20,
                30,
                40
        };

        int slotId =
                slottedPage.insertTuple(tuple);

        assertEquals(0, slotId);

        assertEquals(
                1,
                slottedPage.getTupleCount()
        );

        assertArrayEquals(
                tuple,
                slottedPage.readTuple(slotId)
        );
    }

    @Test
    void multipleTuplesCanBeStored() {

        Page page = new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        byte[] first = {
                1,
                2,
                3
        };

        byte[] second = {
                10,
                20,
                30,
                40,
                50
        };

        int firstSlot =
                slottedPage.insertTuple(first);

        int secondSlot =
                slottedPage.insertTuple(second);

        assertEquals(0, firstSlot);
        assertEquals(1, secondSlot);

        assertEquals(
                2,
                slottedPage.getTupleCount()
        );

        assertArrayEquals(
                first,
                slottedPage.readTuple(firstSlot)
        );

        assertArrayEquals(
                second,
                slottedPage.readTuple(secondSlot)
        );
    }

    @Test
    void invalidSlotCannotBeRead() {

        Page page = new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        assertThrows(
                IllegalArgumentException.class,
                () -> slottedPage.readTuple(0)
        );
    }

    @Test
    void slottedPageMetadataSurvivesPageReconstruction() {

        Page originalPage =
                new Page(0);

        SlottedPage original =
                new SlottedPage(originalPage);

        byte[] tuple = {
                5,
                10,
                15,
                20
        };

        original.insertTuple(tuple);

        byte[] rawPageData =
                originalPage.toByteArray();

        Page reconstructedPage =
                new Page(
                        0,
                        rawPageData
                );

        SlottedPage reconstructed =
                new SlottedPage(
                        reconstructedPage
                );

        assertEquals(
                1,
                reconstructed.getTupleCount()
        );

        assertArrayEquals(
                tuple,
                reconstructed.readTuple(0)
        );
    }

    @Test
    void tupleCanBeUpdated() {

        Page page =
                new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        int slot =
                slottedPage.insertTuple(
                        new byte[] {
                                1,
                                2,
                                3
                        }
                );

        byte[] replacement = {
                9,
                8,
                7,
                6
        };

        slottedPage.updateTuple(
                slot,
                replacement
        );

        assertArrayEquals(
                replacement,
                slottedPage.readTuple(slot)
        );

        assertEquals(
                1,
                slottedPage.getTupleCount()
        );
    }

    @Test
    void growingTuplePreservesOtherSlots() {

        Page page =
                new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        int firstSlot =
                slottedPage.insertTuple(
                        new byte[] {
                                1,
                                2
                        }
                );

        byte[] second = {
                10,
                20,
                30
        };

        int secondSlot =
                slottedPage.insertTuple(
                        second
                );

        byte[] larger =
                new byte[500];

        larger[0] = 99;
        larger[499] = 88;

        slottedPage.updateTuple(
                firstSlot,
                larger
        );

        assertArrayEquals(
                larger,
                slottedPage.readTuple(
                        firstSlot
                )
        );

        assertArrayEquals(
                second,
                slottedPage.readTuple(
                        secondSlot
                )
        );

        assertEquals(
                2,
                slottedPage.getTupleCount()
        );
    }

    @Test
    void oversizedUpdateDoesNotDestroyOriginalTuple() {

        Page page =
                new Page(0);

        SlottedPage slottedPage =
                new SlottedPage(page);

        byte[] original = {
                1,
                2,
                3
        };

        int slot =
                slottedPage.insertTuple(
                        original
                );

        byte[] tooLarge =
                new byte[Page.PAGE_SIZE];

        assertThrows(
                IllegalStateException.class,
                () ->
                        slottedPage.updateTuple(
                                slot,
                                tooLarge
                        )
        );

        /*
         * Failed update must not corrupt
         * the existing record.
         */
        assertArrayEquals(
                original,
                slottedPage.readTuple(slot)
        );
    }

}