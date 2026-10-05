# MiniDB

MiniDB is a small relational database management system implemented from
scratch in Java.

The project explores the internals of a relational DBMS, including physical
storage, indexing, relational query execution, external sorting, alternative
join algorithms, statistics, cost estimation, and physical plan selection.

MiniDB does not use an existing database engine for storage or query
execution.

## Current Architecture

```text
                    Query / Physical Planning

                AccessPathOptimizer
                  /      |       \
                 /       |        \
          Seq Scan    B+ Tree    Hash Index

                       JoinOptimizer
                   /        |        \
                  /         |         \
        Nested Loop      Hash      Sort-Merge


                      Query Execution

                     Projection
                         |
                       Filter
                         |
              +----------+----------+
              |                     |
           Seq Scan                 Join
                                     |
                              child operators


                       Storage Engine

                          Table
                            |
                         HeapFile
                            |
                      SlottedPage
                            |
                           Page
                            |
                       DiskManager
                            |
                         .db file
```

## Implemented Features

### Storage

- 4 KB fixed-size database pages
- Disk-backed page allocation, reading, and writing
- Slotted-page layout for variable-length records
- Heap-file organization
- Record identifiers using page ID and slot ID
- Binary tuple serialization and deserialization

### Indexing

- B+ tree
    - exact-key lookup
    - insertion
    - leaf splitting
    - internal-node splitting
    - linked leaves
    - range lookup
- Hash index
    - exact-key lookup
    - bucket-based collision handling

### Query Execution

- Iterator-style `open / next / close` execution model
- Sequential table scans
- Selection / filtering
- Projection
- Nested-loop join
- In-memory hash join
- Sort-merge join
- Composable physical query-plan trees

### Sorting

- External merge sort
- Configurable in-memory run capacity
- Temporary sorted runs written to disk
- Multi-pass pairwise merging

### Query Optimization

- Table statistics
    - row count
    - page count
    - distinct-value counts
- Basic equality-cardinality estimation
- Cost estimation for:
    - sequential scans
    - B+ tree lookups
    - hash-index lookups
    - nested-loop joins
    - hash joins
    - external sorting
    - sort-merge joins
- Cost-based access-path selection
- Cost-based join algorithm selection

### Transactions, Concurrency, and Recovery

- Transaction IDs and lifecycle states
- Commit and abort
- Row-level shared and exclusive locks
- Shared-to-exclusive lock upgrades
- Locks held until transaction completion
- Transaction-aware record reads and updates
- Write-ahead logging for record updates
- Monotonically increasing log sequence numbers
- Full before-images and after-images
- WAL forced before database updates
- Durable commit records
- Rollback of aborted updates
- REDO of committed updates after restart
- UNDO of incomplete transactions after restart
- Recovery across database process restarts

## Example Query Plan

A query conceptually equivalent to:

```sql
SELECT species, location
FROM animals
JOIN observations
    ON animal_id = animal_ref_id;
```

can be represented using a physical plan such as:

```text
Projection(species, location)
            |
        Hash Join
        /       \
       /         \
Seq Scan       Seq Scan
Animals       Observations
```

MiniDB can also execute the same logical join using nested-loop join or
sort-merge join.

The join optimizer estimates the cost of the available physical alternatives
and chooses one.

## Storage Layout

MiniDB stores heap-file records in 4096-byte pages.

A slotted page is organized approximately as:

```text
+----------------------------------+
| Page header                      |
| tuple count / free-space pointer |
+----------------------------------+
| Slot 0: offset + length          |
| Slot 1: offset + length          |
| Slot 2: offset + length          |
+----------------------------------+
|                                  |
|          free space              |
|                                  |
+----------------------------------+
| tuple data                       |
| tuple data                       |
| tuple data                       |
+----------------------------------+
```

The slot directory grows forward while tuple data grows backward from the end
of the page.

## Indexes

### B+ Tree

The current B+ tree supports integer keys mapped to heap-file `RecordId`
values.

Leaf nodes are linked to support range traversal.

The current B+ tree structure is memory-resident; heap-file records themselves
remain disk-backed.

### Hash Index

The hash index maps integer keys to `RecordId` values using buckets with
collision handling.

The current hash index is also memory-resident.

## External Sorting

MiniDB's external sort operator does not require its entire input to fit in
memory.

It:

1. reads a limited number of tuples,
2. sorts them in memory,
3. writes a sorted run to a temporary file,
4. repeats until all input is consumed,
5. merges the sorted runs until one sorted output remains.

The current memory limit is modeled as a maximum tuple count per run rather
than an exact byte-level buffer budget.

## Cost Model

MiniDB uses simplified abstract cost units rather than measured milliseconds.

For example, the optimizer can compare:

```text
Sequential scan
vs.
B+ tree lookup
vs.
Hash-index lookup
```

and:

```text
Nested-loop join
vs.
Hash join
vs.
Sort-merge join
```

The estimates are intentionally simplified and are designed to demonstrate
physical query-plan selection rather than reproduce a production DBMS cost
model.

## Design Layers

MiniDB separates relational execution from physical storage.

### Logical data

`Schema`, `Column`, and `Tuple` represent relational records.

### Execution

Operators expose an iterator-style interface:

```text
open()
next()
next()
...
close()
```

This allows operators to be composed into query-plan trees.

### Access methods

Sequential scans and index lookups provide alternative methods for retrieving
table records.

### Storage

`HeapFile`, `SlottedPage`, `Page`, and `DiskManager` are responsible for the
physical representation of records.

### Optimization

Statistics and simplified cost formulas allow MiniDB to compare multiple
physical implementations of the same logical operation.

The optimizer currently makes two kinds of decisions:

```text
Equality predicate:
Seq Scan vs B+ Tree vs Hash Index

Equality join:
Nested Loop vs Hash Join vs Sort-Merge
```

## Current Limitations

MiniDB is an educational database engine rather than a production DBMS.

Current limitations include:

- no SQL parser yet
- no SQL NULL values
- integer-only indexed keys
- indexes are currently memory-resident
- no persistent system catalog
- simplified optimizer statistics
- simplified cost formulas
- hash join requires its build side to fit within a configured memory limit
- no transaction manager yet
- no concurrency control yet
- transaction locking is not yet integrated into all table operations
- no deadlock detection or prevention yet
- no rollback/undo of modified records yet
- transactional inserts and deletes are not yet implemented
- WAL currently covers record updates, not transactional inserts or deletes
- recovery uses full before/after images rather than ARIES
- no checkpoints yet
- no pageLSN or compensation log records
- no deadlock detection or prevention yet
- indexes are still memory-resident

These limitations are being implemented incrementally as the project develops.

## Requirements

- Java 21
- Maven

## Build

```bash
mvn compile
```

## Run Tests

```bash
mvn test
```

## Run the Demo

After compiling:

```bash
java -cp target/classes minidb.Demo
```

## Run the Benchmark

After compiling:

```bash
java -cp target/classes minidb.BenchmarkRunner
```

The benchmark compares exact-key lookup using:

- sequential scanning,
- the B+ tree,
- the hash index.

The reported values are simple JVM wall-clock measurements and should be
treated as illustrative rather than rigorous performance measurements.

## Technology

- Java 21
- Maven
- JUnit 5
- Git / GitHub