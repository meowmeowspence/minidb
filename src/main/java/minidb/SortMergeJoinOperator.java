package minidb;

import java.util.ArrayList;
import java.util.List;

public class SortMergeJoinOperator
        implements Operator {

    private final ExternalSortOperator
            leftSorted;

    private final ExternalSortOperator
            rightSorted;

    private final int leftKeyIndex;
    private final int rightKeyIndex;

    private final DataType keyType;

    private final Schema outputSchema;

    private Tuple currentLeft;
    private Tuple currentRight;

    private final List<Tuple> leftGroup;
    private final List<Tuple> rightGroup;

    private int leftGroupIndex;
    private int rightGroupIndex;

    private boolean open;

    public SortMergeJoinOperator(
            Operator left,
            Operator right,
            String leftColumnName,
            String rightColumnName,
            int maxTuplesPerRun
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

        this.leftSorted =
                new ExternalSortOperator(
                        left,
                        leftColumnName,
                        maxTuplesPerRun
                );

        this.rightSorted =
                new ExternalSortOperator(
                        right,
                        rightColumnName,
                        maxTuplesPerRun
                );

        this.leftKeyIndex =
                leftIndex;

        this.rightKeyIndex =
                rightIndex;

        this.keyType =
                leftType;

        this.outputSchema =
                JoinUtils.combineSchemas(
                        leftSchema,
                        rightSchema
                );

        this.leftGroup =
                new ArrayList<>();

        this.rightGroup =
                new ArrayList<>();

        this.leftGroupIndex = 0;
        this.rightGroupIndex = 0;

        this.open = false;
    }

    @Override
    public void open() {

        leftSorted.open();
        rightSorted.open();

        currentLeft =
                leftSorted.next();

        currentRight =
                rightSorted.next();

        clearGroups();

        open = true;
    }

    @Override
    public Tuple next() {

        ensureOpen();

        Tuple groupedResult =
                emitGroupedTuple();

        if (groupedResult != null) {
            return groupedResult;
        }

        while (
                currentLeft != null
                        && currentRight != null
        ) {

            int comparison =
                    compareCurrentKeys();

            if (comparison < 0) {

                /*
                 * Left key is smaller,
                 * so advance left.
                 */
                currentLeft =
                        leftSorted.next();

            } else if (comparison > 0) {

                /*
                 * Right key is smaller,
                 * so advance right.
                 */
                currentRight =
                        rightSorted.next();

            } else {

                /*
                 * Matching join keys.
                 *
                 * Gather all left and right
                 * tuples having this key.
                 */
                gatherMatchingGroups();

                return emitGroupedTuple();
            }
        }

        return null;
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

    @Override
    public void close() {

        leftSorted.close();
        rightSorted.close();

        currentLeft = null;
        currentRight = null;

        clearGroups();

        open = false;
    }

    private void gatherMatchingGroups() {

        clearGroups();

        Object matchingKey =
                currentLeft.getValue(
                        leftKeyIndex
                );

        /*
         * Gather all left tuples having
         * the matching key.
         */
        while (
                currentLeft != null
                        && ValueUtils.compare(
                        keyType,
                        currentLeft.getValue(
                                leftKeyIndex
                        ),
                        matchingKey
                ) == 0
        ) {

            leftGroup.add(
                    currentLeft
            );

            currentLeft =
                    leftSorted.next();
        }

        /*
         * Gather all right tuples having
         * the same matching key.
         */
        while (
                currentRight != null
                        && ValueUtils.compare(
                        keyType,
                        currentRight.getValue(
                                rightKeyIndex
                        ),
                        matchingKey
                ) == 0
        ) {

            rightGroup.add(
                    currentRight
            );

            currentRight =
                    rightSorted.next();
        }

        leftGroupIndex = 0;
        rightGroupIndex = 0;
    }

    private Tuple emitGroupedTuple() {

        if (
                leftGroup.isEmpty()
                        || rightGroup.isEmpty()
                        || leftGroupIndex
                        >= leftGroup.size()
        ) {

            return null;
        }

        Tuple result =
                JoinUtils.combineTuples(
                        leftGroup.get(
                                leftGroupIndex
                        ),
                        rightGroup.get(
                                rightGroupIndex
                        ),
                        outputSchema
                );

        rightGroupIndex++;

        if (
                rightGroupIndex
                        >= rightGroup.size()
        ) {

            rightGroupIndex = 0;
            leftGroupIndex++;
        }

        if (
                leftGroupIndex
                        >= leftGroup.size()
        ) {

            leftGroup.clear();
            rightGroup.clear();

            leftGroupIndex = 0;
            rightGroupIndex = 0;
        }

        return result;
    }

    private int compareCurrentKeys() {

        return ValueUtils.compare(
                keyType,
                currentLeft.getValue(
                        leftKeyIndex
                ),
                currentRight.getValue(
                        rightKeyIndex
                )
        );
    }

    private void clearGroups() {

        leftGroup.clear();
        rightGroup.clear();

        leftGroupIndex = 0;
        rightGroupIndex = 0;
    }

    private void ensureOpen() {

        if (!open) {
            throw new IllegalStateException(
                    "Operator must be opened before reading"
            );
        }
    }
}