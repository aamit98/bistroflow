package DataAccessLayer;

import java.io.IOException;
import java.sql.*;
import java.sql.Statement;
public abstract class Controller {

    private static final String path;

    static {
        try {
            path = new java.io.File(".").getCanonicalPath() + "\\InventoryHR.db";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String connectionString;

    protected Controller() throws ClassNotFoundException {

        Class.forName("org.sqlite.JDBC");
        this.connectionString = "jdbc:sqlite:" + path;
    }

    public abstract boolean insert(Object[] attributesValues) throws Exception;

    public abstract boolean update(Object[] identifiersValues, String varToUpdate, Object valueToUpdate) throws Exception;

    public abstract boolean delete(Object[] identifiersValues) throws Exception;

    public abstract DTO convertReaderToObject(ResultSet resultSet) throws SQLException;

    protected boolean insert(String[] attributesNames, Object[] attributeValues, String tableName) throws Exception {
        if (attributesNames.length != attributeValues.length) {
            throw new Exception("amount of attributes and values differ!");
        }

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            StringBuilder commandText = new StringBuilder("INSERT INTO " + tableName + " (");

            for (int i = 0; i < attributesNames.length; i++) {
                commandText.append(attributesNames[i]);
                if (i < attributesNames.length - 1) {
                    commandText.append(",");
                } else {
                    commandText.append(") VALUES (");
                }
            }

            for (int i = 0; i < attributeValues.length; i++) {
                commandText.append("?");
                if (i < attributeValues.length - 1) {
                    commandText.append(",");
                } else {
                    commandText.append(");");
                }
            }
            Statement s = connection.createStatement();
            s.executeUpdate("PRAGMA foreign_keys = ON; ");
            try (PreparedStatement command = connection.prepareStatement(commandText.toString())) {
                for (int i = 0; i < attributeValues.length; i++) {
                    command.setObject(i + 1, attributeValues[i]);
                }

                int res = command.executeUpdate();
                return res == 1;
            } catch (SQLException e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    protected boolean update(String[] identifiers, Object[] identifiersValues, String tableName, String varToUpdate, Object valueToUpdate) throws Exception {
        if (identifiers.length != identifiersValues.length) {
            throw new Exception("amount of identifiers and values differ!");
        }

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            StringBuilder commandText = new StringBuilder("UPDATE " + tableName + " SET " + varToUpdate + " = ? WHERE ");

            for (int i = 0; i < identifiersValues.length; i++) {
                commandText.append(identifiers[i]).append(" = ?");
                if (i < identifiersValues.length - 1) {
                    commandText.append(" AND ");
                }
            }
            Statement s = connection.createStatement();
            s.executeUpdate("PRAGMA foreign_keys = ON; ");
            try (PreparedStatement command = connection.prepareStatement(commandText.toString())) {
                command.setObject(1, valueToUpdate);
                for (int i = 0; i < identifiersValues.length; i++) {
                    command.setObject(i + 2, identifiersValues[i]);
                }

                int res = command.executeUpdate();
                return res == 1;
            } catch (SQLException e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    protected boolean delete(String[] identifiers, Object[] identifiersValues, String tableName) throws Exception {
        if (identifiers.length != identifiersValues.length) {
            throw new Exception("amount of identifiers and values differ!");
        }

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            StringBuilder commandText = new StringBuilder("DELETE FROM " + tableName + " WHERE ");

            for (int i = 0; i < identifiersValues.length; i++) {
                commandText.append(identifiers[i]).append(" = ?");
                if (i < identifiersValues.length - 1) {
                    commandText.append(" AND ");
                }
            }
            Statement s = connection.createStatement();
            s.executeUpdate("PRAGMA foreign_keys = ON; ");
            try (PreparedStatement command = connection.prepareStatement(commandText.toString())) {
                for (int i = 0; i < identifiersValues.length; i++) {
                    command.setObject(i + 1, identifiersValues[i]);
                }

                int res = command.executeUpdate();
                return res == 1;
            } catch (SQLException e) {
                throw new Exception(e.getMessage());
            }
        }
    }

    /**
     * deletes all data in the DB (for all tables)
     * @throws Exception
     */
    public void deleteAllHr() throws Exception {
        String[] tables = {
                "employeesAvailability", "employee", "employeesRoles", "WeeklyRoleConstraints", "workSchedule",
        };

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            for (String table : tables) {
                try (Statement command = connection.createStatement()) {
                    command.executeUpdate("DELETE FROM " + table);
                } catch (SQLException e) {
                    throw new Exception(e.getMessage());
                }
            }
        }
    }
    public void deleteAllInventory() throws Exception {
        String[] tables = {
                "discounts", "items", "orders", "products"
        };

        try (Connection connection = DriverManager.getConnection(connectionString)) {
            for (String table : tables) {
                try (Statement command = connection.createStatement()) {
                    command.executeUpdate("DELETE FROM " + table);
                } catch (SQLException e) {
                    throw new Exception(e.getMessage());
                }
            }
        }
    }
}
