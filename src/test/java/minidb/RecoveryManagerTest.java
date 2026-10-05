package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecoveryManagerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void recoveryUndoesUncommittedUpdate() {

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

        RecordId recordId;

        /*
         * SESSION 1
         *
         * Make an update but never commit.
         */
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

            recordId =
                    table.insert(
                            tuple(
                                    schema,
                                    "Gecko"
                            )
                    );

            disk.flush();

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

            /*
             * Force the changed page to mimic
             * a STEAL database buffer policy.
             *
             * Then simulate a crash by ending
             * the session without COMMIT/ABORT.
             */
            disk.flush();
        }

        /*
         * SESSION 2
         *
         * Reopen database and WAL.
         */
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

            /*
             * Before recovery, the uncommitted
             * value really is on disk.
             */
            assertEquals(
                    "Leopard Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
            );

            RecoveryManager recovery =
                    new RecoveryManager(
                            log
                    );

            recovery.registerTable(
                    "animals",
                    table
            );

            recovery.recover();

            /*
             * No COMMIT existed, so recovery
             * must restore the before-image.
             */
            assertEquals(
                    "Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
            );
        }
    }

    @Test
    void recoveryRedoesCommittedUpdate() {

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

        RecordId recordId;

        /*
         * SESSION 1
         *
         * Commit an update.
         */
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

            Tuple original =
                    tuple(
                            schema,
                            "Gecko"
                    );

            recordId =
                    table.insert(
                            original
                    );

            disk.flush();

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

            transactions.commit(
                    transaction
            );

            /*
             * Simulate a committed log record
             * surviving while the database
             * page itself contains an older
             * version.
             *
             * This deliberately recreates the
             * state REDO is designed to fix.
             */
            table.update(
                    recordId,
                    original
            );

            disk.flush();

            assertEquals(
                    "Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
            );
        }

        /*
         * SESSION 2
         *
         * Recovery sees COMMIT and reapplies
         * the after-image.
         */
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

            RecoveryManager recovery =
                    new RecoveryManager(
                            log
                    );

            recovery.registerTable(
                    "animals",
                    table
            );

            recovery.recover();

            assertEquals(
                    "Leopard Gecko",
                    table.read(
                            recordId
                    ).getValue(1)
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