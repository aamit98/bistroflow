package DataAccessLayer.Inventory.ControllerClasses;

import DataAccessLayer.Inventory.DTOClasses.ProductDTO;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//import static DataAccessLayer.Inventory.DBConnector.getConnection;

public class ProductController extends Controller {

    private static final String TABLE_NAME = "products";
    private static final String[] COLUMNS_NAMES = {
            "makat", "name", "place", "manufacturer", "costPrice", "currentPrice",
            "categories", "popularity", "supplierPercentage", "storePercentage",
            "items", "minimalAmount"
    };
    private static final String[] IDENTIFIERS = { "makat" };

    public ProductController() throws ClassNotFoundException {
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
    public List<ProductDTO> selectAllProductsDTOs() throws SQLException {
        List<ProductDTO> results = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NAME;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ProductDTO productDTO = (ProductDTO) convertReaderToObject(rs);
                results.add(productDTO);
            }
        }

        return results;
    }

    @Override
    public DTO convertReaderToObject(ResultSet rs) throws SQLException {
        int makat = rs.getInt("makat");
        String name = rs.getString("name");
        String place = rs.getString("place");
        String manufacturer = rs.getString("manufacturer");
        double costPrice = rs.getDouble("costPrice");
        double currentPrice = rs.getDouble("currentPrice");
        String[] categories = rs.getString("categories").split(",");
        Integer popularity = rs.getInt("popularity");
        double supplierPercentage = rs.getDouble("supplierPercentage");
        double storePercentage = rs.getDouble("storePercentage");
        List<Integer> items = parseItems(rs.getString("items"));
        int minimalAmount = rs.getInt("minimalAmount");

        return new ProductDTO(makat, name, place, manufacturer, costPrice, currentPrice,
                categories, popularity, supplierPercentage, storePercentage, items, minimalAmount, this, true);
    }

    // Helper method to parse items from database string to List<Integer>
    private List<Integer> parseItems(String itemsStr) {
        List<Integer> items = new ArrayList<>();
        if (itemsStr != null && !itemsStr.isEmpty() ) {
            String[] itemIds = itemsStr.split(",");
            for (String itemId : itemIds) {
                items.add(Integer.parseInt(itemId.trim()));
            }
        }
        return items;
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