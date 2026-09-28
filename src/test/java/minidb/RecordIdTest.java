package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecordIdTest {

    @Test
    void recordIdsWithSameLocationAreEqual() {

        RecordId first =
                new RecordId(4, 7);

        RecordId second =
                new RecordId(4, 7);

        assertEquals(
                first,
                second
        );

        assertEquals(
                first.hashCode(),
                second.hashCode()
        );
    }
}