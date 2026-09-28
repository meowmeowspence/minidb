package minidb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ColumnTest {

    @Test
    void columnStoresNameAndType() {

        Column column = new Column("age", DataType.INTEGER);

        assertEquals("age", column.getName());
        assertEquals(DataType.INTEGER, column.getType());
    }

    @Test
    void columnRejectsEmptyName() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new Column("", DataType.INTEGER)
        );
    }

}