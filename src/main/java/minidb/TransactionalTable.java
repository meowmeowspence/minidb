package minidb;

public class TransactionalTable {

    private final Table table;

    private final LockManager lockManager;

    private final String tableName;

    private final LogManager logManager;

    public TransactionalTable(
            Table table,
            LockManager lockManager
    ) {

        this(
                null,
                table,
                lockManager,
                null
        );
    }

    public TransactionalTable(
            String tableName,
            Table table,
            LockManager lockManager,
            LogManager logManager
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

        if (
                logManager != null
                        && (
                        tableName == null
                                || tableName.isBlank()
                )
        ) {

            throw new IllegalArgumentException(
                    "Logged table must have a name"
            );
        }

        this.tableName =
                tableName;

        this.table =
                table;

        this.lockManager =
                lockManager;

        this.logManager =
                logManager;
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
         * Updating requires exclusive access.
         */
        lockManager.acquireExclusive(
                transaction,
                recordId
        );

        /*
         * Old non-WAL mode remains available
         * for existing uses and tests.
         */
        if (logManager == null) {

            table.update(
                    recordId,
                    tuple
            );

            return;
        }

        byte[] beforeImage =
                table.readRaw(
                        recordId
                );

        byte[] afterImage =
                table.serializeTuple(
                        tuple
                );

        /*
         * WRITE-AHEAD RULE:
         *
         * 1. append UPDATE log record
         * 2. force log to durable storage
         * 3. modify database page
         */
        logManager.appendUpdate(
                transaction.getId(),
                tableName,
                recordId,
                beforeImage,
                afterImage
        );

        logManager.flush();

        table.updateRaw(
                recordId,
                afterImage
        );
    }
}