package DataAccessLayer.Inventory.ControllerClasses;

import DataAccessLayer.Inventory.DTOClasses.OrderDTO;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//import static DataAccessLayer.DBConnector.getConnection;

public class OrderController extends Controller {

    private static final String TABLE_NAME = "orders";
    private static final String[] COLUMNS_NAMES = {
            "makat", "amount", "dayOfMonth", "id"
    };
    private static final String[] IDENTIFIERS = {"id"};

    public OrderController() throws ClassNotFoundException {
        super();
    }

    @Override
    public boolean insert(Object[] attributesValues) throws Exception {
        return super.insert(COLUMNS_NAMES, attributesValues, TABLE_NAME);
    }

    @Override
    public boolean update(Object[] identifiersValues, String varToUpdate, Object valueToUpdate) throws Exception {
        return super.update(IDENTIFIERS, identifiersValues, TABLE_NAME, varToUpdate, valueToUpdate);
    }

    @Override
    public boolean delete(Object[] identifiersValues) throws Exception {
        return super.delete(IDENTIFIERS, identifiersValues, TABLE_NAME);
    }

    public List<OrderDTO> selectAllOrdersDTOs() throws SQLException {
        List<OrderDTO> results = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NAME;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                OrderDTO orderDTO = (OrderDTO) convertReaderToObject(rs);
                results.add(orderDTO);
            }
        }

        return results;
    }

    @Override
    public DTO convertReaderToObject(ResultSet rs) throws SQLException {
        int makat = rs.getInt("makat");
        int amount = rs.getInt("amount");
        int dayOfMonth = rs.getInt("dayOfMonth");
        int id = rs.getInt("id");

        return new OrderDTO(makat, amount, dayOfMonth, id, this, true);
    }

    // Helper method to execute insert, update, delete queries
    private boolean executeUpdate(String query, Object[] params) throws SQLException {
        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
        }
    }
}