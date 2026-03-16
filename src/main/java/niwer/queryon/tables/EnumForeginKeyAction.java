package niwer.queryon.tables;

/**
 * This enum represents the possible actions that can be taken when a foreign key constraint is violated in a SQL database.
 * It is used to specify the behavior of foreign key constraints in table definitions, such as what happens when a referenced row is deleted or updated.
 */
public enum EnumForeginKeyAction {

    CASCADE("CASCADE"),
    SET_NULL("SET NULL"),
    SET_DEFAULT("SET DEFAULT"),
    // RESTRICT("RESTRICT"), // Not supported yet
    NO_ACTION("NO ACTION");

    private final String SQL;

    EnumForeginKeyAction(String sql) {
        this.SQL = sql;
    }

    public String sql() { return SQL; }
}
