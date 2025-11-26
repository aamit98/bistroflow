package DataAccessLayer.HR.ControllerClasses;

import DataAccessLayer.Controller;
import DataAccessLayer.DTO;
import DataAccessLayer.HR.DTOClasses.WorkScheduleDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class WorkScheduleController extends Controller {
    private static final String TABLE_NAME = "workSchedule";
    private static final String[] COLUMN_NAMES = {"branchId","date","shift","employeeId"};
    private static final String[] IDENTIFIERS = { "branchId","date" };

    public WorkScheduleController() throws ClassNotFoundException {
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
        int branchId = resultSet.getInt("branchId");
        int date = resultSet.getInt("date");
        List<Integer>[][] employeesIds = new List[7][2];
        for(int i =0;i<7;i++)
            for(int j=0;j<2;j++)
            {
                employeesIds[i][j] = new ArrayList<>();
            }
        int shift = resultSet.getInt("shift");
        int employeeId = resultSet.getInt("employeeId");
        employeesIds[shift/2][shift%2].add(employeeId);
        while (resultSet.next()&&branchId == resultSet.getInt("branchId") &&
                date == resultSet.getInt("date"))
        {
            int shift1 = resultSet.getInt("shift");
            int employeeId1 = resultSet.getInt("employeeId");
            employeesIds[shift1/2][shift1%2].add(employeeId1);
        }
        int[][][] temp = new int[7][2][];
        for(int i =0;i<7;i++)
        {
            for(int j = 0;j<2;j++)
            {
                temp[i][j] = new int[employeesIds[i][j].size()];
                int index = 0;
                for(Integer num : employeesIds[i][j])
                {
                    temp[i][j][index] = num;
                    index++;
                }
            }
        }
        return new WorkScheduleDTO(branchId,date,temp,this,true);
    }

    public List<WorkScheduleDTO> getAll() throws SQLException{
        try(Connection connection= DriverManager.getConnection(connectionString)){
            Statement stmt=connection.createStatement();
            ResultSet resultSet = stmt.executeQuery("SELECT * FROM "+TABLE_NAME + " ORDER BY branchId ASC, "
                    +"date ASC, shift ASC");

            List<WorkScheduleDTO> ans = new ArrayList<>();
            while(resultSet.next())
            {
                ans.add((WorkScheduleDTO) convertReaderToObject(resultSet));
            }
            return ans;
        }catch (Exception e){
            throw new SQLException(e);
        }

    }


}
