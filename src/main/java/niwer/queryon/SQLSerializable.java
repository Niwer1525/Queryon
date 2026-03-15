package niwer.queryon;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * This interface allows you to create objects that will represent rows in the database.
 * 
 * @note Classes that implement this interface must have a default constructor (no parameters) to allow instantiation during objectification.
 */
public interface SQLSerializable<T> {

    /**
     * This method will convert a ResultSet row into an object of type T.
     * @param resultSet The ResultSet to objectify
     * @return The object of type T
     * @throws SQLException if an SQL error occurs
     */
    T objectify(ResultSet resultSet) throws SQLException;
}