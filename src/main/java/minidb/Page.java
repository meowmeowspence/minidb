package minidb;

import java.util.Arrays;

public class Page {

    public static final int PAGE_SIZE = 4096;

    private final int pageId;
    private final byte[] data;

    public Page(int pageId) {

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative"
            );
        }

        this.pageId = pageId;
        this.data = new byte[PAGE_SIZE];
    }

    public Page(
            int pageId,
            byte[] data
    ) {

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative"
            );
        }

        if (data == null ||
                data.length != PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page data must be exactly "
                            + PAGE_SIZE
                            + " bytes"
            );
        }

        this.pageId = pageId;
        this.data =
                Arrays.copyOf(data, data.length);
    }

    public int getPageId() {
        return pageId;
    }

    public void write(
            int offset,
            byte[] bytes
    ) {

        validateRange(
                offset,
                bytes.length
        );

        System.arraycopy(
                bytes,
                0,
                data,
                offset,
                bytes.length
        );
    }

    public byte[] read(
            int offset,
            int length
    ) {

        validateRange(offset, length);

        return Arrays.copyOfRange(
                data,
                offset,
                offset + length
        );
    }

    public byte[] toByteArray() {
        return Arrays.copyOf(
                data,
                data.length
        );
    }

    private void validateRange(
            int offset,
            int length
    ) {

        if (offset < 0 ||
                length < 0 ||
                offset + length > PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Page access is outside page boundaries"
            );
        }
    }

}