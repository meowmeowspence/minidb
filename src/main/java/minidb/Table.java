package minidb;

public class Table {

    private final Schema schema;
    private final HeapFile heapFile;
    private final TupleSerializer serializer;

    public Table(
            Schema schema,
            HeapFile heapFile
    ) {

        if (schema == null) {
            throw new IllegalArgumentException(
                    "Schema cannot be null"
            );
        }

        if (heapFile == null) {
            throw new IllegalArgumentException(
                    "Heap file cannot be null"
            );
        }

        this.schema = schema;
        this.heapFile = heapFile;
        this.serializer =
                new TupleSerializer();
    }

    public Schema getSchema() {
        return schema;
    }

    public RecordId insert(
            Tuple tuple
    ) {

        if (tuple == null) {
            throw new IllegalArgumentException(
                    "Tuple cannot be null"
            );
        }

        validateSchema(
                tuple.getSchema()
        );

        byte[] bytes =
                serializer.serialize(
                        tuple
                );

        return heapFile.insert(
                bytes
        );
    }

    public Tuple read(
            RecordId recordId
    ) {

        byte[] bytes =
                heapFile.read(
                        recordId
                );

        return serializer.deserialize(
                schema,
                bytes
        );
    }

    public HeapFileCursor openCursor() {
        return heapFile.openCursor();
    }

    Tuple deserialize(
            byte[] bytes
    ) {

        return serializer.deserialize(
                schema,
                bytes
        );
    }

    private void validateSchema(
            Schema other
    ) {

        if (other.getColumnCount()
                != schema.getColumnCount()) {

            throw new IllegalArgumentException(
                    "Tuple schema does not match table schema"
            );
        }

        for (
                int i = 0;
                i < schema.getColumnCount();
                i++
        ) {

            Column expected =
                    schema.getColumn(i);

            Column actual =
                    other.getColumn(i);

            if (!expected.getName().equals(
                    actual.getName()
            )) {

                throw new IllegalArgumentException(
                        "Tuple schema does not match table schema"
                );
            }

            if (expected.getType()
                    != actual.getType()) {

                throw new IllegalArgumentException(
                        "Tuple schema does not match table schema"
                );
            }
        }
    }

    public int getPageCount() {
        return heapFile.getPageCount();
    }

    public void update(
            RecordId recordId,
            Tuple tuple
    ) {

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Record ID cannot be null"
            );
        }

        if (tuple == null) {
            throw new IllegalArgumentException(
                    "Tuple cannot be null"
            );
        }

        validateSchema(
                tuple.getSchema()
        );

        byte[] bytes =
                serializer.serialize(
                        tuple
                );

        heapFile.update(
                recordId,
                bytes
        );
    }

    byte[] readRaw(
            RecordId recordId
    ) {

        return heapFile.read(
                recordId
        );
    }

    void updateRaw(
            RecordId recordId,
            byte[] bytes
    ) {

        heapFile.update(
                recordId,
                bytes
        );
    }

    byte[] serializeTuple(
            Tuple tuple
    ) {

        if (tuple == null) {
            throw new IllegalArgumentException(
                    "Tuple cannot be null"
            );
        }

        validateSchema(
                tuple.getSchema()
        );

        return serializer.serialize(
                tuple
        );
    }

    void flush() {
        heapFile.flush();
    }

}