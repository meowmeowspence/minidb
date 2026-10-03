package minidb;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class LockManagerTest {

    @Test
    void multipleTransactionsCanHoldSharedLocks() {

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

        RecordId recordId =
                new RecordId(
                        1,
                        2
                );

        lockManager.acquireShared(
                first,
                recordId
        );

        lockManager.acquireShared(
                second,
                recordId
        );

        assertTrue(
                lockManager.holdsSharedLock(
                        first,
                        recordId
                )
        );

        assertTrue(
                lockManager.holdsSharedLock(
                        second,
                        recordId
                )
        );

        manager.commit(first);
        manager.commit(second);
    }

    @Test
    void exclusiveLockBlocksAnotherReaderUntilCommit()
            throws Exception {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction writer =
                manager.begin();

        Transaction reader =
                manager.begin();

        RecordId recordId =
                new RecordId(
                        2,
                        5
                );

        lockManager.acquireExclusive(
                writer,
                recordId
        );

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {

            CountDownLatch started =
                    new CountDownLatch(1);

            Future<Boolean> future =
                    executor.submit(
                            () -> {

                                started.countDown();

                                lockManager.acquireShared(
                                        reader,
                                        recordId
                                );

                                return true;
                            }
                    );

            assertTrue(
                    started.await(
                            1,
                            TimeUnit.SECONDS
                    )
            );

            /*
             * Reader should still be blocked.
             */
            assertThrows(
                    TimeoutException.class,
                    () ->
                            future.get(
                                    100,
                                    TimeUnit.MILLISECONDS
                            )
            );

            /*
             * Commit releases writer's X lock.
             */
            manager.commit(
                    writer
            );

            assertTrue(
                    future.get(
                            1,
                            TimeUnit.SECONDS
                    )
            );

            manager.commit(
                    reader
            );

        } finally {

            executor.shutdownNow();
        }
    }

    @Test
    void sharedLockBlocksWriterUntilCommit()
            throws Exception {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction reader =
                manager.begin();

        Transaction writer =
                manager.begin();

        RecordId recordId =
                new RecordId(
                        3,
                        1
                );

        lockManager.acquireShared(
                reader,
                recordId
        );

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {

            CountDownLatch started =
                    new CountDownLatch(1);

            Future<Boolean> future =
                    executor.submit(
                            () -> {

                                started.countDown();

                                lockManager.acquireExclusive(
                                        writer,
                                        recordId
                                );

                                return true;
                            }
                    );

            assertTrue(
                    started.await(
                            1,
                            TimeUnit.SECONDS
                    )
            );

            assertThrows(
                    TimeoutException.class,
                    () ->
                            future.get(
                                    100,
                                    TimeUnit.MILLISECONDS
                            )
            );

            manager.commit(
                    reader
            );

            assertTrue(
                    future.get(
                            1,
                            TimeUnit.SECONDS
                    )
            );

            manager.commit(
                    writer
            );

        } finally {

            executor.shutdownNow();
        }
    }

    @Test
    void soleSharedOwnerCanUpgradeToExclusive() {

        LockManager lockManager =
                new LockManager();

        TransactionManager manager =
                new TransactionManager(
                        lockManager
                );

        Transaction transaction =
                manager.begin();

        RecordId recordId =
                new RecordId(
                        4,
                        8
                );

        lockManager.acquireShared(
                transaction,
                recordId
        );

        assertTrue(
                lockManager.holdsSharedLock(
                        transaction,
                        recordId
                )
        );

        lockManager.acquireExclusive(
                transaction,
                recordId
        );

        assertTrue(
                lockManager.holdsExclusiveLock(
                        transaction,
                        recordId
                )
        );

        assertFalse(
                lockManager.holdsSharedLock(
                        transaction,
                        recordId
                )
        );

        manager.commit(
                transaction
        );
    }
}