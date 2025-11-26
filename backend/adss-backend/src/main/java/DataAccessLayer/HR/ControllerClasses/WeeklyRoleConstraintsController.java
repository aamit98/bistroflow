package DataAccessLayer.HR.ControllerClasses;

import DataAccessLayer.Controller;
import DataAccessLayer.DTO;
import DataAccessLayer.HR.DTOClasses.WeeklyRoleConstraintsDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WeeklyRoleConstraintsController extends Controller {
    private static final String TABLE_NAME = "WeeklyRoleConstraints";
    private static final String[] COLUMN_NAMES = {"date","branchId","shift","roleId","roleAmount"};
    private static final String[] IDENTIFIERS = {"date","branchId"};

    public WeeklyRoleConstraintsController() throws ClassNotFoundException {
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
       int date=resultSet.getInt("date");
       int branchId=resultSet.getInt("branchId");
       int[][][] rolesIdsArray=new int[7][2][];
       List<Integer>[][] tempRoleLists=new ArrayList[7][2];
       for(int i=0;i<7;i++){
           for(int j=0;j<2;j++) {
               tempRoleLists[i][j] = new ArrayList<>();

           }
       }
       do{
           int roleId=resultSet.getInt("roleId");
           int roleAmount=resultSet.getInt("roleAmount");
           for(int i=0;i<roleAmount;i++){
               tempRoleLists[date%7][date/7].add(roleId);

           }
       }while (resultSet.next() && resultSet.getInt("branchId")==branchId && resultSet.getInt("date")==date);
       return new WeeklyRoleConstraintsDTO(rolesIdsArray,branchId,date,this,true);


    }

    public List<WeeklyRoleConstraintsDTO> getAll() throws SQLException {
        try (Connection connection = DriverManager.getConnection(connectionString)) {
            Statement stmt = connection.createStatement();
             ResultSet resultSet = stmt.executeQuery("SELECT * FROM " + TABLE_NAME);
             List< WeeklyRoleConstraintsDTO> ans = new ArrayList<>();
             while(resultSet.next())
             {
                 ans.add((WeeklyRoleConstraintsDTO)convertReaderToObject(resultSet));
             }
             return ans;
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
