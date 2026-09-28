package minidb;

public class IndexLookupOperator
        implements Operator {

    private final Table table;
    private final BPlusTree index;
    private final int key;

    private boolean open;
    private boolean consumed;

    public IndexLookupOperator(
            Table table,
            BPlusTree index,
            int key
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        if (index == null) {
            throw new IllegalArgumentException(
                    "Index cannot be null"
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

        /*
         * Exact lookup returns at most
         * one tuple because our first
         * B+ tree version requires
         * unique integer keys.
         */
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