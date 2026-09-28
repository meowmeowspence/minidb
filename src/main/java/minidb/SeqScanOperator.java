package minidb;

public class SeqScanOperator
        implements Operator {

    private final Table table;

    private HeapFileCursor cursor;
    private boolean open;

    public SeqScanOperator(
            Table table
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        this.table = table;
        this.cursor = null;
        this.open = false;
    }

    @Override
    public void open() {

        cursor =
                table.openCursor();

        open = true;
    }

    @Override
    public Tuple next() {

        ensureOpen();

        byte[] bytes =
                cursor.next();

        if (bytes == null) {
            return null;
        }

        return table.deserialize(
                bytes
        );
    }

    @Override
    public Schema getOutputSchema() {
        return table.getSchema();
    }

    @Override
    public void close() {

        cursor = null;
        open = false;
    }

    private void ensureOpen() {

        if (!open) {
            throw new IllegalStateException(
                    "Operator must be opened before reading"
            );
        }
    }
}