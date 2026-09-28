package minidb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TupleTest {

    @Test
    void tupleStoresValues() {

        Schema schema = new Schema(
                List.of(
                        new Column("id", DataType.INTEGER),
                        new Column("species", DataType.STRING),
                        new Column("age", DataType.INTEGER)
                )
        );

        Tuple tuple = new Tuple(
                schema,
                List.of(
                        1,
                        "Leopard Gecko",
                        4
                )
        );

        assertEquals(3, tuple.getValueCount());
        assertEquals(1, tuple.getValue(0));
        assertEquals("Leopard Gecko", tuple.getValue(1));
        assertEquals(4, tuple.getValue(2));
    }

    @Test
    void tupleRejectsWrongNumberOfValues() {

        Schema schema = new Schema(
                List.of(
                        new Column("id", DataType.INTEGER),
                        new Column("species", DataType.STRING)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Tuple(
                        schema,
                        List.of(1)
                )
        );
    }

    @Test
    void tupleRejectsWrongValueType() {

        Schema schema = new Schema(
                List.of(
                        new Column("age", DataType.INTEGER)
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new Tuple(
                        schema,
                        List.of("four")
                )
        );
    }

}