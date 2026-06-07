package niwer.queryon.queries.interaction;

import niwer.queryon.DataBase;
import niwer.queryon.tables.Table;

/**
 * Abstract class for executing queries on a database. It provides common functionality for building and executing queries.
 * 
 * @author Niwer
 */
public abstract class QueryExecutor {

    protected final DataBase DATA_BASE;
    protected final Table TABLE;

    protected QueryExecutor(DataBase dataBase, Class<? extends Table> tableClas) {
        this.DATA_BASE = dataBase;
        this.TABLE = dataBase.getTable(tableClas);
    }
    
    @Override
    public String toString() {
        final String REFERENCE = this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
        return String.format("%s : %s", REFERENCE, buildQuery());
    }

    protected abstract String buildQuery();
}