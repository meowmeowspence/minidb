package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionManagerTest {

    @Test
    void beginCreatesActiveUniqueTransactions() {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction first =
                manager.begin();

        Transaction second =
                manager.begin();

        assertTrue(
                first.isActive()
        );

        assertTrue(
                second.isActive()
        );

        assertNotEquals(
                first.getId(),
                second.getId()
        );
    }

    @Test
    void transactionCanCommit() {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction transaction =
                manager.begin();

        manager.commit(
                transaction
        );

        assertEquals(
                TransactionState.COMMITTED,
                transaction.getState()
        );

        assertFalse(
                transaction.isActive()
        );
    }

    @Test
    void transactionCanAbort() {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction transaction =
                manager.begin();

        manager.abort(
                transaction
        );

        assertEquals(
                TransactionState.ABORTED,
                transaction.getState()
        );

        assertFalse(
                transaction.isActive()
        );
    }
}