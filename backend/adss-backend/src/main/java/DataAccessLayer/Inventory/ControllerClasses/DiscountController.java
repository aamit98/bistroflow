package DataAccessLayer.Inventory.ControllerClasses;

import DataAccessLayer.Inventory.DTOClasses.DiscountDTO;
import DataAccessLayer.Controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import static DataAccessLayer.Inventory.DBConnector.getConnection;

public class DiscountController extends Controller {

    private static final String TABLE_NAME = "discounts";
    private static final String[] COLUMNS_NAMES = {
            "id", "categoryNames", "makats", "discountPercentage", "fromSupplier"
    };
    private static final String[] IDENTIFIERS = {"id"};

    public DiscountController() throws ClassNotFoundException {
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

    public List<DiscountDTO> selectAllDiscountsDTOs() throws SQLException {
        List<DiscountDTO> results = new ArrayList<>();
            String query = "SELECT * FROM " + TABLE_NAME;

            try (Connection conn = DriverManager.getConnection(connectionString);
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                DiscountDTO discountDTO = (DiscountDTO) convertReaderToObject(rs);
                results.add(discountDTO);
            }
        }

        return results;
    }

    @Override
    public DiscountDTO convertReaderToObject(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        List<String> categoryNames = parseCategories(rs.getString("categoryNames"));
        List<Integer> makats = parseMakats(rs.getString("makats"));
        double discountPercentage = rs.getDouble("discountPercentage");
        boolean fromSupplier = rs.getBoolean("fromSupplier");

        return new DiscountDTO(id, categoryNames, makats, discountPercentage, fromSupplier, this, true);
    }

    // Helper method to parse category names from database string to List<String>
    private List<String> parseCategories(String categoryNamesStr) {
        if (categoryNamesStr == null || categoryNamesStr.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(categoryNamesStr.split("#"));
    }

    // Helper method to parse makats from database string to List<Integer>
    private List<Integer> parseMakats(String makatsStr) {
        List<Integer> makats = new ArrayList<>();
        if (makatsStr != null && !makatsStr.isEmpty()) {
            String[] makatIds = makatsStr.split(",");
            for (String makatId : makatIds) {
                makats.add(Integer.parseInt(makatId.trim()));
            }
        }
        return makats;
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