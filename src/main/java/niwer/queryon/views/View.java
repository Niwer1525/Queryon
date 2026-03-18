package niwer.queryon.views;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.queries.QueryManager;

public abstract class View {

    private final DataBase DATA_BASE;

    protected View(DataBase db) {
        this.DATA_BASE = db;
    }

    public abstract String name();

    /**
     * Drop this view
     * This is useful when you want to remove a view from the database, for example when you want to update the view definition or when you want to clean up the database by removing unused views.
     * 
     * @param db The DataBase instance to which the table belongs
     * @return The Table instance for chaining
     */
    public final View dropView() {
        Console.log("Dropping table " + this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        QueryManager.query(this.DATA_BASE, "DROP VIEW IF EXISTS " + this.name());
        return this;
    }
}
