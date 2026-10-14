package niwer.queryon;

import niwer.queryon.QueryonEngineTest.TestEnum;
import niwer.queryon.tables.Table;
import niwer.queryon.tables.api.IColumnField;

public class TestTable extends Table {

    public TestTable(DataBase db) {
        super(db);
        addColumnsFromClass(TestTableRow.class).execute();
    }

    @Override
    public String name() { return "test_table"; }

    public static class TestTableRow extends SQLSerializable<TestTableRow> {
        @IColumnField
        public int id;

        @IColumnField
        public TestEnum testEnum;
    }
}
