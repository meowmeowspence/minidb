package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WalTransactionalTableTest {

    @TempDir
    Path tempDirectory;

    @Test
    void transactionalUpdateWritesBeforeAndAfterImages() {

        Path databasePath =
                tempDirectory.resolve(
                        "animals.db"
                );

        Path logPath =
                tempDirectory.resolve(
                        "minidb.log"
                );

        Schema schema =
                schema();

        try (
                DiskManager disk =
                        new DiskManager(
                                databasePath
                        );

                LogManager log =
                        new LogManager(
                                logPath
                        )
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(
                                    disk
                            )
                    );

            RecordId recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    "Gecko"
                            )
                    );

            LockManager locks =
                    new LockManager();

            RecoveryManager recovery =
                    new RecoveryManager(
                            log
                    );

            recovery.registerTable(
                    "animals",
                    table
            );

            TransactionManager transactions =
                    new TransactionManager(
                            locks,
                            log,
                            recovery
                    );

            TransactionalTable transactional =
                    new TransactionalTable(
                            "animals",
                            table,
                            locks,
                            log
                    );

            Transaction transaction =
                    transactions.begin();

            transactional.update(
                    transaction,
                    recordId,
                    tuple(
                            schema,
                            "Leopard Gecko"
                    )
            );

            List<LogRecord> records =
                    log.readAll();

            assertEquals(
                    1,
                    records.size()
            );

            LogRecord update =
                    records.get(0);

            assertEquals(
                    LogRecordType.UPDATE,
                    update.getType()
            );

            assertEquals(
                    transaction.getId(),
                    update.getTransactionId()
            );

            TupleSerializer serializer =
                    new TupleSerializer();

            Tuple before =
                    serializer.deserialize(
                            schema,
                            update.getBeforeImage()
                    );

            Tuple after =
                    serializer.deserialize(
                            schema,
                            update.getAfterImage()
                    );

            assertEquals(
                    "Gecko",
                    before.getValue(1)
            );

            assertEquals(
                    "Leopard Gecko",
                    after.getValue(1)
            );

            transactions.commit(
                    transaction
            );

            assertEquals(
                    LogRecordType.COMMIT,
                    log.readAll()
                            .get(1)
                            .getType()
            );
        }
    }

    @Test
    void abortRestoresBeforeImage() {

        Path databasePath =
                tempDirectory.resolve(
                        "animals.db"
                );

        Path logPath =
                tempDirectory.resolve(
                        "minidb.log"
                );

        Schema schema =
                schema();

        try (
                DiskManager disk =
                        new DiskManager(
                                databasePath
                        );

                LogManager log =
                        new LogManager(
                                logPath
                        )
        ) {

            Table table =
                    new Table(
                            schema,
                            new HeapFile(
                                    disk
                            )
                    );

            RecordId recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    "Gecko"
                            )
                    );

            LockManager locks =
                    new LockManager();

            RecoveryManager recovery =
                    new RecoveryManager(
                            log
                    );

            recovery.registerTable(
                    "animals",
                    table
            );

            TransactionManager transactions =
                    new TransactionManager(
                            locks,
                            log,
                            recovery
                    );

            TransactionalTable transactional =
                    new TransactionalTable(
                            "animals",
                            table,
                            locks,
                            log
                    );

            Transaction transaction =
                    transactions.begin();

            transactional.update(
                    transaction,
                    recordId,
                    tuple(
                            schema,
                            "Leopard Gecko"
                    )
            );

            assertEquals(
                    "Leopard Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
            );

            transactions.abort(
                    transaction
            );

            /*
             * Atomicity:
             * aborted update is gone.
             */
            assertEquals(
                    "Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
            );

            List<LogRecord> records =
                    log.readAll();

            assertEquals(
                    LogRecordType.ABORT,
                    records
                            .get(
                                    records.size() - 1
                            )
                            .getType()
            );
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
            String species
    ) {

        return new Tuple(
                schema,
                List.of(
                        1,
                        species
                )
        );
    }
}