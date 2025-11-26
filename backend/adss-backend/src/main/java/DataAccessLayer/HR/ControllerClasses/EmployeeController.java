package DataAccessLayer.HR.ControllerClasses;

import DataAccessLayer.Controller;
import DataAccessLayer.DTO;
import DataAccessLayer.HR.DTOClasses.EmployeeDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeController extends Controller {
    private static final String TABLE_NAME = "employee";
    private static final String[] COLUMN_NAMES = {"id","hasAccess","password","isHRManager","isDriver",
            "branchId","termsOfEmployment","name","startDate","bankCode","bankBranchCode","bankAccount"
            ,"hourlyRate","monthlyRate"};
    private static final String[] IDENTIFIERS = {"id"};

    public EmployeeController() throws ClassNotFoundException {
        super();
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
        return new EmployeeDTO(
                resultSet.getInt("id"),
                resultSet.getBoolean("hasAccess"),
                resultSet.getString("password"),
                resultSet.getBoolean("isHRManager"),
                resultSet.getBoolean("isDriver"),
                resultSet.getInt("branchId"),
                resultSet.getString("termsOfEmployment"),
                resultSet.getString("name"),
                resultSet.getInt("startDate"),
                resultSet.getInt("bankCode"),
                resultSet.getInt("bankBranchCode"),
                resultSet.getInt("bankAccount"),
                resultSet.getInt("hourlyRate"),
                resultSet.getInt("monthlyRate"),
                this,
                true
        );
    }
    public List<EmployeeDTO> getAll() throws SQLException{
        List<EmployeeDTO> ans = new ArrayList<>();
        try(Connection connection= DriverManager.getConnection(connectionString)){
            Statement stmt=connection.createStatement();
            ResultSet resultSet  = stmt.executeQuery("SELECT * FROM "+TABLE_NAME);
            while(resultSet.next())
            {
                ans.add((EmployeeDTO) convertReaderToObject(resultSet));
            }
            return ans;
        }catch (Exception e){
            throw new SQLException(e);
        }

    }

}
