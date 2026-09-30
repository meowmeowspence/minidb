package minidb;

import java.util.ArrayList;
import java.util.List;

public class HashIndex {

    private final List<List<Entry>> buckets;

    private int size;

    public HashIndex(int bucketCount) {

        if (bucketCount < 1) {
            throw new IllegalArgumentException(
                    "Hash index must contain at least one bucket"
            );
        }

        this.buckets =
                new ArrayList<>();

        for (
                int i = 0;
                i < bucketCount;
                i++
        ) {

            buckets.add(
                    new ArrayList<>()
            );
        }

        this.size = 0;
    }

    public void insert(
            int key,
            RecordId recordId
    ) {

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Record ID cannot be null"
            );
        }

        List<Entry> bucket =
                bucketFor(key);

        for (Entry entry : bucket) {

            if (entry.key == key) {
                throw new IllegalArgumentException(
                        "Duplicate key: " + key
                );
            }
        }

        bucket.add(
                new Entry(
                        key,
                        recordId
                )
        );

        size++;
    }

    public RecordId search(int key) {

        List<Entry> bucket =
                bucketFor(key);

        for (Entry entry : bucket) {

            if (entry.key == key) {
                return entry.recordId;
            }
        }

        return null;
    }

    public int size() {
        return size;
    }

    public int getBucketCount() {
        return buckets.size();
    }

    private List<Entry> bucketFor(
            int key
    ) {

        int bucketIndex =
                Math.floorMod(
                        Integer.hashCode(key),
                        buckets.size()
                );

        return buckets.get(
                bucketIndex
        );
    }

    private static final class Entry {

        private final int key;
        private final RecordId recordId;

        private Entry(
                int key,
                RecordId recordId
        ) {

            this.key = key;
            this.recordId = recordId;
        }
    }
}