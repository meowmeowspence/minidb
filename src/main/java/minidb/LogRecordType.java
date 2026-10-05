package minidb;

public enum LogRecordType {

    UPDATE(1),
    COMMIT(2),
    ABORT(3);

    private final int code;

    LogRecordType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    static LogRecordType fromCode(
            int code
    ) {

        for (LogRecordType type : values()) {

            if (type.code == code) {
                return type;
            }
        }

        throw new IllegalArgumentException(
                "Unknown log record type: "
                        + code
        );
    }
}