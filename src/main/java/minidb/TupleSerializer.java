package minidb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TupleSerializer {

    public byte[] serialize(Tuple tuple) {

        try {
            ByteArrayOutputStream byteStream =
                    new ByteArrayOutputStream();

            DataOutputStream output =
                    new DataOutputStream(byteStream);

            Schema schema = tuple.getSchema();

            for (int i = 0; i < schema.getColumnCount(); i++) {

                DataType type =
                        schema.getColumn(i).getType();

                Object value =
                        tuple.getValue(i);

                switch (type) {

                    case INTEGER ->
                            output.writeInt((Integer) value);

                    case LONG ->
                            output.writeLong((Long) value);

                    case BOOLEAN ->
                            output.writeBoolean((Boolean) value);

                    case STRING -> {
                        byte[] stringBytes =
                                ((String) value)
                                        .getBytes(StandardCharsets.UTF_8);

                        output.writeInt(stringBytes.length);
                        output.write(stringBytes);
                    }
                }
            }

            output.flush();

            return byteStream.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to serialize tuple",
                    e
            );
        }
    }

    public Tuple deserialize(
            Schema schema,
            byte[] bytes
    ) {

        try {
            ByteArrayInputStream byteStream =
                    new ByteArrayInputStream(bytes);

            DataInputStream input =
                    new DataInputStream(byteStream);

            List<Object> values =
                    new ArrayList<>();

            for (int i = 0; i < schema.getColumnCount(); i++) {

                DataType type =
                        schema.getColumn(i).getType();

                Object value = switch (type) {

                    case INTEGER ->
                            input.readInt();

                    case LONG ->
                            input.readLong();

                    case BOOLEAN ->
                            input.readBoolean();

                    case STRING -> {
                        int length =
                                input.readInt();

                        if (length < 0) {
                            throw new IllegalStateException(
                                    "Invalid string length"
                            );
                        }

                        byte[] stringBytes =
                                new byte[length];

                        input.readFully(stringBytes);

                        yield new String(
                                stringBytes,
                                StandardCharsets.UTF_8
                        );
                    }
                };

                values.add(value);
            }

            return new Tuple(schema, values);

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to deserialize tuple",
                    e
            );
        }
    }

}