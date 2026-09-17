package niwer.queryon;

public enum SchemaPrunePolicy {
    STRICT_SAFE, // Warn only; log detected discrepancies without altering schema
    PRUNE_COLUMNS, // Drop unregistered columns, keep unregistered tables
    PRUNE_ALL // Drop both unregistered columns and unregistered tables
}