package niwer.queryon;

import niwer.queryon.queries.Expression;
import niwer.queryon.tables.EnumColumnTypes;
import niwer.queryon.tables.EnumForeginKeyAction;
import niwer.queryon.tables.Table;

public class TestFoodTable extends Table {

    public TestFoodTable(DataBase db) {
        super(db);

        this.dropTable(db); // Drop the table if it already exists to ensure a clean state for testing

        /* Add multiple columns at once */
        this.addColumns(
            createColumn(db, "id", EnumColumnTypes.INT).autoIncrement().primaryKey(),
            createColumn(db, "name", 255).notNull(),
            createColumn(db, "calories", EnumColumnTypes.INT).defaultValue(25, Expression.of("calories").isLessThanOrEqualTo(125)),
            createColumn(db, "user_identifier", EnumColumnTypes.INT).unique().foreignKey(TestUserTable.class, "id", EnumForeginKeyAction.CASCADE)
        )
        .execute(); // Execute the table creation in the database
    }

    @Override
    public String name() {
        return "food_table";
    }
}
