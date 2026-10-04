package minidb;

public class TransactionalTable {

    private final Table table;

    private final LockManager lockManager;

    public TransactionalTable(
            Table table,
            LockManager lockManager
    ) {

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        if (lockManager == null) {
            throw new IllegalArgumentException(
                    "Lock manager cannot be null"
            );
        }

        this.table = table;
        this.lockManager = lockManager;
    }

    public Schema getSchema() {
        return table.getSchema();
    }

    public Tuple read(
            Transaction transaction,
            RecordId recordId
    ) {

        /*
         * Reading requires a shared lock.
         *
         * The lock remains held until the
         * transaction commits or aborts.
         */
        lockManager.acquireShared(
                transaction,
                recordId
        );

        return table.read(
                recordId
        );
    }

    public void update(
            Transaction transaction,
            RecordId recordId,
            Tuple tuple
    ) {

        /*
         * Updating requires exclusive access
         * to the record.
         */
        lockManager.acquireExclusive(
                transaction,
                recordId
        );

        table.update(
                recordId,
                tuple
        );
    }
}