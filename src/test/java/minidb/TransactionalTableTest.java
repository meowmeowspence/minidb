package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class TransactionalTableTest {

    @TempDir
    Path tempDirectory;

    @Test
    void transactionalReadAcquiresSharedLock() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "table.db"
                );

        try (
                DiskManager disk =
                        new DiskManager(path)
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            RecordId recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    1,
                                    "Gecko"
                            )
                    );

            LockManager locks =
                    new LockManager();

            TransactionManager transactions =
                    new TransactionManager(
                            locks
                    );

            TransactionalTable transactionalTable =
                    new TransactionalTable(
                            table,
                            locks
                    );

            Transaction transaction =
                    transactions.begin();

            Tuple result =
                    transactionalTable.read(
                            transaction,
                            recordId
                    );

            assertEquals(
                    "Gecko",
                    result.getValue(1)
            );

            assertTrue(
                    locks.holdsSharedLock(
                            transaction,
                            recordId
                    )
            );

            transactions.commit(
                    transaction
            );
        }
    }

    @Test
    void transactionalUpdateAcquiresExclusiveLock() {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "table.db"
                );

        try (
                DiskManager disk =
                        new DiskManager(path)
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            RecordId recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    1,
                                    "Gecko"
                            )
                    );

            LockManager locks =
                    new LockManager();

            TransactionManager transactions =
                    new TransactionManager(
                            locks
                    );

            TransactionalTable transactionalTable =
                    new TransactionalTable(
                            table,
                            locks
                    );

            Transaction transaction =
                    transactions.begin();

            transactionalTable.update(
                    transaction,
                    recordId,
                    tuple(
                            schema,
                            1,
                            "Leopard Gecko"
                    )
            );

            assertTrue(
                    locks.holdsExclusiveLock(
                            transaction,
                            recordId
                    )
            );

            /*
             * T1 already owns X, so reading
             * its record does not require
             * another conflicting lock.
             */
            Tuple result =
                    transactionalTable.read(
                            transaction,
                            recordId
                    );

            assertEquals(
                    "Leopard Gecko",
                    result.getValue(1)
            );

            transactions.commit(
                    transaction
            );
        }
    }

    @Test
    void readerWaitsForWriterToCommit()
            throws Exception {

        Schema schema =
                schema();

        Path path =
                tempDirectory.resolve(
                        "table.db"
                );

        try (
                DiskManager disk =
                        new DiskManager(path)
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(disk)
                    );

            RecordId recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    1,
                                    "Gecko"
                            )
                    );

            LockManager locks =
                    new LockManager();

            TransactionManager transactions =
                    new TransactionManager(
                            locks
                    );

            TransactionalTable transactionalTable =
                    new TransactionalTable(
                            table,
                            locks
                    );

            Transaction writer =
                    transactions.begin();

            Transaction reader =
                    transactions.begin();

            /*
             * Writer modifies the record and
             * continues holding the X lock.
             */
            transactionalTable.update(
                    writer,
                    recordId,
                    tuple(
                            schema,
                            1,
                            "Leopard Gecko"
                    )
            );

            ExecutorService executor =
                    Executors
                            .newSingleThreadExecutor();

            try {

                CountDownLatch started =
                        new CountDownLatch(1);

                Future<Tuple> future =
                        executor.submit(
                                () -> {

                                    started.countDown();

                                    return transactionalTable
                                            .read(
                                                    reader,
                                                    recordId
                                            );
                                }
                        );

                assertTrue(
                        started.await(
                                1,
                                TimeUnit.SECONDS
                        )
                );

                /*
                 * Reader should be blocked by
                 * writer's exclusive lock.
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
                 * Commit releases writer's
                 * exclusive lock.
                 */
                transactions.commit(
                        writer
                );

                Tuple visible =
                        future.get(
                                1,
                                TimeUnit.SECONDS
                        );

                assertEquals(
                        "Leopard Gecko",
                        visible.getValue(1)
                );

                transactions.commit(
                        reader
                );

            } finally {

                executor.shutdownNow();
            }
        }
    }

    private Schema schema() {

        return new Schema(
                List.of(
                        new Column(
                                "id",
                                DataType.INTEGER
                        ),
                        new Column(
                                "species",
                                DataType.STRING
                        )
                )
        );
    }

    private Tuple tuple(
            Schema schema,
            int id,
            String species
    ) {

        return new Tuple(
                schema,
                List.of(
                        id,
                        species
                )
        );
    }
}