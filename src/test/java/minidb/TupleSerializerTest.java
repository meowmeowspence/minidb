package minidb;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TupleSerializerTest {

    @Test
    void tupleCanBeSerializedAndDeserialized() {

        Schema schema = new Schema(
                List.of(
                        new Column("id", DataType.INTEGER),
                        new Column("species", DataType.STRING),
                        new Column("age", DataType.INTEGER),
                        new Column("endangered", DataType.BOOLEAN)
                )
        );

        Tuple original = new Tuple(
                schema,
                List.of(
                        42,
                        "Leopard Gecko",
                        4,
                        false
                )
        );

        TupleSerializer serializer =
                new TupleSerializer();

        byte[] bytes =
                serializer.serialize(original);

        Tuple restored =
                serializer.deserialize(
                        schema,
                        bytes
                );

        assertEquals(42, restored.getValue(0));
        assertEquals(
                "Leopard Gecko",
                restored.getValue(1)
        );
        assertEquals(4, restored.getValue(2));
        assertEquals(false, restored.getValue(3));
    }

}