package niwer.queryon.interactions;

import java.sql.ResultSet;

@FunctionalInterface
public interface QueryCallback {
    Object execute(ResultSet result);
}
