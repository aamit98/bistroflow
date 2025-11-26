package DataAccessLayer.Inventory.ControllerClasses;

import DataAccessLayer.Inventory.DTOClasses.ItemDTO;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

//import static DataAccessLayer.Inventory.DBConnector.getConnection;

public class ItemController extends Controller {

    private static final String TABLE_NAME = "items";
    private static final String[] COLUMNS_NAMES = { "sn", "inStorage", "expireDate", "isDefective" };
    private static final String[] IDENTIFIERS = { "sn" };

    public ItemController() throws ClassNotFoundException {
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

    public List<ItemDTO> selectAllItemsDTOs() throws SQLException {
        List<ItemDTO> results = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_NAME;

        try (Connection conn = DriverManager.getConnection(connectionString);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ItemDTO itemDTO = (ItemDTO) convertReaderToObject(rs);
                results.add(itemDTO);
            }
        }

        return results;
    }

    @Override
    public DTO convertReaderToObject(ResultSet rs) throws SQLException {
        int sn = rs.getInt("sn");
        boolean inStorage = rs.getBoolean("inStorage");
        long timestampMillis = rs.getLong("expireDate");
        Date expireDate = new Date(timestampMillis);
        boolean isDefective = rs.getBoolean("isDefective");

        return new ItemDTO(sn, inStorage, expireDate, isDefective, this, true);
    }
}