package minidb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class SlottedPage {

    private static final int HEADER_SIZE = 8;
    private static final int SLOT_SIZE = 8;

    private static final int TUPLE_COUNT_OFFSET = 0;
    private static final int FREE_SPACE_END_OFFSET = 4;

    private final Page page;

    public SlottedPage(Page page) {

        if (page == null) {
            throw new IllegalArgumentException(
                    "Page cannot be null"
            );
        }

        this.page = page;

        /*
         * A brand-new Page contains only zero bytes.
         *
         * For an empty slotted page, free space should
         * initially extend all the way to the end of
         * the 4096-byte page.
         */
        if (readInt(FREE_SPACE_END_OFFSET) == 0
                && readInt(TUPLE_COUNT_OFFSET) == 0) {

            writeInt(
                    FREE_SPACE_END_OFFSET,
                    Page.PAGE_SIZE
            );
        }
    }

    public int getTupleCount() {
        return readInt(TUPLE_COUNT_OFFSET);
    }

    public int getFreeSpace() {

        int tupleCount = getTupleCount();

        int slotDirectoryEnd =
                HEADER_SIZE
                        + tupleCount * SLOT_SIZE;

        return getFreeSpaceEnd()
                - slotDirectoryEnd;
    }

    public boolean canFit(int tupleLength) {

        if (tupleLength < 0) {
            return false;
        }

        /*
         * Inserting a tuple requires space for:
         *
         * 1. the tuple itself
         * 2. one new slot entry
         */
        return getFreeSpace()
                >= tupleLength + SLOT_SIZE;
    }

    public int insertTuple(byte[] tupleBytes) {

        if (tupleBytes == null) {
            throw new IllegalArgumentException(
                    "Tuple bytes cannot be null"
            );
        }

        if (!canFit(tupleBytes.length)) {
            throw new IllegalStateException(
                    "Tuple does not fit on page"
            );
        }

        int tupleCount = getTupleCount();

        int tupleOffset =
                getFreeSpaceEnd()
                        - tupleBytes.length;

        /*
         * Store the tuple near the END of the page.
         */
        page.write(
                tupleOffset,
                tupleBytes
        );

        /*
         * Store its location in the slot directory.
         */
        int slotOffset =
                HEADER_SIZE
                        + tupleCount * SLOT_SIZE;

        writeInt(
                slotOffset,
                tupleOffset
        );

        writeInt(
                slotOffset + 4,
                tupleBytes.length
        );

        /*
         * Update page metadata.
         */
        writeInt(
                TUPLE_COUNT_OFFSET,
                tupleCount + 1
        );

        writeInt(
                FREE_SPACE_END_OFFSET,
                tupleOffset
        );

        /*
         * The slot number becomes the tuple's
         * identifier within this page.
         */
        return tupleCount;
    }

    public byte[] readTuple(int slotId) {

        validateSlotId(slotId);

        int slotOffset =
                HEADER_SIZE
                        + slotId * SLOT_SIZE;

        int tupleOffset =
                readInt(slotOffset);

        int tupleLength =
                readInt(slotOffset + 4);

        return page.read(
                tupleOffset,
                tupleLength
        );
    }

    public Page getPage() {
        return page;
    }

    public void updateTuple(
            int slotId,
            byte[] tupleBytes
    ) {

        validateSlotId(slotId);

        if (tupleBytes == null) {
            throw new IllegalArgumentException(
                    "Tuple bytes cannot be null"
            );
        }

        /*
         * Read every tuple before modifying
         * the physical page.
         */
        List<byte[]> tuples =
                new ArrayList<>();

        int totalTupleBytes = 0;

        for (
                int i = 0;
                i < getTupleCount();
                i++
        ) {

            byte[] bytes;

            if (i == slotId) {
                bytes = tupleBytes;
            } else {
                bytes = readTuple(i);
            }

            tuples.add(bytes);

            totalTupleBytes +=
                    bytes.length;
        }

        int requiredSpace =
                HEADER_SIZE
                        + tuples.size()
                        * SLOT_SIZE
                        + totalTupleBytes;

        /*
         * Check BEFORE touching the original
         * page. If the updated record cannot
         * fit, the page remains unchanged.
         */
        if (requiredSpace
                > Page.PAGE_SIZE) {

            throw new IllegalStateException(
                    "Updated tuple does not fit on page"
            );
        }

        /*
         * Clear the page.
         */
        page.write(
                0,
                new byte[Page.PAGE_SIZE]
        );

        /*
         * Restore the empty slotted-page
         * header.
         */
        writeInt(
                TUPLE_COUNT_OFFSET,
                0
        );

        writeInt(
                FREE_SPACE_END_OFFSET,
                Page.PAGE_SIZE
        );

        /*
         * Reinsert tuples in their original
         * order.
         *
         * Because the order is unchanged,
         * their slot IDs remain unchanged.
         */
        for (byte[] bytes : tuples) {

            insertTuple(bytes);
        }
    }

    private int getFreeSpaceEnd() {
        return readInt(FREE_SPACE_END_OFFSET);
    }

    private void validateSlotId(int slotId) {

        if (slotId < 0
                || slotId >= getTupleCount()) {

            throw new IllegalArgumentException(
                    "Invalid slot ID: " + slotId
            );
        }
    }

    private int readInt(int offset) {

        byte[] bytes =
                page.read(offset, Integer.BYTES);

        return ByteBuffer.wrap(bytes)
                .getInt();
    }

    private void writeInt(
            int offset,
            int value
    ) {

        byte[] bytes =
                ByteBuffer
                        .allocate(Integer.BYTES)
                        .putInt(value)
                        .array();

        page.write(offset, bytes);
    }
}