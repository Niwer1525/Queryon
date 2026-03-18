package niwer.queryon.triggers;

import niwer.lumen.Console;
import niwer.queryon.DataBase;
import niwer.queryon.QueryonEngine;
import niwer.queryon.QueryonLogTypes;
import niwer.queryon.queries.QueryManager;

public abstract class Trigger {

    private final DataBase DATA_BASE;

    protected Trigger(DataBase db) {
        this.DATA_BASE = db;
    }

    public abstract String name();

    /**
     * Drop this trigger
     * This is useful when you want to remove a trigger from the database, for example when you want to update the trigger definition or when you want to clean up the database by removing unused triggers.
     * 
     * @param db The DataBase instance to which the table belongs
     * @return The Table instance for chaining
     */
    public final Trigger dropTrigger() {
        Console.log("Dropping table " + this.name()).type(QueryonLogTypes.SQL).container(QueryonEngine.LOGGER).send();
        QueryManager.query(this.DATA_BASE, "DROP TRIGGER IF EXISTS " + this.name());
        return this;
    }
}
