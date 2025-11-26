package DataAccessLayer.HR.ControllerClasses;

import DataAccessLayer.Controller;
import DataAccessLayer.DTO;
import DataAccessLayer.HR.DTOClasses.EmployeeAvailabilityDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmployeeAvailabilityController extends Controller {
    private static final String TABLE_NAME = "employeesAvailability";
    private static final String[] COLUMN_NAMES = {"employeeId","date","sundayMorning","sundayEvening",
            "mondayMorning","mondayEvening","tuesdayMorning","tuesdayEvening",
            "wednesdayMorning","wednesdayEvening","thursdayMorning","thursdayEvening",
            "fridayMorning","fridayEvening","saturdayMorning","saturdayEvening"};
    private static final String[] IDENTIFIERS = { "employeeId" , "date" };

    public EmployeeAvailabilityController() throws ClassNotFoundException {
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
      int employeeId=resultSet.getInt("employeeId");
      int date =resultSet.getInt("date");
      boolean[][] availabilityArray=new boolean[7][2];
      availabilityArray[0][0] = resultSet.getBoolean("sundayMorning");
      availabilityArray[0][1] = resultSet.getBoolean("sundayEvening");
      availabilityArray[1][0] = resultSet.getBoolean("mondayMorning");
      availabilityArray[1][1] = resultSet.getBoolean("mondayEvening");
      availabilityArray[2][0] = resultSet.getBoolean("tuesdayMorning");
      availabilityArray[2][1] = resultSet.getBoolean("tuesdayEvening");
      availabilityArray[3][0] = resultSet.getBoolean("wednesdayMorning");
      availabilityArray[3][1] = resultSet.getBoolean("wednesdayEvening");
      availabilityArray[4][0] = resultSet.getBoolean("thursdayMorning");
      availabilityArray[4][1] = resultSet.getBoolean("thursdayEvening");
      availabilityArray[5][0] = resultSet.getBoolean("fridayMorning");
      availabilityArray[5][1] = resultSet.getBoolean("fridayEvening");
      availabilityArray[6][0] = resultSet.getBoolean("saturdayMorning");
      availabilityArray[6][1] = resultSet.getBoolean("saturdayEvening");
      return new EmployeeAvailabilityDTO(employeeId,date,availabilityArray,this,true);


    }
    public List<EmployeeAvailabilityDTO> getAll() throws SQLException{
        try(Connection connection= DriverManager.getConnection(connectionString)){
            Statement stmt=connection.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT * FROM "+TABLE_NAME);
            List<EmployeeAvailabilityDTO> ans = new ArrayList<>();
            while(resultSet.next())
            {
                ans.add((EmployeeAvailabilityDTO) convertReaderToObject(resultSet));
            }
            return ans;
        }catch (Exception e){
            throw new SQLException(e);
        }
    }

}
