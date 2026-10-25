package niwer.queryon.queries.interaction;

import java.util.ArrayList;
import java.util.List;

import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.queries.Expression;
import niwer.queryon.tables.Table;

/**
 * Abstract class for executing queries on a database. It provides common functionality for building and executing queries.
 * 
 * @author Niwer
 */
public abstract class QueryExecutor {

    protected final DataBase DATA_BASE;
    protected final Table TABLE;
    private final List<Object> PARAMETERS = new ArrayList<>();

    protected QueryExecutor(DataBase dataBase, Class<? extends Table> tableClas) {
        this.DATA_BASE = dataBase;
        this.TABLE = dataBase.getTable(tableClas);
    }
    
    @Override
    public String toString() {
        final String REFERENCE = this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
        return String.format("%s : %s", REFERENCE, buildQuery());
    }

    protected final void clearParameters() { PARAMETERS.clear(); }

    /**
     * Add a parameter to the query. This method is used to add parameters for prepared statements.
     * 
     * @param parameter The parameter to be added to the query
     */
    protected final void addParameter(Object parameter) {
        PARAMETERS.add(QueryonEngine.prepareParam(parameter));
    }

    /**
     * Add a list of parameters to the query. This method is used to add multiple parameters for prepared statements.
     * 
     * @param parameters The list of parameters to be added to the query
     */
    protected final void addParameters(List<Object> parameters) {
        if (parameters != null) for (Object param : parameters) this.addParameter(param);
    }

    protected final void addParameters(QueryExecutor other) {
        if (other != null) this.addParameters(List.copyOf(other.PARAMETERS));
    }

    protected final void addParameters(Expression expression) {
        this.addParameters(expression.parameters());
    }

    protected final Object[] parameters() { return PARAMETERS.toArray(); }

    protected abstract String buildQuery();
}