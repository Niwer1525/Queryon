package niwer.queryon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import niwer.queryon.backup.DatabaseBackupManager;
import niwer.queryon.queries.QueryManager;

class ConcurrencyStressTest {

    private static final int THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 25;

    @Test
    void parallelReads(@TempDir File tempDir) throws Exception {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        insertRows(db, THREADS * OPERATIONS_PER_THREAD);

        final List<Integer> counts = runConcurrently(THREADS, () ->
            QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table"));

        assertEquals(THREADS, counts.size());
        assertTrue(counts.stream().allMatch(count -> count == THREADS * OPERATIONS_PER_THREAD));
    }

    @Test
    void concurrentWrites(@TempDir File tempDir) throws Exception {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        final int totalRows = THREADS * OPERATIONS_PER_THREAD;

        runIndexedConcurrently(THREADS, thread -> {
            final int start = thread * OPERATIONS_PER_THREAD;
            for (int offset = 0; offset < OPERATIONS_PER_THREAD; offset++) {
                final int id = start + offset + 1;
                QueryManager.query(db,
                    "INSERT INTO user_table (id, name, age) VALUES (?, ?, ?)",
                    id, "user-" + id, 20);
            }
            return null;
        });

        assertEquals(totalRows, QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table"));
    }

    @Test
    void modeChangesWhileQueriesRun(@TempDir File tempDir) throws Exception {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        insertRows(db, 1);

        final ExecutorService executor = Executors.newFixedThreadPool(THREADS + 1);
        try {
            final CountDownLatch start = new CountDownLatch(1);
            final List<Callable<Void>> tasks = new ArrayList<>();
            for (int thread = 0; thread < THREADS; thread++) {
                tasks.add(() -> {
                    start.await();
                    for (int operation = 0; operation < OPERATIONS_PER_THREAD; operation++) {
                        QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table");
                    }
                    return null;
                });
            }
            tasks.add(() -> {
                start.await();
                for (int operation = 0; operation < OPERATIONS_PER_THREAD; operation++) {
                    db.setConnectionMode(operation % 2 == 0
                        ? DataBase.ConnectionMode.PERSISTENT
                        : DataBase.ConnectionMode.PER_QUERY);
                }
                return null;
            });

            final List<Future<Void>> futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (Future<Void> future : futures) future.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table"));
    }

    @Test
    void backupRunsAlongsideWrites(@TempDir File tempDir, @TempDir File backupDir) throws Exception {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir);
        final DatabaseBackupManager backupManager = DatabaseBackupManager.create(db, backupDir, "stress-backup", 7);
        final ExecutorService executor = Executors.newFixedThreadPool(THREADS + 2);
        try {
            final List<Callable<Void>> tasks = new ArrayList<>();
            for (int thread = 0; thread < THREADS; thread++) {
                final int start = thread * OPERATIONS_PER_THREAD;
                tasks.add(() -> {
                    for (int offset = 0; offset < OPERATIONS_PER_THREAD; offset++) {
                        final int id = start + offset + 1;
                        QueryManager.query(db,
                            "INSERT INTO user_table (id, name, age) VALUES (?, ?, ?)",
                            id, "user-" + id, 20);
                    }
                    return null;
                });
            }
            tasks.add(() -> {
                backupManager.createBackup("stress-one");
                return null;
            });
            tasks.add(() -> {
                backupManager.createBackup("stress-two");
                return null;
            });

            for (Future<Void> future : executor.invokeAll(tasks, 30, TimeUnit.SECONDS)) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(THREADS * OPERATIONS_PER_THREAD, QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table"));
        try (var files = Files.list(backupDir.toPath())) {
            assertTrue(files.findAny().isPresent(), "At least one concurrent backup should be created");
        }
    }

    @Test
    void recoversAfterPersistentConnectionFailure(@TempDir File tempDir) throws Exception {
        final DataBase db = QueryonEngineTest.setupUsersDB(tempDir)
            .setConnectionMode(DataBase.ConnectionMode.PERSISTENT);
        db.connect();
        assertTrue(db.isConnected());
        db.sqlConnection().close();

        assertFalse(db.isConnected());
        assertEquals(0, QueryManager.queryInt(db, "SELECT COUNT(*) FROM user_table"));
        assertTrue(db.isConnected());
    }

    private static void insertRows(DataBase db, int count) throws QueryonException {
        for (int id = 1; id <= count; id++) {
            QueryManager.query(db,
                "INSERT INTO user_table (id, name, age) VALUES (?, ?, ?)",
                id, "user-" + id, 20);
        }
    }

    private static <T> List<T> runConcurrently(int threads, Callable<T> operation) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            final List<Future<T>> futures = IntStream.range(0, threads)
                .mapToObj(ignored -> executor.submit(operation))
                .toList();
            final List<T> results = new ArrayList<>(threads);
            for (Future<T> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception exception) throw exception;
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }

    private static <T> List<T> runIndexedConcurrently(int threads, java.util.function.Function<Integer, T> operation) throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            final List<Future<T>> futures = IntStream.range(0, threads)
                .mapToObj(index -> executor.submit(() -> operation.apply(index)))
                .toList();
            final List<T> results = new ArrayList<>(threads);
            for (Future<T> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
            return results;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception exception) throw exception;
            throw e;
        } finally {
            executor.shutdownNow();
        }
    }
}
