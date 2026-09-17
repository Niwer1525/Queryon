package niwer.queryon.backup;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import niwer.lumen.Console;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;

public class DatabaseBackupScheduler {

    private volatile ScheduledExecutorService scheduler;
    private final long INTERVAL_MINUTES;
    private final DatabaseBackupManager BACKUP_MANAGER;

    protected DatabaseBackupScheduler(long intervalMinutes, DatabaseBackupManager backupManager) {
        if (intervalMinutes <= 0L) throw new IllegalArgumentException("Backup interval must be positive");
        if (backupManager == null) throw new IllegalArgumentException("Backup manager cannot be null");

        this.INTERVAL_MINUTES = intervalMinutes;
        this.BACKUP_MANAGER = backupManager;

        /* Initialize the backup scheduler */
        start();
    }
    
    /**
     * Starts the backup scheduler, which will create backups at the specified interval. If the scheduler is already running, it will be stopped and restarted.
     */
    public synchronized void start() {
        /* Stop and try to delete old backups */
        stop();
        this.BACKUP_MANAGER.deletedOldBackups();

        scheduler = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
        scheduler.scheduleAtFixedRate(() -> BACKUP_MANAGER.createBackup("automatic-backup"), INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
        Console.log("Database backups scheduled every " + INTERVAL_MINUTES + " minute(s)").type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
    }

    /**
     * Stops the backup scheduler, preventing any further automatic backups from being created. If the scheduler is not running, this method has no effect.
     */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private static final class DaemonThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            final Thread thread = new Thread(runnable, "queryon-database-backup");
            thread.setDaemon(true);
            return thread;
        }
    }
}
