package niwer.queryon.benchmark;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import niwer.queryon.DataBase;
import niwer.queryon.queries.QueryManager;

/**
 * Compares Queryon connection modes using concurrent read and write workloads.
 *
 * <p>Arguments: output directory, thread count, operations per thread, warmup rounds.</p>
 */
public final class ConnectionModeBenchmark {

    private ConnectionModeBenchmark() {}

    public static void main(String[] args) throws Exception {
        final Path outputDirectory = Path.of(args.length > 0 ? args[0] : "build/benchmark-results");
        final int threads = argument(args, 1, 8);
        final int operationsPerThread = argument(args, 2, 250);
        final int warmupRounds = argument(args, 3, 2);

        if (threads <= 0 || operationsPerThread <= 0 || warmupRounds < 0)
            throw new IllegalArgumentException("threads and operations must be positive; warmup rounds cannot be negative");

        Files.createDirectories(outputDirectory);
        final List<Result> results = new ArrayList<>();
        for (DataBase.ConnectionMode mode : DataBase.ConnectionMode.values()) {
            for (Workload workload : Workload.values()) {
                results.add(run(mode, workload, threads, operationsPerThread, warmupRounds, outputDirectory));
            }
        }

        writeCsv(outputDirectory.resolve("connection-mode-results.csv"), results);
        writeHtml(outputDirectory.resolve("connection-mode-results.html"), results);
        results.forEach(result -> System.out.printf(
            Locale.ROOT,
            "%s,%s throughput=%.2f ops/s p50=%.3f ms p95=%.3f ms p99=%.3f ms errors=%d%n",
            result.mode, result.workload, result.throughput, result.p50Millis, result.p95Millis, result.p99Millis, result.errors));
    }

    private static Result run(DataBase.ConnectionMode mode, Workload workload, int threads, int operations, int warmupRounds, Path outputDirectory) throws Exception {
        final Path databaseFile = outputDirectory.resolve(mode.name().toLowerCase(Locale.ROOT) + "-" + workload.name().toLowerCase(Locale.ROOT) + ".db");
        Files.deleteIfExists(databaseFile);
        final DataBase database = new DataBase(databaseFile.toFile())
            .setConnectionMode(mode)
            .setSqliteBusyTimeoutMilliseconds(10_000);
        QueryManager.query(database, "CREATE TABLE benchmark_row (id INTEGER PRIMARY KEY, value INTEGER NOT NULL)");

        for (int round = 0; round < warmupRounds; round++)
            executeRound(database, workload, threads, operations, false);

        final Measurement measurement = executeRound(database, workload, threads, operations, true);
        final Result result = Result.from(mode, workload, threads, operations, measurement);
        database.disconnect();
        return result;
    }

    private static Measurement executeRound(DataBase database, Workload workload, int threads, int operations, boolean measure) throws Exception {
        if (workload == Workload.WRITE)
            QueryManager.query(database, "DELETE FROM benchmark_row");

        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Void>> futures = new ArrayList<>();
        final List<Long> latencies = new ArrayList<>(threads * operations);
        final Object latencyLock = new Object();
        final long startNanos;
        for (int thread = 0; thread < threads; thread++) {
            final int threadId = thread;
            futures.add(executor.submit(() -> {
                ready.countDown();
                start.await();
                for (int operation = 0; operation < operations; operation++) {
                    final long operationStart = System.nanoTime();
                    try {
                        if (workload == Workload.READ)
                            QueryManager.queryInt(database, "SELECT COUNT(*) FROM benchmark_row");
                        else
                            QueryManager.query(database, "INSERT INTO benchmark_row (id, value) VALUES (?, ?)",
                                threadId * operations + operation + 1, operation);
                    } finally {
                        if (measure) {
                            synchronized (latencyLock) {
                                latencies.add(System.nanoTime() - operationStart);
                            }
                        }
                    }
                }
                return null;
            }));
        }
        ready.await();
        startNanos = System.nanoTime();
        start.countDown();
        long errors = 0;
        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                errors++;
            }
        }
        final long elapsedNanos = System.nanoTime() - startNanos;
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        return new Measurement(threads * operations, elapsedNanos, latencies, errors);
    }

    private static void writeCsv(Path file, List<Result> results) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("mode,workload,threads,operationsPerThread,throughputOpsPerSecond,p50Millis,p95Millis,p99Millis,minMillis,maxMillis,errorCount%n".replace("%n", System.lineSeparator()));
            for (Result result : results) {
                writer.write(String.format(Locale.ROOT, "%s,%s,%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d%n",
                    result.mode, result.workload, result.threads, result.operationsPerThread, result.throughput,
                    result.p50Millis, result.p95Millis, result.p99Millis, result.minMillis, result.maxMillis, result.errors));
            }
        }
    }

    private static void writeHtml(Path file, List<Result> results) throws IOException {
        final StringBuilder bars = new StringBuilder();
        final double maxThroughput = results.stream().mapToDouble(result -> result.throughput).max().orElse(1);
        for (Result result : results) {
            final double width = Math.max(1, result.throughput / maxThroughput * 100);
            bars.append("<p><b>").append(result.mode).append(" / ").append(result.workload)
                .append("</b> ").append(String.format(Locale.ROOT, "%.2f ops/s", result.throughput))
                .append("<span style=\"display:block;background:#4f86f7;height:1.4em;width:")
                .append(String.format(Locale.ROOT, "%.2f", width)).append("%\"></span>")
                .append(String.format(Locale.ROOT, "p50 %.3f ms, p95 %.3f ms, p99 %.3f ms", result.p50Millis, result.p95Millis, result.p99Millis))
                .append("</p>");
        }
        Files.writeString(file, "<!doctype html><meta charset=\"utf-8\"><title>Queryon connection mode benchmark</title>"
            + "<style>body{font:16px sans-serif;max-width:900px;margin:2rem auto}span{margin:.3rem 0}</style>"
            + "<h1>Queryon connection mode benchmark</h1>" + bars);
    }

    private static int argument(String[] args, int index, int fallback) {
        return args.length > index ? Integer.parseInt(args[index]) : fallback;
    }

    private enum Workload { READ, WRITE }

    private record Measurement(int operations, long elapsedNanos, List<Long> latencies, long errors) {}

    private record Result(DataBase.ConnectionMode mode, Workload workload, int threads, int operationsPerThread,
                          double throughput, double p50Millis, double p95Millis, double p99Millis,
                          double minMillis, double maxMillis, long errors) {
        private static Result from(DataBase.ConnectionMode mode, Workload workload, int threads, int operations, Measurement measurement) {
            final List<Long> sorted = measurement.latencies.stream().sorted(Comparator.naturalOrder()).toList();
            return new Result(mode, workload, threads, operations,
                measurement.operations / (measurement.elapsedNanos / 1_000_000_000.0),
                percentile(sorted, .50), percentile(sorted, .95), percentile(sorted, .99),
                percentile(sorted, 0), percentile(sorted, 1), measurement.errors);
        }

        private static double percentile(List<Long> values, double percentile) {
            if (values.isEmpty()) return Double.NaN;
            return values.get((int) Math.min(values.size() - 1, Math.round(percentile * (values.size() - 1)))) / 1_000_000.0;
        }
    }
}
