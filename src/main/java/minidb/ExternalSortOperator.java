package minidb;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExternalSortOperator
        implements Operator {

    private final Operator child;

    private final Schema outputSchema;

    private final int sortColumnIndex;

    private final DataType sortType;

    private final int maxTuplesPerRun;

    private final TupleSerializer serializer;

    private Path tempDirectory;

    private DataInputStream mergedInput;

    private boolean open;

    public ExternalSortOperator(
            Operator child,
            String sortColumnName,
            int maxTuplesPerRun
    ) {

        if (child == null) {
            throw new IllegalArgumentException(
                    "Child operator cannot be null"
            );
        }

        if (maxTuplesPerRun < 1) {
            throw new IllegalArgumentException(
                    "Run capacity must be at least 1"
            );
        }

        Schema schema =
                child.getOutputSchema();

        int columnIndex =
                schema.indexOf(
                        sortColumnName
                );

        if (columnIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown sort column: "
                            + sortColumnName
            );
        }

        this.child = child;
        this.outputSchema = schema;

        this.sortColumnIndex =
                columnIndex;

        this.sortType =
                schema
                        .getColumn(columnIndex)
                        .getType();

        this.maxTuplesPerRun =
                maxTuplesPerRun;

        this.serializer =
                new TupleSerializer();

        this.tempDirectory = null;
        this.mergedInput = null;
        this.open = false;
    }

    @Override
    public void open() {

        if (open) {
            throw new IllegalStateException(
                    "Sort operator is already open"
            );
        }

        try {

            tempDirectory =
                    Files.createTempDirectory(
                            "minidb-sort-"
                    );

            child.open();

            List<Path> runs =
                    generateInitialRuns();

            child.close();

            Path finalRun;

            if (runs.isEmpty()) {

                finalRun =
                        Files.createTempFile(
                                tempDirectory,
                                "empty-",
                                ".bin"
                        );

            } else {

                finalRun =
                        mergeRuns(runs);
            }

            mergedInput =
                    new DataInputStream(
                            new BufferedInputStream(
                                    Files.newInputStream(
                                            finalRun
                                    )
                            )
                    );

            open = true;

        } catch (IOException e) {

            child.close();

            deleteTemporaryFiles();

            throw new IllegalStateException(
                    "Failed to perform external sort",
                    e
            );
        }
    }

    @Override
    public Tuple next() {

        ensureOpen();

        try {
            return readTuple(
                    mergedInput
            );

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to read sorted tuple",
                    e
            );
        }
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

    @Override
    public void close() {

        try {

            if (mergedInput != null) {
                mergedInput.close();
            }

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to close sort output",
                    e
            );

        } finally {

            mergedInput = null;

            child.close();

            deleteTemporaryFiles();

            open = false;
        }
    }

    private List<Path> generateInitialRuns()
            throws IOException {

        List<Path> runs =
                new ArrayList<>();

        List<Tuple> buffer =
                new ArrayList<>();

        Tuple tuple;

        while (
                (tuple = child.next())
                        != null
        ) {

            buffer.add(tuple);

            if (buffer.size()
                    == maxTuplesPerRun) {

                runs.add(
                        writeSortedRun(
                                buffer
                        )
                );

                buffer.clear();
            }
        }

        if (!buffer.isEmpty()) {

            runs.add(
                    writeSortedRun(
                            buffer
                    )
            );
        }

        return runs;
    }

    private Path writeSortedRun(
            List<Tuple> tuples
    ) throws IOException {

        tuples.sort(
                this::compareTuples
        );

        Path run =
                Files.createTempFile(
                        tempDirectory,
                        "run-",
                        ".bin"
                );

        try (
                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                run
                                        )
                                )
                        )
        ) {

            for (Tuple tuple : tuples) {
                writeTuple(
                        output,
                        tuple
                );
            }
        }

        return run;
    }

    private Path mergeRuns(
            List<Path> originalRuns
    ) throws IOException {

        List<Path> currentRuns =
                new ArrayList<>(
                        originalRuns
                );

        while (currentRuns.size() > 1) {

            List<Path> nextRuns =
                    new ArrayList<>();

            for (
                    int i = 0;
                    i < currentRuns.size();
                    i += 2
            ) {

                if (
                        i + 1
                                >= currentRuns.size()
                ) {

                    nextRuns.add(
                            currentRuns.get(i)
                    );

                } else {

                    nextRuns.add(
                            mergePair(
                                    currentRuns.get(i),
                                    currentRuns.get(i + 1)
                            )
                    );
                }
            }

            currentRuns =
                    nextRuns;
        }

        return currentRuns.get(0);
    }

    private Path mergePair(
            Path leftRun,
            Path rightRun
    ) throws IOException {

        Path merged =
                Files.createTempFile(
                        tempDirectory,
                        "merge-",
                        ".bin"
                );

        try (
                DataInputStream leftInput =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                leftRun
                                        )
                                )
                        );

                DataInputStream rightInput =
                        new DataInputStream(
                                new BufferedInputStream(
                                        Files.newInputStream(
                                                rightRun
                                        )
                                )
                        );

                DataOutputStream output =
                        new DataOutputStream(
                                new BufferedOutputStream(
                                        Files.newOutputStream(
                                                merged
                                        )
                                )
                        )
        ) {

            Tuple leftTuple =
                    readTuple(leftInput);

            Tuple rightTuple =
                    readTuple(rightInput);

            while (
                    leftTuple != null
                            && rightTuple != null
            ) {

                if (
                        compareTuples(
                                leftTuple,
                                rightTuple
                        ) <= 0
                ) {

                    writeTuple(
                            output,
                            leftTuple
                    );

                    leftTuple =
                            readTuple(
                                    leftInput
                            );

                } else {

                    writeTuple(
                            output,
                            rightTuple
                    );

                    rightTuple =
                            readTuple(
                                    rightInput
                            );
                }
            }

            while (leftTuple != null) {

                writeTuple(
                        output,
                        leftTuple
                );

                leftTuple =
                        readTuple(
                                leftInput
                        );
            }

            while (rightTuple != null) {

                writeTuple(
                        output,
                        rightTuple
                );

                rightTuple =
                        readTuple(
                                rightInput
                        );
            }
        }

        return merged;
    }

    private int compareTuples(
            Tuple left,
            Tuple right
    ) {

        return ValueUtils.compare(
                sortType,
                left.getValue(
                        sortColumnIndex
                ),
                right.getValue(
                        sortColumnIndex
                )
        );
    }

    private void writeTuple(
            DataOutputStream output,
            Tuple tuple
    ) throws IOException {

        byte[] bytes =
                serializer.serialize(
                        tuple
                );

        /*
         * Tuple records in our temporary
         * sort files are stored as:
         *
         * [4-byte length][tuple bytes]
         */
        output.writeInt(
                bytes.length
        );

        output.write(bytes);
    }

    private Tuple readTuple(
            DataInputStream input
    ) throws IOException {

        final int length;

        try {

            length =
                    input.readInt();

        } catch (EOFException e) {

            return null;
        }

        if (length < 0) {
            throw new IllegalStateException(
                    "Invalid tuple length in sort file"
            );
        }

        byte[] bytes =
                new byte[length];

        input.readFully(bytes);

        return serializer.deserialize(
                outputSchema,
                bytes
        );
    }

    private void ensureOpen() {

        if (!open) {
            throw new IllegalStateException(
                    "Operator must be opened before reading"
            );
        }
    }

    private void deleteTemporaryFiles() {

        if (
                tempDirectory == null
                        || !Files.exists(
                        tempDirectory
                )
        ) {
            return;
        }

        try (
                var paths =
                        Files.walk(
                                tempDirectory
                        )
        ) {

            paths.sorted(
                    Comparator.reverseOrder()
            ).forEach(
                    path -> {

                        try {
                            Files.deleteIfExists(
                                    path
                            );

                        } catch (IOException e) {

                            throw new IllegalStateException(
                                    "Failed to delete temporary sort file",
                                    e
                            );
                        }
                    }
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to clean temporary sort files",
                    e
            );

        } finally {

            tempDirectory = null;
        }
    }
}