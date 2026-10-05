package minidb;

import java.util.concurrent.atomic.AtomicLong;

public class TransactionManager {

    private final AtomicLong nextTransactionId;

    private final LockManager lockManager;

    private final LogManager logManager;

    private final RecoveryManager recoveryManager;

    public TransactionManager(
            LockManager lockManager
    ) {

        this(
                lockManager,
                null,
                null
        );
    }

    public TransactionManager(
            LockManager lockManager,
            LogManager logManager,
            RecoveryManager recoveryManager
    ) {

        if (lockManager == null) {
            throw new IllegalArgumentException(
                    "Lock manager cannot be null"
            );
        }

        if (
                (logManager == null)
                        != (recoveryManager == null)
        ) {

            throw new IllegalArgumentException(
                    "Log manager and recovery manager must be provided together"
            );
        }

        this.lockManager =
                lockManager;

        this.logManager =
                logManager;

        this.recoveryManager =
                recoveryManager;

        long firstTransactionId =
                1;

        if (logManager != null) {

            firstTransactionId =
                    logManager
                            .getMaxTransactionId()
                            + 1;
        }

        this.nextTransactionId =
                new AtomicLong(
                        firstTransactionId
                );
    }

    public Transaction begin() {

        long transactionId =
                nextTransactionId
                        .getAndIncrement();

        return new Transaction(
                transactionId
        );
    }

    public void commit(
            Transaction transaction
    ) {

        validateActive(
                transaction
        );

        /*
         * COMMIT must become durable before we
         * tell the caller that commit succeeded.
         */
        if (logManager != null) {

            logManager.appendCommit(
                    transaction.getId()
            );

            logManager.flush();
        }

        transaction.markCommitted();

        lockManager.releaseAll(
                transaction
        );
    }

    public void abort(
            Transaction transaction
    ) {

        validateActive(
                transaction
        );

        if (recoveryManager != null) {

            /*
             * Restore all before-images and
             * force them to the database file.
             */
            recoveryManager
                    .undoTransaction(
                            transaction.getId()
                    );

            /*
             * Only after the undo is durable do
             * we record a durable ABORT.
             */
            logManager.appendAbort(
                    transaction.getId()
            );

            logManager.flush();
        }

        transaction.markAborted();

        lockManager.releaseAll(
                transaction
        );
    }

    private void validateActive(
            Transaction transaction
    ) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Transaction cannot be null"
            );
        }

        if (!transaction.isActive()) {
            throw new IllegalStateException(
                    "Transaction is no longer active"
            );
        }
    }
}