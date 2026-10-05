package minidb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.List;

public class LogManager
        implements AutoCloseable {

    private final Path path;

    private final RandomAccessFile file;

    private long nextLsn;

    private long maxTransactionId;

    public LogManager(
            Path path
    ) {

        if (path == null) {
            throw new IllegalArgumentException(
                    "Log path cannot be null"
            );
        }

        this.path = path;

        try {

            Path parent =
                    path
                            .toAbsolutePath()
                            .getParent();

            if (parent != null) {
                Files.createDirectories(
                        parent
                );
            }

            this.file =
                    new RandomAccessFile(
                            path.toFile(),
                            "rw"
                    );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to open write-ahead log",
                    e
            );
        }

        List<LogRecord> existing =
                readAll();

        long highestLsn = 0;
        long highestTransactionId = 0;

        for (LogRecord record
                : existing) {

            highestLsn =
                    Math.max(
                            highestLsn,
                            record.getLsn()
                    );

            highestTransactionId =
                    Math.max(
                            highestTransactionId,
                            record.getTransactionId()
                    );
        }

        this.nextLsn =
                highestLsn + 1;

        this.maxTransactionId =
                highestTransactionId;
    }

    public synchronized LogRecord appendUpdate(
            long transactionId,
            String tableName,
            RecordId recordId,
            byte[] beforeImage,
            byte[] afterImage
    ) {

        LogRecord record =
                new LogRecord(
                        nextLsn++,
                        transactionId,
                        LogRecordType.UPDATE,
                        tableName,
                        recordId,
                        beforeImage,
                        afterImage
                );

        append(
                record
        );

        return record;
    }

    public synchronized LogRecord appendCommit(
            long transactionId
    ) {

        LogRecord record =
                new LogRecord(
                        nextLsn++,
                        transactionId,
                        LogRecordType.COMMIT,
                        null,
                        null,
                        null,
                        null
                );

        append(
                record
        );

        return record;
    }

    public synchronized LogRecord appendAbort(
            long transactionId
    ) {

        LogRecord record =
                new LogRecord(
                        nextLsn++,
                        transactionId,
                        LogRecordType.ABORT,
                        null,
                        null,
                        null,
                        null
                );

        append(
                record
        );

        return record;
    }

    public synchronized void flush() {

        try {

            file.getFD().sync();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to flush write-ahead log",
                    e
            );
        }
    }

    public synchronized List<LogRecord> readAll() {

        List<LogRecord> records =
                new ArrayList<>();

        if (!Files.exists(path)) {
            return List.of();
        }

        try (
                DataInputStream input =
                        new DataInputStream(
                                Files.newInputStream(
                                        path
                                )
                        )
        ) {

            while (true) {

                final int recordLength;

                try {

                    recordLength =
                            input.readInt();

                } catch (EOFException e) {

                    break;
                }

                if (recordLength <= 0) {

                    throw new IllegalStateException(
                            "Invalid WAL record length"
                    );
                }

                byte[] payload =
                        input.readNBytes(
                                recordLength
                        );

                /*
                 * A crash may leave a partial
                 * final record.
                 *
                 * Ignore that incomplete tail.
                 */
                if (
                        payload.length
                                != recordLength
                ) {

                    break;
                }

                records.add(
                        decode(
                                payload
                        )
                );
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to read write-ahead log",
                    e
            );
        }

        return List.copyOf(
                records
        );
    }

    public synchronized long getMaxTransactionId() {
        return maxTransactionId;
    }

    private void append(
            LogRecord record
    ) {

        byte[] payload =
                encode(
                        record
                );

        try {

            file.seek(
                    file.length()
            );

            /*
             * WAL record format:
             *
             * [4-byte record length]
             * [record payload]
             */
            file.writeInt(
                    payload.length
            );

            file.write(
                    payload
            );

            maxTransactionId =
                    Math.max(
                            maxTransactionId,
                            record.getTransactionId()
                    );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to append WAL record",
                    e
            );
        }
    }

    private byte[] encode(
            LogRecord record
    ) {

        try (
                ByteArrayOutputStream bytes =
                        new ByteArrayOutputStream();

                DataOutputStream output =
                        new DataOutputStream(
                                bytes
                        )
        ) {

            output.writeInt(
                    record
                            .getType()
                            .getCode()
            );

            output.writeLong(
                    record.getLsn()
            );

            output.writeLong(
                    record.getTransactionId()
            );

            if (record.isUpdate()) {

                output.writeUTF(
                        record.getTableName()
                );

                output.writeInt(
                        record
                                .getRecordId()
                                .getPageId()
                );

                output.writeInt(
                        record
                                .getRecordId()
                                .getSlotId()
                );

                byte[] before =
                        record.getBeforeImage();

                byte[] after =
                        record.getAfterImage();

                output.writeInt(
                        before.length
                );

                output.write(
                        before
                );

                output.writeInt(
                        after.length
                );

                output.write(
                        after
                );
            }

            output.flush();

            return bytes.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to encode WAL record",
                    e
            );
        }
    }

    private LogRecord decode(
            byte[] payload
    ) {

        try (
                DataInputStream input =
                        new DataInputStream(
                                new ByteArrayInputStream(
                                        payload
                                )
                        )
        ) {

            LogRecordType type =
                    LogRecordType.fromCode(
                            input.readInt()
                    );

            long lsn =
                    input.readLong();

            long transactionId =
                    input.readLong();

            if (type != LogRecordType.UPDATE) {

                return new LogRecord(
                        lsn,
                        transactionId,
                        type,
                        null,
                        null,
                        null,
                        null
                );
            }

            String tableName =
                    input.readUTF();

            int pageId =
                    input.readInt();

            int slotId =
                    input.readInt();

            int beforeLength =
                    input.readInt();

            if (beforeLength < 0) {
                throw new IllegalStateException(
                        "Invalid WAL before-image length"
                );
            }

            byte[] before =
                    new byte[beforeLength];

            input.readFully(
                    before
            );

            int afterLength =
                    input.readInt();

            if (afterLength < 0) {
                throw new IllegalStateException(
                        "Invalid WAL after-image length"
                );
            }

            byte[] after =
                    new byte[afterLength];

            input.readFully(
                    after
            );

            return new LogRecord(
                    lsn,
                    transactionId,
                    type,
                    tableName,
                    new RecordId(
                            pageId,
                            slotId
                    ),
                    before,
                    after
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Corrupt WAL record",
                    e
            );
        }
    }

    @Override
    public synchronized void close() {

        try {

            flush();

            file.close();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to close WAL",
                    e
            );
        }
    }
}