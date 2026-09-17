package niwer.queryon.backup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;

public final class DatabaseBackupManager {

    private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss_SSS");
    
    // Variables to track the state of the database at the time of the last backup
    private long lastDbSize = -1;
    private String lastDbHash = null;

    private final DataBase DATABASE;
    private final File BACKUP_DIR;
    private final String BACKUP_FILE_PREFIX;
    private final DatabaseBackupScheduler SCHEDULER;
    private final int BACKUP_RETENTION_DAYS;

    private DatabaseBackupManager(DataBase database, File backupDir, String backupFilePrefix, int backupRetentionDays, long backupIntervalMinutes) {
        if (database == null) throw new IllegalArgumentException("Database cannot be null");
        if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory()) throw new IllegalArgumentException("Backup directory is not properly configured or accessible");
        if (backupFilePrefix == null || backupFilePrefix.isEmpty()) throw new IllegalArgumentException("Backup file prefix cannot be null or empty");

        this.DATABASE = database;
        this.BACKUP_DIR = backupDir;
        this.BACKUP_FILE_PREFIX = backupFilePrefix;
        this.SCHEDULER = backupIntervalMinutes <= 0 ? null : new DatabaseBackupScheduler(backupIntervalMinutes, this);
        this.BACKUP_RETENTION_DAYS = backupRetentionDays;

        this.deletedOldBackups(); // Clean up old backups on initialization
    }

    /**
     * Creates a DatabaseBackupManager instance without scheduling automatic backups. Use this if you want to manually trigger backups.
     * 
     * @param database The DataBase instance to back up.
     * @param backupDir The directory where backups will be stored.
     * @param backupFilePrefix The prefix for backup file names.
     * @param backupRetentionDays The number of days to retain backups. Backups older than this will be deleted. If set to 0 or negative, no backups will be deleted.
     * @return A new DatabaseBackupManager instance.
     */
    public static DatabaseBackupManager create(DataBase database, File backupDir, String backupFilePrefix, int backupRetentionDays) {
        return new DatabaseBackupManager(database, backupDir, backupFilePrefix, backupRetentionDays, -1);
    }

    /**
     * Creates a DatabaseBackupManager instance with automatic backup scheduling. Backups will be created at the specified interval.
     * 
     * @param database The DataBase instance to back up.
     * @param backupDir The directory where backups will be stored.
     * @param backupFilePrefix The prefix for backup file names.
     * @param backupRetentionDays The number of days to retain backups. Backups older than this will be deleted. If set to 0 or negative, no backups will be deleted.
     * @param backupIntervalMinutes The interval in minutes at which automatic backups will be created. Must be positive.
     * @return A new DatabaseBackupManager instance.
     */
    public static DatabaseBackupManager createWithScheduler(DataBase database, File backupDir, String backupFilePrefix, int backupRetentionDays, long backupIntervalMinutes) {
        return new DatabaseBackupManager(database, backupDir, backupFilePrefix, backupRetentionDays, backupIntervalMinutes);
    }
    
    /**
     * @return the DatabaseBackupScheduler associated with this manager, if any. If automatic backups are not enabled, this will return null.
     */
    public DatabaseBackupScheduler scheduler() {
        return this.SCHEDULER;
    }

    /**
     * Creates a backup of the database.
     * If a backup with the same content already exists, it will be skipped to avoid unnecessary duplication.
     */
    public void createBackup() {
        createBackup(null);
    }

    /**
     * Creates a backup of the database with a custom file name.
     * If a backup with the same content already exists, it will be skipped to avoid unnecessary
     * 
     * @param customBackupFileName The custom name for the backup file. If null, The default name provided when creating the DatabaseBackupManager instance will be used.
     */
    public void createBackup(String customBackupFileName) {
        final Path DATABASE_FILE = this.DATABASE.dataBaseFile().toPath();
        if (!Files.exists(DATABASE_FILE)) {
            Console.log("Skipping database backup because the database file does not exist yet").type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
            return;
        }

        try {
            /* Check metadata (size) before hashing to save compute time */
            final long CURRENT_FILE_SIZE = Files.size(DATABASE_FILE);
            String currentHash = null;
            boolean shouldSkipBackup = false;
            if (CURRENT_FILE_SIZE == lastDbSize && lastDbHash != null) {
                currentHash = hashFile(DATABASE_FILE);
                if (currentHash.equals(lastDbHash)) shouldSkipBackup = true;
            }

            if (shouldSkipBackup) {
                final String TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                Console.log("Backup skipped at " + TIME + ". The previous is the same.").type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
                return;
            }

            final String backupFileName = (customBackupFileName != null ? customBackupFileName : this.BACKUP_FILE_PREFIX + "-backup") + "-" + LocalDateTime.now().format(BACKUP_TIMESTAMP) + ".db";
            final Path backupDirectory = this.BACKUP_DIR.toPath();
            final Path backupFile = backupDirectory.resolve(backupFileName).toAbsolutePath().normalize();
            if (!Files.isWritable(backupFile.getParent())) throw new IOException("Target backup directory is not writable: " + backupFile.getParent());

            Files.createDirectories(backupDirectory);
            final String SQL = "VACUUM INTO ?";
            try (var STATEMENT = this.DATABASE.sqlConnection().prepareStatement(SQL)) {
                STATEMENT.setString(1, backupFile.toString());
                STATEMENT.executeUpdate();
            }
            
            /* Update tracking variables after a successful backup */
            lastDbSize = CURRENT_FILE_SIZE;
            if (currentHash == null) lastDbHash = hashFile(DATABASE_FILE);
            else lastDbHash = currentHash;

            Console.log("Database backup created at " + backupFile).type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
        } catch (IOException | SQLException e) {
            Console.log("Failed to create database backup: " + e.getMessage()).error().type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
        }
    }

    /**
     * Deletes old database backups based on the configured retention period.
     * Backups older than the specified number of days will be deleted.
     * If the retention period is set to 0 or negative, no backups will be deleted.
     * 
     * @Note This method is called automatically during initialization, but can also be called manually if needed.
     */
    public void deletedOldBackups() {
        if(this.BACKUP_RETENTION_DAYS <= 0) return;
        Stream.of(this.BACKUP_DIR.listFiles()).parallel().filter(file -> {
            try {
                if (file == null || !file.isFile()) return false; // Skip folders and nulls
    
                final String FILME_NAME = file.getName();
                final FileTime CREATION_TIME = (FileTime) Files.getAttribute(file.toPath(), "creationTime");
                return FILME_NAME.startsWith(this.BACKUP_FILE_PREFIX) && CREATION_TIME.toMillis() < System.currentTimeMillis() - TimeUnit.DAYS.toMillis(this.BACKUP_RETENTION_DAYS);
            } catch (Exception e) {
                Console.log("Failed to delete old database backups: " + e.getMessage()).error().type(QueryonLogTypes.BACKUP).container(QueryonEngine.LOGGER).send();
                return false;
            }
        }).forEach(file -> {
            if (file != null) file.delete();
        });
    }

    private static String hashFile(Path file) throws IOException {
        if(file == null || !Files.exists(file) || !Files.isRegularFile(file)) throw new IllegalArgumentException("Invalid file path provided for hashing.");

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream IS = Files.newInputStream(file); DigestInputStream DIS = new DigestInputStream(IS, digest)) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                while (DIS.read(buffer) != -1) {} // Reading the stream automatically updates the MessageDigest
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder(2 * hashBytes.length);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}