# Sphenic Number Explorer

A JavaFX application that checks every integer from 1 through an inclusive upper limit using multiple threads, displays each thread's progress, and lists the sphenic numbers in ascending order.

A sphenic number is the product of exactly three distinct primes, each appearing once. For example, `30 = 2 × 3 × 5` qualifies; `60 = 2² × 3 × 5` does not.

## Run

Use JDK 21 or newer and set `JAVA_HOME` to that JDK. The project includes the Maven wrapper, so a separate Maven installation is unnecessary.

```powershell
.\mvnw.cmd javafx:run
```

The first build needs internet access to download Maven and dependencies. Once cached, use `-o` to run offline:

```powershell
.\mvnw.cmd -o javafx:run
```

In IntelliJ IDEA, open the Maven project, select a JDK 21+ project SDK, reload Maven, and run `org.example.sphenic_number.Launcher`.

## Use

1. Enter an upper limit from **1 to 10,000,000**.
2. Enter **1 to 64 threads**, no greater than the upper limit.
3. Select **Start** (or press Enter). The inputs are disabled while the search runs.
4. Inspect each thread's assigned range and progress. Results and both totals appear after all threads finish.
5. Select **Cancel** (or press Escape) to stop a run. Cancelled runs do not display partial results as final totals. Start again to run a new search.

The default input, `100` with `3` threads, produces:

```text
Sphenic numbers: 30, 42, 66, 70, 78
Sphenic count: 5
Non-sphenic count: 95
```

Small runs may finish almost immediately; try `1000000` or a larger upper limit to observe progress. No artificial delays are added. The controls accept plain whole numbers without commas.

## How the threads work

Each worker receives one number first. Remaining numbers are divided using weights `1, 2, …, cnt`, with leftover units assigned from the last worker backward. If sizes are all equal and unequal allocation is possible, one number moves from the first worker to the last. The resulting contiguous ranges cover `1..num` exactly once.

For `100/3`, the ranges are `1–17`, `18–50`, and `51–100`. A single thread handles the whole range; when `num == cnt`, every worker handles one number.

The scanner uses trial division and rejects repeated prime factors. Each worker owns its matches. JavaFX tasks publish progress at most about every 50 ms, plus initial/final updates; control updates stay on the JavaFX Application Thread. After all workers succeed, a background aggregation task joins their ordered results on the same worker pool. The GUI uses a virtualized list for large result sets. Cancelling or closing the window cancels active tasks and shuts down the pool.

## Verify

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Add `-o` for offline execution after dependencies are cached. Tests cover factorization, input validation, range coverage, parallel scanning, cancellation, and JavaFX interface behavior. JavaFX tests require a graphical desktop environment; they load the actual FXML and exercise its controls.

The build targets Java 21 and uses JavaFX 21.0.6, JUnit 5.12.1, and Maven Surefire 3.5.3.
