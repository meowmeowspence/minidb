package minidb;

final class ValueUtils {

    private ValueUtils() {
    }

    static int compare(
            DataType type,
            Object left,
            Object right
    ) {

        return switch (type) {

            case INTEGER ->
                    Integer.compare(
                            (Integer) left,
                            (Integer) right
                    );

            case LONG ->
                    Long.compare(
                            (Long) left,
                            (Long) right
                    );

            case BOOLEAN ->
                    Boolean.compare(
                            (Boolean) left,
                            (Boolean) right
                    );

            case STRING ->
                    ((String) left)
                            .compareTo(
                                    (String) right
                            );
        };
    }
}