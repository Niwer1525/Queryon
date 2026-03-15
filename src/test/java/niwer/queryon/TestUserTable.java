package niwer.queryon;

import java.sql.ResultSet;
import java.sql.SQLException;

import niwer.queryon.tables.EnumColumnTypes;
import niwer.queryon.tables.Table;

public class TestUserTable extends Table {

    @Override
    public void register(DataBase db) {
        createTable(db, "test_table")
            .addColumns(
                createColumn("id", EnumColumnTypes.INT).autoIncrement().primaryKey(),
                createTextColumn("name", 255).notNull(),
                createColumn("age", EnumColumnTypes.INT)
            )
        .execute(); // Execute the table creation in the database
    }

    public static class TestUser implements SQLSerializable<TestUser> {
        private int id;
        private String name;
        private int age;

        public TestUser() {} // Default constructor for objectification

        public TestUser(int id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public TestUser objectify(ResultSet result) throws SQLException {
            this.id = result.getInt("id");
            this.name = result.getString("name");
            this.age = result.getInt("age");
            return this;
        }

        @Override
        public int hashCode() {
            return id; // Simple hash code based on the unique ID
        }

        public int id() { return id; }
        
        public String name() { return name; }

        public int age() { return age; }
    }
}
