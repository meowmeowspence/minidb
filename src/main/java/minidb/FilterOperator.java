package minidb;

import java.util.function.Predicate;

public class FilterOperator
        implements Operator {

    private final Operator child;
    private final Predicate<Tuple> predicate;

    public FilterOperator(
            Operator child,
            Predicate<Tuple> predicate
    ) {

        if (child == null) {
            throw new IllegalArgumentException(
                    "Child operator cannot be null"
            );
        }

        if (predicate == null) {
            throw new IllegalArgumentException(
                    "Predicate cannot be null"
            );
        }

        this.child = child;
        this.predicate = predicate;
    }

    @Override
    public void open() {
        child.open();
    }

    @Override
    public Tuple next() {

        while (true) {

            Tuple tuple =
                    child.next();

            if (tuple == null) {
                return null;
            }

            if (predicate.test(tuple)) {
                return tuple;
            }
        }
    }

    @Override
    public Schema getOutputSchema() {
        return child.getOutputSchema();
    }

    @Override
    public void close() {
        child.close();
    }
}