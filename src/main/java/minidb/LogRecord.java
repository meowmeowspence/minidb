package minidb;

public final class LogRecord {

    private final long lsn;

    private final long transactionId;

    private final LogRecordType type;

    private final String tableName;

    private final RecordId recordId;

    private final byte[] beforeImage;

    private final byte[] afterImage;

    LogRecord(
            long lsn,
            long transactionId,
            LogRecordType type,
            String tableName,
            RecordId recordId,
            byte[] beforeImage,
            byte[] afterImage
    ) {

        if (lsn < 1) {
            throw new IllegalArgumentException(
                    "LSN must be positive"
            );
        }

        if (transactionId < 1) {
            throw new IllegalArgumentException(
                    "Transaction ID must be positive"
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Log record type cannot be null"
            );
        }

        if (type == LogRecordType.UPDATE) {

            if (
                    tableName == null
                            || tableName.isBlank()
            ) {

                throw new IllegalArgumentException(
                        "Update log record must contain a table name"
                );
            }

            if (recordId == null) {
                throw new IllegalArgumentException(
                        "Update log record must contain a RecordId"
                );
            }

            if (
                    beforeImage == null
                            || afterImage == null
            ) {

                throw new IllegalArgumentException(
                        "Update log record must contain before and after images"
                );
            }
        }

        this.lsn = lsn;
        this.transactionId =
                transactionId;
        this.type = type;
        this.tableName = tableName;
        this.recordId = recordId;

        this.beforeImage =
                beforeImage == null
                        ? null
                        : beforeImage.clone();

        this.afterImage =
                afterImage == null
                        ? null
                        : afterImage.clone();
    }

    public long getLsn() {
        return lsn;
    }

    public long getTransactionId() {
        return transactionId;
    }

    public LogRecordType getType() {
        return type;
    }

    public String getTableName() {
        return tableName;
    }

    public RecordId getRecordId() {
        return recordId;
    }

    public byte[] getBeforeImage() {

        return beforeImage == null
                ? null
                : beforeImage.clone();
    }

    public byte[] getAfterImage() {

        return afterImage == null
                ? null
                : afterImage.clone();
    }

    public boolean isUpdate() {
        return type == LogRecordType.UPDATE;
    }
}