package minidb;

public class OptimizedJoinPlan {

    private final Operator operator;

    private final JoinAlgorithm algorithm;

    private final long estimatedCost;

    private final long estimatedRowCount;

    public OptimizedJoinPlan(
            Operator operator,
            JoinAlgorithm algorithm,
            long estimatedCost,
            long estimatedRowCount
    ) {

        if (operator == null) {
            throw new IllegalArgumentException(
                    "Operator cannot be null"
            );
        }

        if (algorithm == null) {
            throw new IllegalArgumentException(
                    "Join algorithm cannot be null"
            );
        }

        if (estimatedCost < 0) {
            throw new IllegalArgumentException(
                    "Estimated cost cannot be negative"
            );
        }

        if (estimatedRowCount < 0) {
            throw new IllegalArgumentException(
                    "Estimated row count cannot be negative"
            );
        }

        this.operator = operator;
        this.algorithm = algorithm;
        this.estimatedCost =
                estimatedCost;

        this.estimatedRowCount =
                estimatedRowCount;
    }

    public Operator getOperator() {
        return operator;
    }

    public JoinAlgorithm getAlgorithm() {
        return algorithm;
    }

    public long getEstimatedCost() {
        return estimatedCost;
    }

    public long getEstimatedRowCount() {
        return estimatedRowCount;
    }
}