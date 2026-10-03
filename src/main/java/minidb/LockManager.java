package minidb;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LockManager {

    private final Map<RecordId, LockState>
            locks;

    public LockManager() {

        this.locks =
                new HashMap<>();
    }

    public synchronized void acquireShared(
            Transaction transaction,
            RecordId recordId
    ) {

        validate(
                transaction,
                recordId
        );

        LockState state =
                locks.computeIfAbsent(
                        recordId,
                        ignored ->
                                new LockState()
                );

        /*
         * If this transaction already owns
         * the exclusive lock, it already has
         * stronger access than a shared lock.
         */
        if (
                state.exclusiveOwner != null
                        && state.exclusiveOwner
                        == transaction.getId()
        ) {

            return;
        }

        while (
                state.exclusiveOwner != null
                        && state.exclusiveOwner
                        != transaction.getId()
        ) {

            waitForLock();

            ensureActive(
                    transaction
            );
        }

        state.sharedOwners.add(
                transaction.getId()
        );
    }

    public synchronized void acquireExclusive(
            Transaction transaction,
            RecordId recordId
    ) {

        validate(
                transaction,
                recordId
        );

        LockState state =
                locks.computeIfAbsent(
                        recordId,
                        ignored ->
                                new LockState()
                );

        /*
         * Acquiring the same exclusive lock
         * twice is harmless.
         */
        if (
                state.exclusiveOwner != null
                        && state.exclusiveOwner
                        == transaction.getId()
        ) {

            return;
        }

        while (
                hasConflictingExclusiveOwner(
                        state,
                        transaction
                )
                        || hasConflictingSharedOwner(
                        state,
                        transaction
                )
        ) {

            waitForLock();

            ensureActive(
                    transaction
            );
        }

        /*
         * This also handles lock upgrading.
         *
         * If this transaction previously held
         * the only shared lock, remove it and
         * replace it with an exclusive lock.
         */
        state.sharedOwners.remove(
                transaction.getId()
        );

        state.exclusiveOwner =
                transaction.getId();
    }

    public synchronized void releaseAll(
            Transaction transaction
    ) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Transaction cannot be null"
            );
        }

        for (LockState state
                : locks.values()) {

            state.sharedOwners.remove(
                    transaction.getId()
            );

            if (
                    state.exclusiveOwner != null
                            && state.exclusiveOwner
                            == transaction.getId()
            ) {

                state.exclusiveOwner =
                        null;
            }
        }

        /*
         * Wake up transactions that might
         * have been waiting for these locks.
         */
        notifyAll();
    }

    synchronized boolean holdsSharedLock(
            Transaction transaction,
            RecordId recordId
    ) {

        LockState state =
                locks.get(
                        recordId
                );

        return state != null
                && state.sharedOwners.contains(
                transaction.getId()
        );
    }

    synchronized boolean holdsExclusiveLock(
            Transaction transaction,
            RecordId recordId
    ) {

        LockState state =
                locks.get(
                        recordId
                );

        return state != null
                && state.exclusiveOwner != null
                && state.exclusiveOwner
                == transaction.getId();
    }

    private boolean hasConflictingExclusiveOwner(
            LockState state,
            Transaction transaction
    ) {

        return state.exclusiveOwner != null
                && state.exclusiveOwner
                != transaction.getId();
    }

    private boolean hasConflictingSharedOwner(
            LockState state,
            Transaction transaction
    ) {

        for (long owner
                : state.sharedOwners) {

            if (
                    owner
                            != transaction.getId()
            ) {

                return true;
            }
        }

        return false;
    }

    private void validate(
            Transaction transaction,
            RecordId recordId
    ) {

        if (transaction == null) {
            throw new IllegalArgumentException(
                    "Transaction cannot be null"
            );
        }

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Record ID cannot be null"
            );
        }

        ensureActive(
                transaction
        );
    }

    private void ensureActive(
            Transaction transaction
    ) {

        if (!transaction.isActive()) {
            throw new IllegalStateException(
                    "Transaction is no longer active"
            );
        }
    }

    private void waitForLock() {

        try {

            wait();

        } catch (InterruptedException e) {

            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Interrupted while waiting for lock",
                    e
            );
        }
    }

    private static final class LockState {

        private final Set<Long>
                sharedOwners;

        private Long exclusiveOwner;

        private LockState() {

            this.sharedOwners =
                    new HashSet<>();

            this.exclusiveOwner =
                    null;
        }
    }
}