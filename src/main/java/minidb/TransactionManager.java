package minidb;

import java.util.concurrent.atomic.AtomicLong;

public class TransactionManager {

    private final AtomicLong nextTransactionId;

    private final LockManager lockManager;

    public TransactionManager(
            LockManager lockManager
    ) {

        if (lockManager == null) {
            throw new IllegalArgumentException(
                    "Lock manager cannot be null"
            );
        }

        this.lockManager =
                lockManager;

        this.nextTransactionId =
                new AtomicLong(1);
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
         * Mark the transaction finished
         * before releasing its locks so it
         * cannot acquire new locks afterward.
         */
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