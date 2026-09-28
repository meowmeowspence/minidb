package minidb;

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
    public String toString() {

        return "RecordId{"
                + "pageId=" + pageId
                + ", slotId=" + slotId
                + '}';
    }
}