package niwer.queryon.queries;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonException;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.SQLSerializable;

/**
 * This class allows to perform SQL queries on the database and get the result as an object.
 * The result can be null if the query doesn't return anything (e.g. UPDATE) or can be an object of type T if the query returns a result set (e.g. SELECT), or a primitive type if the query returns a single value (e.g. COUNT).
 * 
 * @author Niwer
 * @see SQLSerializable
 */
public class QueryManager {

    private static Object executeQuery(DataBase db, QueryCallback callback, String sql, boolean shouldReturnResult, Object... params) throws QueryonException {
        if (db == null) throw new IllegalArgumentException("Database cannot be null.");
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("SQL command cannot be null or empty.");

        db.reconnect(); // Ensure the connection is active before executing the query
        try(final PreparedStatement STATE = db.sqlConnection().prepareStatement(sql)) {

            /* Pass all the objects to the prepared statement. This will replace '?'' placeholders */
            for (int i = 0; i < params.length; i++) STATE.setObject(i + 1, params[i]);

            /* Execute the query and handle the result */
            if(shouldReturnResult)
                return callback.execute(STATE.executeQuery()); // Execute the query and pass the result to the callback.

            STATE.executeUpdate(); // If no result is expected, just execute the update
        } catch (SQLException e) {
            Console.log("SQL error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
            throw new QueryonException("SQL error occurred while executing query.", e);
        } catch (Exception e) {
            Console.log("Error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
            throw new QueryonException("Error occurred while executing query.", e);
        } finally {
            db.disconnect(); // Ensure the connection is closed after the query
        }
        return null;
    }

    private static <T extends SQLSerializable<T>> Object gatherSerializedResult(ResultSet result, Class<T> serializable) {
        if(result == null) return null; // If the query doesn't return a result set (e.g. UPDATE)
        if(serializable == null) return null; // If no serializer is provided, return null

        /* Try to gather the result(s) */
        try {
            final List<T> RESULTS = new ArrayList<>(); // List to hold multiple results

            while (result.next()) {
                final T SERIALIZED_OBJ = serializable.getDeclaredConstructor().newInstance();
                SERIALIZED_OBJ.objectify(result);
                RESULTS.add(SERIALIZED_OBJ); // Objectify the result and add it to the list
            }
            result.close(); // Close the result set after processing

            if (RESULTS.isEmpty()) return null; // If no results were found, return null
            if (RESULTS.size() == 1) return RESULTS.get(0); // If only one result, return it directly
            return RESULTS; // If multiple results, return the list
        } catch (Exception e) {
            throw new QueryonException("Error while serializing: " + serializable.getName(), e);
        }
    }

    /**
     * Perform an SQL query on the database without expecting a result (e.g. UPDATE, INSERT, DELETE).
     * 
     * @param db The database to perform the query on
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     */
    public static void query(DataBase db, String sql, Object... params) throws QueryonException { query(db, null, sql, params); }

    /**
     * Perform an SQL query on the database and get the result as an object of type T.
     * 
     * @param <T> The type of the object to return, which must implement SQLSerializable
     * @param db The database to perform the query on
     * @param serializable The class of the object to return, which must implement SQLSerializable
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * 
     * @return The result of the query as an object of type <code>T</code>, <br> 
     * or a <code>List of T</code> if the query returns multiple results, <br>
     * or null if the query doesn't return anything <br>
     * or if an error occurs
     */
    public static <T extends SQLSerializable<T>> Object query(DataBase db, Class<T> serializable, String sql, Object... params) throws QueryonException {
        return executeQuery(db, result -> gatherSerializedResult(result, serializable), sql, serializable != null, params);
    }

    /**
     * Perform an SQL query on the database and get the result as an object of type T, which must implement SQLSerializable.
     * 
     * @param <T> The type of the object to return, which must implement SQLSerializable
     * @param db The database to perform the query on
     * @param serializable The class of the object to return, which must implement SQLSerializable
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * @return The result of the query as an object of type <code>T</code>,
     * or null if the query doesn't return anything or if an error occurs.
     */
    public static <T extends SQLSerializable<T>> T querySerializable(DataBase db, Class<T> serializable, String sql, Object... params) throws QueryonException {
        final Object RESULT = query(db, serializable, sql, params);
        if (RESULT == null) return null;
        if (RESULT instanceof SQLSerializable ser) {
            @SuppressWarnings("unchecked")
            final T TYPED_RESULT = (T) ser; // Java doesn't allow to directly cast SQLSerializable to T, so we need to do it in two steps
            return TYPED_RESULT;
        }
        throw new IllegalStateException("Expected a result of type " + serializable.getName() + ", but got " + (RESULT != null ? RESULT.getClass().getName() : "null"));
    }

    /**
     * Perform an SQL query on the database and get the result as an object of type T.
     * 
     * @param <T> The type of the object to return, which must implement SQLSerializable
     * @param db The database to perform the query on
     * @param serializable The class of the object to return, which must implement SQLSerializable
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * 
     * @return The result of the query as a <code>List of T</code><br>
     * or an empty list if the query doesn't return anything or if an error occurs
     */
    public static <T extends SQLSerializable<T>> List<T> queryList(DataBase db, Class<T> serializable, String sql, Object... params) throws QueryonException {
        final Object RESULT = query(db, serializable, sql, params);
        if(RESULT == null) return List.of(); // If the query doesn't return anything, return an empty list
        if (RESULT instanceof List<?> lst) {
            @SuppressWarnings("unchecked")
            final List<T> TYPED_LIST = (List<T>) lst; // Java doesn't allow to directly cast List<?> to List<T>, so we need to do it in two steps
            return TYPED_LIST;
        }
        throw new IllegalStateException("An error occurred while retrieving the result list. Expected a List of " + serializable.getName() + ", but got " + (RESULT != null ? RESULT.getClass().getName() : "null"));
    }

    public static boolean queryBoolean(DataBase db, String sql, Object... params) throws QueryonException { return (boolean)queryPrimitive(db, Boolean.class, sql, params); }

    public static int queryInt(DataBase db, String sql, Object... params) throws QueryonException { return (int)queryPrimitive(db, Integer.class, sql, params); }

    public static long queryLong(DataBase db, String sql, Object... params) throws QueryonException { return (long)queryPrimitive(db, Long.class, sql, params); }

    public static double queryDouble(DataBase db, String sql, Object... params) throws QueryonException { return (double)queryPrimitive(db, Double.class, sql, params); }

    public static float queryFloat(DataBase db, String sql, Object... params) throws QueryonException { return (float)queryPrimitive(db, Float.class, sql, params); }

    public static String queryString(DataBase db, String sql, Object... params) throws QueryonException { return (String)queryPrimitive(db, String.class, sql, params); }

    /**
     * Perform an SQL query on the database and get the result as a primitive type (e.g. boolean, int, long, double, float, String).
     * This assumes that the query returns a single value (e.g. SELECT COUNT(*) FROM table) and will throw an exception if the query returns multiple values or no value.
     * 
     * @param db The database to perform the query on
     * @param primitiveType The class of the primitive type to return (e.g. boolean.class, int.class, long.class
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * 
     * @return The result of the query as a primitive type, or throws an exception if the query doesn't return a single value
     */
    public static <T> T queryPrimitive(DataBase db, Class<T> primitiveType, String sql, Object... params) throws QueryonException {
        final Object RESULT = executeQuery(db, result -> {
            try {
                if (result == null) throw new IllegalStateException("Expected a result set, but got null.");
                if (!result.next()) throw new IllegalStateException("Expected a result, but got an empty result set.");

                final Object VALUE = result.getObject(1); // Get the first column of the first row
                if (VALUE == null) return null; // If the value is null, return null

                /* Convert the value to the expected primitive type */
                if (primitiveType == boolean.class || primitiveType == Boolean.class) return result.getBoolean(1);
                if (primitiveType == int.class || primitiveType == Integer.class) return result.getInt(1);
                if (primitiveType == long.class || primitiveType == Long.class) return result.getLong(1);
                if (primitiveType == double.class || primitiveType == Double.class) return result.getDouble(1);
                if (primitiveType == float.class || primitiveType == Float.class) return result.getFloat(1);
                if (primitiveType == String.class) return result.getString(1);

                throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType.getName());
            } catch (Exception e) {
                throw new QueryonException("Error occurred while converting primitive result.", e);
            }
        }, sql, true, params);

        if (RESULT == null) throw new IllegalStateException("Expected a single value result, but got null.");
        if (primitiveType.isInstance(RESULT)) {
            @SuppressWarnings("unchecked")
            final T TYPED_RESULT = (T) RESULT; // Java doesn't allow to directly cast Object to T, so we need to do it in two steps
            return TYPED_RESULT;
        }
        throw new IllegalStateException("Expected a result of type " + primitiveType.getName() + ", but got " + RESULT.getClass().getName());
    }

    /**
     * Perform an SQL query on the database and count the number of results returned (e.g. for SELECT queries).
     * 
     * @param db The database to perform the query on
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * @return The number of results returned by the query, or 0 if the query returns no results or if an error occurs
     */
    public static int queryCountResults(DataBase db, String sql, Object... params) throws QueryonException {
        final Object RESULT = executeQuery(db, result -> {
            try {
                if (result == null) throw new IllegalStateException("Expected a result set, but got null.");
               
                int count = 0;
                while (result.next()) count++; // Count the number of rows in the result set
                return count; // Return the count of rows
            } catch (Exception e) {
                throw new QueryonException("Error occurred while counting query results.", e);
            }
        }, sql, true, params);

        if (RESULT instanceof Integer count) return count;
        throw new IllegalStateException("Expected an integer result indicating the count of results, but got " + (RESULT != null ? RESULT.getClass().getName() : "null"));
    }

    /**
     * Perform an SQL query on the database and check if it returns any result (e.g. for existence checks).
     * 
     * @param db The database to perform the query on
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * @return true if the query returns at least one result, false if it returns no results or if an error occurs
     */
    public static boolean queryHasResult(DataBase db, String sql, Object... params) throws QueryonException { return queryCountResults(db, sql, params) > 0; }
}
