package niwer.queryon;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import niwer.lumen.Console;
import niwer.queryon.tables.Table;

public class DataBaseSync {

    protected static List<String> fetchLiveTableNames(DataBase db) {
        final List<String> TABLES_NAMES = new ArrayList<>();
        final String SQL = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%';";

        db.reconnect(); // Ensure an open connection exists

        /* Execute the query and populate the list of table names */
        try (Statement STATEMENT = db.sqlConnection().createStatement(); ResultSet RESULT_SET = STATEMENT.executeQuery(SQL)) {
            while (RESULT_SET.next()) TABLES_NAMES.add(RESULT_SET.getString("name"));
        } catch (SQLException e) {
            Console.log("Failed to fetch live table names: " + e.getMessage()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        }

        return TABLES_NAMES;
    }

    protected static List<String> fetchLiveColumns(DataBase db, Table table) {
        final List<String> COLUMN_NAMES = new ArrayList<>();
        final String SQL = "PRAGMA table_info(" + QueryonEngine.escapeString(table.name()) + ");";

        db.reconnect();

        try (Statement STATEMENT = db.sqlConnection().createStatement(); ResultSet RESULT_SET = STATEMENT.executeQuery(SQL)) {
            while (RESULT_SET.next()) COLUMN_NAMES.add(RESULT_SET.getString("name")); // PRAGMA table_info returns columns: cid, name, type, notnull, dflt_value, pk
        } catch (SQLException e) {
            Console.log("Failed to fetch live columns for table " + table.name() + ": " + e.getMessage()).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        }

        return COLUMN_NAMES;
    }
}
