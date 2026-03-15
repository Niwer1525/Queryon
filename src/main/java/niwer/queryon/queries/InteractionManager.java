package niwer.queryon.queries;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.SQLSerializable;

/**
 * This class allows to perform SQL queries on the database and get the result as an object.
 * The result can be null if the query doesn't return anything (e.g. UPDATE) or can be an object of type T if the query returns a result set (e.g. SELECT), or a primitive type if the query returns a single value (e.g. COUNT).
 * 
 * @author Niwer
 * @see SQLSerializable
 */
public class InteractionManager {

    private static Object executeQuery(DataBase db, QueryCallback callback, String sql, boolean shouldReturnResult, Object... params) {
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
        } catch (Exception e) {
            Console.log("Error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
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
                RESULTS.add(SERIALIZED_OBJ.objectify(result)); // Objectify the result and add it to the list
            }
            result.close(); // Close the result set after processing

            /* Log the number of results returned */
            // Console.log("Query returned %d result(s).", RESULTS.size()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
            
            if (RESULTS.isEmpty()) return null; // If no results were found, return null
            if (RESULTS.size() == 1) return RESULTS.get(0); // If only one result, return it directly
            return RESULTS; // If multiple results, return the list
        } catch (Exception e) {
            Console.log("Error while serializing : " + serializable.getName(), e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        }
        return null;
    }

    /**
     * Perform an SQL query on the database without expecting a result (e.g. UPDATE, INSERT, DELETE).
     * 
     * @param db The database to perform the query on
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     */
    public static void query(DataBase db, String sql, Object... params) { query(db, null, sql, params); }

    /**
     * Perform an SQL write query and return the number of affected rows.
     *
     * @param db The database to perform the query on
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * @return Number of affected rows
     */
    @Deprecated
    public static int queryUpdateCount(DataBase db, String sql, Object... params) {
        if (db == null) throw new IllegalArgumentException("Database cannot be null.");
        if (sql == null || sql.isEmpty()) throw new IllegalArgumentException("SQL command cannot be null or empty.");

        db.reconnect();
        try(final PreparedStatement STATE = db.sqlConnection().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) STATE.setObject(i + 1, params[i]);
            return STATE.executeUpdate();
        } catch (SQLException e) {
            Console.log("SQL error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        } catch (Exception e) {
            Console.log("Error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
        } finally {
            db.disconnect();
        }
        return 0;
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
     * @return The result of the query as an object of type <code>T</code>, <br> 
     * or a <code>List of T</code> if the query returns multiple results, <br>
     * or null if the query doesn't return anything <br>
     * or if an error occurs
     */
    public static <T extends SQLSerializable<T>> Object query(DataBase db, Class<T> serializable, String sql, Object... params) {
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
    public static <T extends SQLSerializable<T>> T querySerializable(DataBase db, Class<T> serializable, String sql, Object... params) {
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
     * or null if the query doesn't return anything <br>
     * or if an error occurs
     */
    public static <T extends SQLSerializable<T>> List<T> queryList(DataBase db, Class<T> serializable, String sql, Object... params) {
        final Object RESULT = query(db, serializable, sql, params);
        if (RESULT instanceof List<?> lst) {
            @SuppressWarnings("unchecked")
            final List<T> TYPED_LIST = (List<T>) lst; // Java doesn't allow to directly cast List<?> to List<T>, so we need to do it in two steps
            return TYPED_LIST;
        }
        throw new IllegalStateException("Expected a list of results, but got a single result. Use the query() method instead.");
    }

    public static boolean queryBoolean(DataBase db, String sql, Object... params) {
        return (boolean)queryPrimitive(db, boolean.class, sql, params);
    }

    public static int queryInt(DataBase db, String sql, Object... params) {
        return (int)queryPrimitive(db, int.class, sql, params);
    }

    public static long queryLong(DataBase db, String sql, Object... params) {
        return (long)queryPrimitive(db, long.class, sql, params);
    }

    public static double queryDouble(DataBase db, String sql, Object... params) {
        return (double)queryPrimitive(db, double.class, sql, params);
    }

    public static float queryFloat(DataBase db, String sql, Object... params) {
        return (float)queryPrimitive(db, float.class, sql, params);
    }

    public static String queryString(DataBase db, String sql, Object... params) {
        return (String)queryPrimitive(db, String.class, sql, params);
    }

    /**
     * Perform an SQL query on the database and get the result as a primitive type (e.g. boolean, int, long, double, float, String).
     * 
     * @param db The database to perform the query on
     * @param primitiveType The class of the primitive type to return (e.g. boolean.class, int.class, long.class
     * @param sql The SQL query to perform, with '?' placeholders for parameters
     * @param params The parameters to set in the prepared statement, in the order of the placeholders
     * 
     * @return The result of the query as a primitive type, or throws an exception if the query doesn't return a single value
     */
    public static Object queryPrimitive(DataBase db, Class<?> primitiveType, String sql, Object... params) {
        final Object RESULT = executeQuery(db, result -> {
            try {
                if (result == null) throw new IllegalStateException("Expected a result set, but got null.");
                if (!result.next()) throw new IllegalStateException("Expected a result, but got an empty result set.");

                Object value = result.getObject(1); // Get the first column of the first row
                if (value == null) return null; // If the value is null, return null

                /* Convert the value to the expected primitive type */
                if (primitiveType == boolean.class || primitiveType == Boolean.class) return result.getBoolean(1);
                if (primitiveType == int.class || primitiveType == Integer.class) return result.getInt(1);
                if (primitiveType == long.class || primitiveType == Long.class) return result.getLong(1);
                if (primitiveType == double.class || primitiveType == Double.class) return result.getDouble(1);
                if (primitiveType == float.class || primitiveType == Float.class) return result.getFloat(1);
                if (primitiveType == String.class) return result.getString(1);

                throw new IllegalArgumentException("Unsupported primitive type: " + primitiveType.getName());
            } catch (SQLException e) {
                Console.log("SQL error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
            } catch (Exception e) {
                Console.log("Error occurred while executing query.", e).type(QueryonLogTypes.SQL).error().container(QueryonEngine.LOGGER).send();
            }
            return null;
        }, sql, true, params);

        if (RESULT == null) throw new IllegalStateException("Expected a single value result, but got null.");
        return RESULT;
    }
}
