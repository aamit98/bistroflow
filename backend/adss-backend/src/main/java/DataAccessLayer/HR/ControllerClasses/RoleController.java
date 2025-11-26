package DataAccessLayer.HR.ControllerClasses;

import DataAccessLayer.Controller;
import DataAccessLayer.DTO;
import DataAccessLayer.HR.DTOClasses.RoleDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RoleController extends Controller {
    private static final String TABLE_NAME = "employeesRoles";
    private static final String[] COLUMN_NAMES = {"employeeId","roleId"};
    private static final String[] IDENTIFIERS = {"employeeId"};

    public RoleController() throws ClassNotFoundException {
    }

    @Override
    public boolean insert(Object[] attributesValues) throws Exception {
        return super.insert(COLUMN_NAMES, attributesValues, TABLE_NAME);

    }

    @Override
    public boolean update(Object[] identifiersValues, String varToUpdate, Object valueToUpdate) throws Exception {
        return super.update(IDENTIFIERS, identifiersValues, TABLE_NAME, varToUpdate, valueToUpdate);
    }

    @Override
    public boolean delete(Object[] identifiersValues) throws Exception {
        return super.delete(IDENTIFIERS, identifiersValues, TABLE_NAME);
    }

    @Override
    public DTO convertReaderToObject(ResultSet resultSet) throws SQLException {
        int employeeId = resultSet.getInt("employeeId");
        int roleOrdinal = resultSet.getInt("roleId");
        return new RoleDTO(employeeId, roleOrdinal, this, true);
    }
    public List<RoleDTO> getAllEmployeeRoles(int employeeId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stmt = connection.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + TABLE_NAME + " where employeeId = "+ employeeId);
            List<RoleDTO>  roles = new ArrayList<>();
            while(resultSet.next())
            {
                roles.add((RoleDTO)convertReaderToObject(resultSet));
            }
            return roles;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
