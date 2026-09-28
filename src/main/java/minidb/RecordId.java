package minidb;

import java.util.Objects;

public class RecordId {

    private final int pageId;
    private final int slotId;

    public RecordId(
            int pageId,
            int slotId
    ) {

        if (pageId < 0) {
            throw new IllegalArgumentException(
                    "Page ID cannot be negative"
            );
        }

        if (slotId < 0) {
            throw new IllegalArgumentException(
                    "Slot ID cannot be negative"
            );
        }

        this.pageId = pageId;
        this.slotId = slotId;
    }

    public int getPageId() {
        return pageId;
    }

    public int getSlotId() {
        return slotId;
    }

    @Override
    public boolean equals(Object other) {

        if (this == other) {
            return true;
        }

        if (!(other instanceof RecordId recordId)) {
            return false;
        }

        return pageId == recordId.pageId
                && slotId == recordId.slotId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                pageId,
                slotId
        );
    }

    @Override
    public String toString() {

        return "RecordId{"
                + "pageId=" + pageId
                + ", slotId=" + slotId
                + '}';
    }
}