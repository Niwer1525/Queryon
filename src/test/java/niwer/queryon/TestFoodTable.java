package niwer.queryon;

import niwer.queryon.tables.EnumForeginKeyAction;
import niwer.queryon.tables.Table;
import niwer.queryon.tables.api.IColumnField;
import niwer.queryon.tables.api.IForeignKey;

public class TestFoodTable extends Table {

    public TestFoodTable(DataBase db) {
        super(db);

        this.dropTable(db); // Drop the table if it already exists to ensure a clean state for testing

        /* Add multiple columns at once */
        // this.addColumn(createColumn(db, "calories", EnumColumnTypes.INT).defaultValue(25, Expression.of("calories").isLessThanOrEqualTo(125))); // Default isn't supported with @ColumnField yet, so we have to create the column manually for now
        this.addColumnsFromClass(FoodItem.class);
        this.execute(); // Execute the table creation in the database
    }

    @Override
    public String name() {
        return "food_table";
    }

    public static class FoodItem extends SQLSerializable<FoodItem> {
        @IColumnField(autoIncrement = true, primaryKey = true)
        private int id;
        
        @IColumnField(name = "name", charLimit = 255, notNull = true)
        private String name;

        @IColumnField(name = "calories", defaultValue = "25")
        private double calories;

        @IColumnField(name = "user_identifier", unique = true, foreignKey = @IForeignKey(table = TestUserTable.class, column = "id", onDelete = EnumForeginKeyAction.CASCADE))
        private int userId;

        public FoodItem() {} // Default constructor for objectification

        public FoodItem(int id, String name, double calories, int userId) {
            this.id = id;
            this.name = name;
            this.calories = calories;
            this.userId = userId;
        }

        @Override
        public int hashCode() {
            return id; // Simple hash code based on the unique ID
        }
    }
}
