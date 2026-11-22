# Sphenic Number

A JavaFX application that checks every integer from 1 through an inclusive upper limit using multiple threads, displays each thread's progress, and lists the sphenic numbers in ascending order.

A sphenic number is the product of exactly three distinct primes, each appearing once. For example, `30 = 2 × 3 × 5` qualifies; `60 = 2² × 3 × 5` does not.

## Application

The supported upper limit is **1 to 10,000,000**, with **1 to 64 threads** and no more threads than numbers. Each thread has its own range and progress indicator. Results include the sorted sphenic numbers and the sphenic and non-sphenic totals. Cancelled runs have no final totals.

The default input, `100` with `3` threads, produces:

```text
Sphenic numbers: 30, 42, 66, 70, 78
Sphenic count: 5
Non-sphenic count: 95
```

## Threading

Each worker receives one number first. Remaining numbers are divided using weights `1, 2, …, cnt`, with leftover units assigned from the last worker backward. If sizes are all equal and unequal allocation is possible, one number moves from the first worker to the last. The resulting contiguous ranges cover `1..num` exactly once.

For `100/3`, the ranges are `1–17`, `18–50`, and `51–100`. A single thread handles the whole range; when `num == cnt`, every worker handles one number.

The scanner uses trial division and rejects repeated prime factors. Each worker owns its matches. JavaFX tasks publish progress at most about every 50 ms, plus initial/final updates; control updates stay on the JavaFX Application Thread. After all workers succeed, a background aggregation task joins their ordered results on the same worker pool. The GUI uses a virtualized list for large result sets. Cancelling or closing the window cancels active tasks and shuts down the pool.

## Technology

Java 21, JavaFX 21.0.6, JUnit 5.12.1, and Maven Surefire 3.5.3. The Maven wrapper is included.

Tests cover factorization, input validation, range coverage, parallel scanning, cancellation, and JavaFX interface behavior.
