package niwer.queryon.queries;

import java.sql.ResultSet;

@FunctionalInterface
public interface QueryCallback {
    Object execute(ResultSet result);
}
