package niwer.queryon;

import niwer.queryon.tables.Table;

public class TestEmptyTable extends Table {

    public TestEmptyTable(DataBase db) {
        super(db);
    }

    @Override
    public String name() { return "test_table"; }

}