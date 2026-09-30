package minidb;

public class HashIndexLookupOperator
        implements Operator {

    private final Table table;
    private final HashIndex index;
    private final int key;

    private boolean open;
    private boolean consumed;

    public HashIndexLookupOperator(
            Table table,
            HashIndex index,
            int key
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        if (index == null) {
            throw new IllegalArgumentException(
                    "Hash index cannot be null"
            );
        }

        this.table = table;
        this.index = index;
        this.key = key;

        this.open = false;
        this.consumed = false;
    }

    @Override
    public void open() {

        open = true;
        consumed = false;
    }

    @Override
    public Tuple next() {

        ensureOpen();

        if (consumed) {
            return null;
        }

        consumed = true;

        RecordId recordId =
                index.search(key);

        if (recordId == null) {
            return null;
        }

        return table.read(
                recordId
        );
    }

    @Override
    public Schema getOutputSchema() {
        return table.getSchema();
    }

    @Override
    public void close() {

        open = false;
        consumed = false;
    }

    private void ensureOpen() {

        if (!open) {
            throw new IllegalStateException(
                    "Operator must be opened before reading"
            );
        }
    }
}