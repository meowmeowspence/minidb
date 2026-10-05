package minidb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecoveryManager {

    private final LogManager logManager;

    private final Map<String, Table>
            tables;

    public RecoveryManager(
            LogManager logManager
    ) {

        if (logManager == null) {
            throw new IllegalArgumentException(
                    "Log manager cannot be null"
            );
        }

        this.logManager =
                logManager;

        this.tables =
                new HashMap<>();
    }

    public void registerTable(
            String tableName,
            Table table
    ) {

        if (
                tableName == null
                        || tableName.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "Table name cannot be empty"
            );
        }

        if (table == null) {
            throw new IllegalArgumentException(
                    "Table cannot be null"
            );
        }

        tables.put(
                tableName,
                table
        );
    }

    public void undoTransaction(
            long transactionId
    ) {

        List<LogRecord> records =
                logManager.readAll();

        Set<Table> changedTables =
                new HashSet<>();

        for (
                int i = records.size() - 1;
                i >= 0;
                i--
        ) {

            LogRecord record =
                    records.get(i);

            if (
                    record.getTransactionId()
                            != transactionId
                            || !record.isUpdate()
            ) {

                continue;
            }

            Table table =
                    tableFor(
                            record
                    );

            table.updateRaw(
                    record.getRecordId(),
                    record.getBeforeImage()
            );

            changedTables.add(
                    table
            );
        }

        flushTables(
                changedTables
        );
    }

    public void recover() {

        List<LogRecord> records =
                logManager.readAll();

        Set<Long> committed =
                new HashSet<>();

        Set<Long> aborted =
                new HashSet<>();

        Set<Long> transactionsWithUpdates =
                new HashSet<>();

        for (LogRecord record
                : records) {

            if (record.isUpdate()) {

                transactionsWithUpdates.add(
                        record.getTransactionId()
                );

            } else if (
                    record.getType()
                            == LogRecordType.COMMIT
            ) {

                committed.add(
                        record.getTransactionId()
                );

            } else if (
                    record.getType()
                            == LogRecordType.ABORT
            ) {

                aborted.add(
                        record.getTransactionId()
                );
            }
        }

        Set<Table> changedTables =
                new HashSet<>();

        /*
         * REDO committed transactions in
         * forward log order.
         */
        for (LogRecord record
                : records) {

            if (
                    record.isUpdate()
                            && committed.contains(
                            record.getTransactionId()
                    )
            ) {

                Table table =
                        tableFor(
                                record
                        );

                table.updateRaw(
                        record.getRecordId(),
                        record.getAfterImage()
                );

                changedTables.add(
                        table
                );
            }
        }

        /*
         * Transactions with UPDATE records
         * but neither COMMIT nor ABORT are
         * crash losers.
         */
        Set<Long> losers =
                new HashSet<>(
                        transactionsWithUpdates
                );

        losers.removeAll(
                committed
        );

        losers.removeAll(
                aborted
        );

        /*
         * UNDO loser transactions in reverse
         * log order.
         */
        for (
                int i = records.size() - 1;
                i >= 0;
                i--
        ) {

            LogRecord record =
                    records.get(i);

            if (
                    record.isUpdate()
                            && losers.contains(
                            record.getTransactionId()
                    )
            ) {

                Table table =
                        tableFor(
                                record
                        );

                table.updateRaw(
                        record.getRecordId(),
                        record.getBeforeImage()
                );

                changedTables.add(
                        table
                );
            }
        }

        /*
         * Make REDO/UNDO page changes durable
         * before recording recovery-completed
         * ABORT markers.
         */
        flushTables(
                changedTables
        );

        if (!losers.isEmpty()) {

            for (long transactionId
                    : losers) {

                logManager.appendAbort(
                        transactionId
                );
            }

            logManager.flush();
        }
    }

    private Table tableFor(
            LogRecord record
    ) {

        Table table =
                tables.get(
                        record.getTableName()
                );

        if (table == null) {

            throw new IllegalStateException(
                    "No table registered for WAL name: "
                            + record.getTableName()
            );
        }

        return table;
    }

    private void flushTables(
            Set<Table> changedTables
    ) {

        for (Table table
                : changedTables) {

            table.flush();
        }
    }
}