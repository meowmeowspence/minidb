package minidb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BPlusTree {

    private final int maxKeys;

    private Node root;

    private int size;

    public BPlusTree(int maxKeys) {

        if (maxKeys < 3) {
            throw new IllegalArgumentException(
                    "B+ tree must allow at least 3 keys per node"
            );
        }

        this.maxKeys = maxKeys;
        this.root = new LeafNode();
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

        LeafNode leaf =
                findLeaf(key);

        int existingIndex =
                Collections.binarySearch(
                        leaf.keys,
                        key
                );

        if (existingIndex >= 0) {
            throw new IllegalArgumentException(
                    "Duplicate key: " + key
            );
        }

        int insertionIndex =
                -existingIndex - 1;

        leaf.keys.add(
                insertionIndex,
                key
        );

        leaf.values.add(
                insertionIndex,
                recordId
        );

        size++;

        if (leaf.keys.size()
                > maxKeys) {

            splitLeaf(leaf);
        }
    }

    public RecordId search(int key) {

        LeafNode leaf =
                findLeaf(key);

        int index =
                Collections.binarySearch(
                        leaf.keys,
                        key
                );

        if (index < 0) {
            return null;
        }

        return leaf.values.get(index);
    }

    public List<RecordId> rangeSearch(
            int minimumKey,
            int maximumKey
    ) {

        if (minimumKey > maximumKey) {
            throw new IllegalArgumentException(
                    "Minimum key cannot be greater than maximum key"
            );
        }

        List<RecordId> results =
                new ArrayList<>();

        LeafNode leaf =
                findLeaf(minimumKey);

        while (leaf != null) {

            for (
                    int i = 0;
                    i < leaf.keys.size();
                    i++
            ) {

                int key =
                        leaf.keys.get(i);

                if (key < minimumKey) {
                    continue;
                }

                if (key > maximumKey) {
                    return List.copyOf(
                            results
                    );
                }

                results.add(
                        leaf.values.get(i)
                );
            }

            leaf = leaf.next;
        }

        return List.copyOf(results);
    }

    public int size() {
        return size;
    }

    public int getHeight() {

        int height = 1;

        Node node = root;

        while (!node.isLeaf()) {

            InternalNode internal =
                    (InternalNode) node;

            node =
                    internal.children.get(0);

            height++;
        }

        return height;
    }

    private LeafNode findLeaf(int key) {

        Node node = root;

        while (!node.isLeaf()) {

            InternalNode internal =
                    (InternalNode) node;

            int childIndex = 0;

            while (
                    childIndex
                            < internal.keys.size()
                            && key
                            >= internal.keys.get(
                            childIndex
                    )
            ) {

                childIndex++;
            }

            node =
                    internal.children.get(
                            childIndex
                    );
        }

        return (LeafNode) node;
    }

    private void splitLeaf(
            LeafNode leaf
    ) {

        int splitIndex =
                (leaf.keys.size() + 1)
                        / 2;

        LeafNode right =
                new LeafNode();

        right.parent =
                leaf.parent;

        right.keys.addAll(
                new ArrayList<>(
                        leaf.keys.subList(
                                splitIndex,
                                leaf.keys.size()
                        )
                )
        );

        right.values.addAll(
                new ArrayList<>(
                        leaf.values.subList(
                                splitIndex,
                                leaf.values.size()
                        )
                )
        );

        leaf.keys
                .subList(
                        splitIndex,
                        leaf.keys.size()
                )
                .clear();

        leaf.values
                .subList(
                        splitIndex,
                        leaf.values.size()
                )
                .clear();

        /*
         * Preserve linked-list ordering
         * between leaf nodes.
         */
        right.next =
                leaf.next;

        leaf.next =
                right;

        /*
         * In a B+ tree, the first key in
         * the new right leaf becomes the
         * separator placed in the parent.
         */
        int separatorKey =
                right.keys.get(0);

        insertIntoParent(
                leaf,
                separatorKey,
                right
        );
    }

    private void insertIntoParent(
            Node left,
            int separatorKey,
            Node right
    ) {

        /*
         * Splitting the root means the tree
         * needs a brand-new root.
         */
        if (left == root) {

            InternalNode newRoot =
                    new InternalNode();

            newRoot.keys.add(
                    separatorKey
            );

            newRoot.children.add(
                    left
            );

            newRoot.children.add(
                    right
            );

            left.parent =
                    newRoot;

            right.parent =
                    newRoot;

            root =
                    newRoot;

            return;
        }

        InternalNode parent =
                left.parent;

        int leftIndex =
                parent.children.indexOf(
                        left
                );

        if (leftIndex < 0) {
            throw new IllegalStateException(
                    "Parent does not contain child"
            );
        }

        parent.keys.add(
                leftIndex,
                separatorKey
        );

        parent.children.add(
                leftIndex + 1,
                right
        );

        right.parent =
                parent;

        if (parent.keys.size()
                > maxKeys) {

            splitInternal(parent);
        }
    }

    private void splitInternal(
            InternalNode node
    ) {

        int middleIndex =
                node.keys.size()
                        / 2;

        int separatorKey =
                node.keys.get(
                        middleIndex
                );

        InternalNode right =
                new InternalNode();

        right.parent =
                node.parent;

        /*
         * Keys after the promoted key move
         * into the new right internal node.
         */
        right.keys.addAll(
                new ArrayList<>(
                        node.keys.subList(
                                middleIndex + 1,
                                node.keys.size()
                        )
                )
        );

        /*
         * Corresponding children move too.
         */
        right.children.addAll(
                new ArrayList<>(
                        node.children.subList(
                                middleIndex + 1,
                                node.children.size()
                        )
                )
        );

        for (Node child
                : right.children) {

            child.parent =
                    right;
        }

        /*
         * Remove the promoted key and
         * everything to its right from
         * the old node.
         */
        node.keys
                .subList(
                        middleIndex,
                        node.keys.size()
                )
                .clear();

        node.children
                .subList(
                        middleIndex + 1,
                        node.children.size()
                )
                .clear();

        insertIntoParent(
                node,
                separatorKey,
                right
        );
    }

    private abstract static class Node {

        final List<Integer> keys =
                new ArrayList<>();

        InternalNode parent;

        abstract boolean isLeaf();
    }

    private static final class LeafNode
            extends Node {

        final List<RecordId> values =
                new ArrayList<>();

        LeafNode next;

        @Override
        boolean isLeaf() {
            return true;
        }
    }

    private static final class InternalNode
            extends Node {

        final List<Node> children =
                new ArrayList<>();

        @Override
        boolean isLeaf() {
            return false;
        }
    }
}