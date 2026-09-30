package minidb;

public class OptimizedPlan {

    private final Operator operator;
    private final AccessPath accessPath;
    private final long estimatedCost;

    public OptimizedPlan(
            Operator operator,
            AccessPath accessPath,
            long estimatedCost
    ) {

        if (operator == null) {
            throw new IllegalArgumentException(
                    "Operator cannot be null"
            );
        }

        if (accessPath == null) {
            throw new IllegalArgumentException(
                    "Access path cannot be null"
            );
        }

        if (estimatedCost < 0) {
            throw new IllegalArgumentException(
                    "Estimated cost cannot be negative"
            );
        }

        this.operator = operator;
        this.accessPath = accessPath;
        this.estimatedCost = estimatedCost;
    }

    public Operator getOperator() {
        return operator;
    }

    public AccessPath getAccessPath() {
        return accessPath;
    }

    public long getEstimatedCost() {
        return estimatedCost;
    }
}