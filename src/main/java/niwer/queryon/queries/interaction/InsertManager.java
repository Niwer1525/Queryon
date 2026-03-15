package niwer.queryon.queries.interaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import niwer.queryon.DataBase;
import niwer.queryon.SQLSerializable;
import niwer.queryon.queries.InteractionManager;
import niwer.queryon.queries.ValueToken;
import niwer.queryon.tables.Table;

/**
 * Manager for building and executing INSERT queries with a fluent API.
 * Supports single and multi-row inserts, inserting from SELECT, UPSERT (ON CONFLICT), and RETURNING clauses.
 * 
 * @author Niwer
 */
public class InsertManager {

    private final DataBase DATA_BASE;
    private final Table TABLE;
    private final boolean IGNORE_IF_EXISTS;
    private final Map<String, ValueToken> VALUES = new LinkedHashMap<>();
    private final List<Map<String, ValueToken>> EXTRA_ROWS = new ArrayList<>();
    
    private String tableAlias;
    private String insertSelectQuery;
    private final List<Object> INSERT_SELECT_PARAMS = new ArrayList<>();

    private String conflictTarget;
    private boolean conflictDoNothing;
    private final Map<String, ValueToken> CONFLICT_UPDATE = new LinkedHashMap<>();
    private String conflictWhereClause;
    private final List<Object> conflictWhereParams = new ArrayList<>();

    private String returningClause;

    private InsertManager(DataBase db, Class<? extends Table> table, boolean ignoreIfExists) {
        this.DATA_BASE = db;
        this.TABLE = db.getTable(table);
        this.IGNORE_IF_EXISTS = ignoreIfExists;
    }

    /**
     * Starts an insertion query for the specified table.
     * This function will not ignore existing entries with the same primary key, use insertOrIgnore() for that.
     * 
     * @param table The table to select from
     */
    public static InsertManager insert(DataBase db, Class<? extends Table> table) { return new InsertManager(db, table, false); }

    /**
     * Starts an insertion query for the specified table, ignoring existing entries with the same primary key.
     * 
     * @param table The table to select from
     */
    public static InsertManager insertOrIgnore(DataBase db, Class<? extends Table> table) { return new InsertManager(db, table, true); }

    public InsertManager value(String column, Object value) {
        VALUES.put(column, ValueToken.parameter(value));
        return this;
    }

    public InsertManager valueNull(String column) {
        VALUES.put(column, ValueToken.raw("NULL"));
        return this;
    }

    public InsertManager valueDefault(String column) {
        VALUES.put(column, ValueToken.raw("DEFAULT"));
        return this;
    }

    /**
     * Use a SQL expression for a value (for example: "CURRENT_TIMESTAMP", "? + 1", "(SELECT COUNT(*) FROM users)").
     */
    public InsertManager valueExpression(String column, String expression, Object... params) {
        if (expression == null || expression.isBlank()) throw new IllegalArgumentException("Value expression cannot be null or empty.");
        VALUES.put(column, ValueToken.expression(expression, params));
        return this;
    }

    /**
     * Add an extra row. Keys are columns and values are bound as prepared statement parameters.
     */
    public InsertManager row(Map<String, Object> values) {
        if (values == null || values.isEmpty()) throw new IllegalArgumentException("Row values cannot be null or empty.");

        final Map<String, ValueToken> row = new HashMap<>();
        for (final Map.Entry<String, Object> entry : values.entrySet()) row.put(entry.getKey(), ValueToken.parameter(entry.getValue()));
        EXTRA_ROWS.add(row);
        return this;
    }

    /**
     * Insert from a SELECT statement.
     */
    public InsertManager fromSelect(String selectSql, Object... params) {
        if (selectSql == null || selectSql.isBlank()) throw new IllegalArgumentException("Select SQL cannot be null or empty.");
        this.insertSelectQuery = selectSql;
        this.INSERT_SELECT_PARAMS.clear();
        if (params != null && params.length > 0) this.INSERT_SELECT_PARAMS.addAll(Arrays.asList(params));
        return this;
    }

    /**
     * Add an alias for the inserted table.
     */
    public InsertManager as(String alias) {
        if (alias == null || alias.isBlank()) throw new IllegalArgumentException("Alias cannot be null or empty.");
        this.tableAlias = alias;
        return this;
    }

    /**
     * Configure conflict target for UPSERT (SQLite/PostgreSQL syntax).
     */
    public InsertManager onConflict(String... columns) {
        if (columns == null || columns.length == 0) throw new IllegalArgumentException("onConflict requires at least one column.");
        this.conflictTarget = String.join(", ", columns);
        return this;
    }

    public InsertManager doNothingOnConflict() {
        this.conflictDoNothing = true;
        this.CONFLICT_UPDATE.clear();
        return this;
    }

    public InsertManager doUpdateSet(String column, Object value) {
        this.conflictDoNothing = false;
        this.CONFLICT_UPDATE.put(column, ValueToken.parameter(value));
        return this;
    }

    /**
     * Shortcut for: column = excluded.column
     */
    public InsertManager doUpdateSetExcluded(String column) {
        this.conflictDoNothing = false;
        this.CONFLICT_UPDATE.put(column, ValueToken.raw("excluded." + column));
        return this;
    }

    public InsertManager doUpdateWhere(String sqlCondition, Object... params) {
        if (sqlCondition == null || sqlCondition.isBlank()) throw new IllegalArgumentException("Conflict WHERE condition cannot be null or empty.");
        this.conflictWhereClause = sqlCondition;
        this.conflictWhereParams.clear();
        if (params != null && params.length > 0) this.conflictWhereParams.addAll(Arrays.asList(params));
        return this;
    }

    public InsertManager returningAll() {
        this.returningClause = "*";
        return this;
    }

    public InsertManager returning(String... columns) {
        if (columns == null || columns.length == 0) throw new IllegalArgumentException("returning requires at least one column.");
        this.returningClause = String.join(", ", columns);
        return this;
    }

    public int execute() {
        if (this.returningClause != null) throw new IllegalStateException("Use execute(Class<T>) when RETURNING is configured.");

        final BuiltInsertQuery built = buildQuery();
        return InteractionManager.queryUpdateCount(this.DATA_BASE, built.sql(), built.params().toArray());
    }

    public <T extends SQLSerializable<T>> Object execute(Class<T> serializer) {
        if (this.returningClause == null) throw new IllegalStateException("RETURNING must be configured before execute(Class<T>). Use returning(...) or returningAll().");

        final BuiltInsertQuery built = buildQuery();
        return InteractionManager.query(this.DATA_BASE, serializer, built.sql(), built.params().toArray());
    }

    private BuiltInsertQuery buildQuery() {
        final StringBuilder query = new StringBuilder("INSERT ");
        if(this.IGNORE_IF_EXISTS) query.append("OR IGNORE ");
        query.append("INTO ").append(TABLE.name());

        if (this.tableAlias != null) query.append(" AS ").append(this.tableAlias);

        final Set<String> columns = collectColumns();
        if (!columns.isEmpty()) query.append(" (").append(String.join(", ", columns)).append(")");

        final List<Object> params = new ArrayList<>();

        if (this.insertSelectQuery != null) {
            if (!this.VALUES.isEmpty() || !this.EXTRA_ROWS.isEmpty()) {
                throw new IllegalStateException("Cannot combine value(...) / row(...) with fromSelect(...). Choose one insertion source.");
            }
            query.append(" ").append(this.insertSelectQuery);
            params.addAll(this.INSERT_SELECT_PARAMS);
        } else {
            if (columns.isEmpty()) throw new IllegalStateException("No values configured for INSERT.");
            query.append(" VALUES ");

            appendValuesRow(query, columns, this.VALUES, params);
            for (final Map<String, ValueToken> row : this.EXTRA_ROWS) {
                query.append(", ");
                appendValuesRow(query, columns, row, params);
            }
        }

        appendConflictClause(query, params);

        if (this.returningClause != null) query.append(" RETURNING ").append(this.returningClause);

        return new BuiltInsertQuery(query.toString(), params);
    }

    private Set<String> collectColumns() {
        final Set<String> columns = new LinkedHashSet<>();
        columns.addAll(this.VALUES.keySet());
        for (final Map<String, ValueToken> row : this.EXTRA_ROWS) columns.addAll(row.keySet());
        return columns;
    }

    private static void appendValuesRow(StringBuilder query, Set<String> columns, Map<String, ValueToken> row, List<Object> params) {
        query.append("(");
        int i = 0;

        for (final String column : columns) {
            final ValueToken value = row.get(column);
            if (value == null) query.append("DEFAULT");
            else {
                query.append(value.sqlFragment());
                params.addAll(value.params());
            }
            if (i++ < columns.size() - 1) query.append(", ");
        }

        query.append(")");
    }

    private void appendConflictClause(StringBuilder query, List<Object> params) {
        if (this.conflictTarget == null && !this.conflictDoNothing && this.CONFLICT_UPDATE.isEmpty()) return;
        if (this.conflictTarget == null) throw new IllegalStateException("Conflict action was configured without onConflict(...).");

        query.append(" ON CONFLICT (").append(this.conflictTarget).append(") ");

        if (this.conflictDoNothing) {
            query.append("DO NOTHING");
            return;
        }

        if (this.CONFLICT_UPDATE.isEmpty()) throw new IllegalStateException("DO UPDATE requires at least one update expression.");

        query.append("DO UPDATE SET ");
        int i = 0;
        for (final Map.Entry<String, ValueToken> entry : this.CONFLICT_UPDATE.entrySet()) {
            query.append(entry.getKey()).append(" = ").append(entry.getValue().sqlFragment());
            params.addAll(entry.getValue().params());
            if (i++ < this.CONFLICT_UPDATE.size() - 1) query.append(", ");
        }

        if (this.conflictWhereClause != null) {
            query.append(" WHERE ").append(this.conflictWhereClause);
            params.addAll(this.conflictWhereParams);
        }
    }

    private record BuiltInsertQuery(String sql, List<Object> params) {}
}