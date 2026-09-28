package minidb;

import java.nio.file.Path;
import java.util.List;

public class MiniDB {

    public static void main(String[] args) {

        Schema schema = new Schema(
                List.of(
                        new Column(
                                "id",
                                DataType.INTEGER
                        ),
                        new Column(
                                "species",
                                DataType.STRING
                        ),
                        new Column(
                                "age",
                                DataType.INTEGER
                        )
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

        TupleSerializer serializer =
                new TupleSerializer();

        byte[] tupleBytes =
                serializer.serialize(tuple);

        Path databasePath =
                Path.of("animals.db");

        try (DiskManager disk =
                     new DiskManager(databasePath)) {

            Page page =
                    disk.allocatePage();

            page.write(
                    0,
                    tupleBytes
            );

            disk.writePage(page);

            System.out.println(
                    "Wrote tuple to page "
                            + page.getPageId()
            );

            System.out.println(
                    "Database pages: "
                            + disk.getPageCount()
            );
        }
    }

}