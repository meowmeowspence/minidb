package minidb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogManagerTest {

    @TempDir
    Path tempDirectory;

    @Test
    void logRecordsSurviveReopen() {

        Path path =
                tempDirectory.resolve(
                        "minidb.log"
                );

        RecordId recordId =
                new RecordId(
                        2,
                        4
                );

        try (
                LogManager log =
                        new LogManager(path)
        ) {

            log.appendUpdate(
                    1,
                    "animals",
                    recordId,
                    new byte[] {
                            1,
                            2
                    },
                    new byte[] {
                            9,
                            8,
                            7
                    }
            );

            log.appendCommit(
                    1
            );

            log.flush();
        }

        try (
                LogManager reopened =
                        new LogManager(path)
        ) {

            List<LogRecord> records =
                    reopened.readAll();

            assertEquals(
                    2,
                    records.size()
            );

            LogRecord update =
                    records.get(0);

            assertEquals(
                    LogRecordType.UPDATE,
                    update.getType()
            );

            assertEquals(
                    1,
                    update.getLsn()
            );

            assertEquals(
                    "animals",
                    update.getTableName()
            );

            assertEquals(
                    recordId,
                    update.getRecordId()
            );

            assertArrayEquals(
                    new byte[] {
                            1,
                            2
                    },
                    update.getBeforeImage()
            );

            assertArrayEquals(
                    new byte[] {
                            9,
                            8,
                            7
                    },
                    update.getAfterImage()
            );

            assertEquals(
                    LogRecordType.COMMIT,
                    records.get(1).getType()
            );
        }
    }

    @Test
    void logSequenceNumbersIncrease() {

        Path path =
                tempDirectory.resolve(
                        "minidb.log"
                );

        try (
                LogManager log =
                        new LogManager(path)
        ) {

            LogRecord first =
                    log.appendCommit(
                            1
                    );

            LogRecord second =
                    log.appendCommit(
                            2
                    );

            assertTrue(
                    second.getLsn()
                            > first.getLsn()
            );
        }
    }

    @Test
    void transactionIdsContinuePastExistingLog() {

        Path path =
                tempDirectory.resolve(
                        "minidb.log"
                );

        try (
                LogManager log =
                        new LogManager(path)
        ) {

            log.appendCommit(
                    41
            );

            log.flush();
        }

        try (
                LogManager log =
                        new LogManager(path)
        ) {

            LockManager locks =
                    new LockManager();

            RecoveryManager recovery =
                    new RecoveryManager(
                            log
                    );

            TransactionManager manager =
                    new TransactionManager(
                            locks,
                            log,
                            recovery
                    );

            Transaction transaction =
                    manager.begin();

            assertEquals(
                    42,
                    transaction.getId()
            );
        }
    }
}