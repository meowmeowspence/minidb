package minidb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SchemaTest {

    @Test
    void schemaStoresColumns() {

        Schema schema = new Schema(
                List.of(
                        new Column("id", DataType.INTEGER),
                        new Column("species", DataType.STRING),
                        new Column("age", DataType.INTEGER)
                )
        );

        assertEquals(3, schema.getColumnCount());

        assertEquals(
                "species",
                schema.getColumn(1).getName()
        );
    }

    @Test
    void schemaFindsColumnIndex() {

        Schema schema = new Schema(
                List.of(
                        new Column("id", DataType.INTEGER),
                        new Column("species", DataType.STRING)
                )
        );

        assertEquals(1, schema.indexOf("species"));
        assertEquals(-1, schema.indexOf("missing"));
    }

}