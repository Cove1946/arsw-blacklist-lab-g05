# Execution environment

| Item | Value |
|---|---|
| Operating system | Microsoft Windows 11 Home Single Language 64-bit (Build 26200) |
| CPU model | AMD Ryzen 7 7730U with Radeon Graphics |
| Physical cores / Logical processors | 8 / 16 |
| RAM | 15.34 GB |
| JDK vendor and version | Oracle Corporation, JDK 21.0.9+7-LTS |
| Maven version | Apache Maven 3.9.12 |
| Measurement date | 2026-08-07 |

## Methodology notes

- All ten mandatory configurations were run on the same machine, back to back, without changing
  the source code between runs.
- `mvn clean test` passed before measuring.
- Each configuration used 2 warm-up executions (discarded) and 5 measured executions.
- Runs were executed directly against the compiled classes
  (`java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner ...`) after a single
  `mvn compile`, to avoid Maven/JVM startup overhead from skewing the measured elapsed times.
  Elapsed time itself is always the duration returned by the search implementation
  (`SearchResult.elapsed()`), never wall-clock or IDE timestamps.
- IP address: `202.24.34.55`. Provider count: 100. Alarm threshold: 5.
- Every one of the 50 measured runs (10 configurations x 5 runs) reported `matches=7` and
  `consulted_providers=100`, confirming functional equivalence across strategies and pool sizes.

## Commands used

```bash
mvn clean test
mvn compile

java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner SEQUENTIAL 202.24.34.55 false 2 5
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 false 2 5 2
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 false 2 5 4
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 false 2 5 8
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner VIRTUAL 202.24.34.55 false 2 5

java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner SEQUENTIAL 202.24.34.55 true 2 5
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 true 2 5 2
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 true 2 5 4
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner FIXED 202.24.34.55 true 2 5 8
java -cp target/classes edu.eci.arsw.blacklist.BenchmarkRunner VIRTUAL 202.24.34.55 true 2 5
```

(Equivalently via `mvn exec:java -Dexec.args="..."` for each line, at the cost of extra
Maven-startup time per invocation.)

## Summary (derived from results.csv)

| Scenario | Strategy | Pool size | Average ms | Minimum ms | Maximum ms | Speedup | Matches | Consulted |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| No simulated I/O | Sequential | — | 0.020 | 0.014 | 0.029 | 1.00 | 7 | 100 |
| No simulated I/O | Fixed pool | 2 | 0.505 | 0.380 | 0.668 | 0.04 | 7 | 100 |
| No simulated I/O | Fixed pool | 4 | 0.566 | 0.473 | 0.650 | 0.04 | 7 | 100 |
| No simulated I/O | Fixed pool | 8 | 1.223 | 0.663 | 2.824 | 0.02 | 7 | 100 |
| No simulated I/O | Virtual threads | — | 0.721 | 0.464 | 0.905 | 0.03 | 7 | 100 |
| Simulated I/O | Sequential | — | 11124.252 | 11020.956 | 11500.182 | 1.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 2 | 5597.677 | 5504.754 | 5846.998 | 1.99 | 7 | 100 |
| Simulated I/O | Fixed pool | 4 | 3006.012 | 2993.078 | 3018.336 | 3.70 | 7 | 100 |
| Simulated I/O | Fixed pool | 8 | 1558.112 | 1550.568 | 1562.532 | 7.14 | 7 | 100 |
| Simulated I/O | Virtual threads | — | 199.278 | 198.660 | 200.603 | 55.82 | 7 | 100 |

Speedup = sequential average time / strategy average time, within the same scenario.
