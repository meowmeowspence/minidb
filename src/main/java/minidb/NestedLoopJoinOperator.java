package minidb;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

public class NestedLoopJoinOperator
        implements Operator {

    private final Operator left;
    private final Operator right;

    private final BiPredicate<Tuple, Tuple>
            predicate;

    private final Schema outputSchema;

    private final List<Tuple>
            rightTuples;

    private Tuple currentLeft;

    private int rightIndex;

    private boolean open;

    public NestedLoopJoinOperator(
            Operator left,
            Operator right,
            BiPredicate<Tuple, Tuple> predicate
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

        if (predicate == null) {
            throw new IllegalArgumentException(
                    "Join predicate cannot be null"
            );
        }

        this.left = left;
        this.right = right;
        this.predicate = predicate;

        this.outputSchema =
                JoinUtils.combineSchemas(
                        left.getOutputSchema(),
                        right.getOutputSchema()
                );

        this.rightTuples =
                new ArrayList<>();

        this.currentLeft = null;
        this.rightIndex = 0;
        this.open = false;
    }

    @Override
    public void open() {

        left.open();
        right.open();

        rightTuples.clear();

        Tuple rightTuple;

        while (
                (rightTuple = right.next())
                        != null
        ) {

            rightTuples.add(
                    rightTuple
            );
        }

        right.close();

        currentLeft =
                left.next();

        rightIndex = 0;

        open = true;
    }

    @Override
    public Tuple next() {

        ensureOpen();

        while (currentLeft != null) {

            while (
                    rightIndex
                            < rightTuples.size()
            ) {

                Tuple rightTuple =
                        rightTuples.get(
                                rightIndex
                        );

                rightIndex++;

                if (
                        predicate.test(
                                currentLeft,
                                rightTuple
                        )
                ) {

                    return JoinUtils.combineTuples(
                            currentLeft,
                            rightTuple,
                            outputSchema
                    );
                }
            }

            /*
             * Finished comparing this left
             * tuple against every right tuple.
             *
             * Move to the next left tuple.
             */
            currentLeft =
                    left.next();

            rightIndex = 0;
        }

        return null;
    }

    @Override
    public Schema getOutputSchema() {
        return outputSchema;
    }

    @Override
    public void close() {

        left.close();
        right.close();

        rightTuples.clear();

        currentLeft = null;
        rightIndex = 0;
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