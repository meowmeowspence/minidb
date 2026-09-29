package minidb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HashJoinOperator
        implements Operator {

    private final Operator left;
    private final Operator right;

    private final int leftKeyIndex;
    private final int rightKeyIndex;

    private final Schema outputSchema;

    private final Map<Object, List<Tuple>>
            hashTable;

    private Tuple currentLeft;

    private List<Tuple> currentMatches;

    private int currentMatchIndex;

    private boolean open;

    public HashJoinOperator(
            Operator left,
            Operator right,
            String leftColumnName,
            String rightColumnName
    ) {

        if (left == null) {
            throw new IllegalArgumentException(
                    "Left operator cannot be null"
            );
        }

        if (right == null) {
            throw new IllegalArgumentException(
                    "Right operator cannot be null"
            );
        }

        Schema leftSchema =
                left.getOutputSchema();

        Schema rightSchema =
                right.getOutputSchema();

        int leftIndex =
                leftSchema.indexOf(
                        leftColumnName
                );

        int rightIndex =
                rightSchema.indexOf(
                        rightColumnName
                );

        if (leftIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown left join column: "
                            + leftColumnName
            );
        }

        if (rightIndex < 0) {
            throw new IllegalArgumentException(
                    "Unknown right join column: "
                            + rightColumnName
            );
        }

        DataType leftType =
                leftSchema
                        .getColumn(leftIndex)
                        .getType();

        DataType rightType =
                rightSchema
                        .getColumn(rightIndex)
                        .getType();

        if (leftType != rightType) {
            throw new IllegalArgumentException(
                    "Join columns must have matching types"
            );
        }

        this.left = left;
        this.right = right;

        this.leftKeyIndex =
                leftIndex;

        this.rightKeyIndex =
                rightIndex;

        this.outputSchema =
                JoinUtils.combineSchemas(
                        leftSchema,
                        rightSchema
                );

        this.hashTable =
                new HashMap<>();

        this.currentLeft = null;
        this.currentMatches = null;
        this.currentMatchIndex = 0;
        this.open = false;
    }

    @Override
    public void open() {

        left.open();
        right.open();

        hashTable.clear();

        /*
         * BUILD PHASE
         *
         * Read the right side and construct:
         *
         * join key -> matching right tuples
         */
        Tuple rightTuple;

        while (
                (rightTuple = right.next())
                        != null
        ) {

            Object key =
                    rightTuple.getValue(
                            rightKeyIndex
                    );

            hashTable
                    .computeIfAbsent(
                            key,
                            ignored ->
                                    new ArrayList<>()
                    )
                    .add(
                            rightTuple
                    );
        }

        right.close();

        /*
         * PROBE PHASE begins when next()
         * starts reading the left input.
         */
        currentLeft = null;
        currentMatches = null;
        currentMatchIndex = 0;

        open = true;
    }

    @Override
    public Tuple next() {

        ensureOpen();

        while (true) {

            /*
             * If the current left tuple has
             * more matches, return the next
             * joined tuple.
             */
            if (
                    currentMatches != null
                            && currentMatchIndex
                            < currentMatches.size()
            ) {

                Tuple rightTuple =
                        currentMatches.get(
                                currentMatchIndex
                        );

                currentMatchIndex++;

                return JoinUtils.combineTuples(
                        currentLeft,
                        rightTuple,
                        outputSchema
                );
            }

            /*
             * Otherwise get another left
             * tuple and probe the hash table.
             */
            currentLeft =
                    left.next();

            if (currentLeft == null) {
                return null;
            }

            Object key =
                    currentLeft.getValue(
                            leftKeyIndex
                    );

            currentMatches =
                    hashTable.get(key);

            currentMatchIndex = 0;

            /*
             * If no right tuples have this
             * key, loop and get another
             * left tuple.
             */
        }
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

    @Override
    public void close() {

        left.close();
        right.close();

        hashTable.clear();

        currentLeft = null;
        currentMatches = null;
        currentMatchIndex = 0;
        open = false;
    }

    private void ensureOpen() {

        if (!open) {
            throw new IllegalStateException(
                    "Operator must be opened before reading"
            );
        }
    }
}