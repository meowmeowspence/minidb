package minidb;

public final class Transaction {

    private final long id;

    private volatile TransactionState state;

    Transaction(long id) {

        if (id < 1) {
            throw new IllegalArgumentException(
                    "Transaction ID must be positive"
            );
        }

        this.id = id;
        this.state =
                TransactionState.ACTIVE;
    }

    public long getId() {
        return id;
    }

    public TransactionState getState() {
        return state;
    }

    public boolean isActive() {
        return state
                == TransactionState.ACTIVE;
    }

    synchronized void markCommitted() {

        ensureActive();

        state =
                TransactionState.COMMITTED;
    }

    synchronized void markAborted() {

        ensureActive();

        state =
                TransactionState.ABORTED;
    }

    private void ensureActive() {

        if (!isActive()) {
            throw new IllegalStateException(
                    "Transaction is no longer active"
            );
        }
    }

    @Override
    public String toString() {

        return "Transaction{"
                + "id=" + id
                + ", state=" + state
                + '}';
    }
}