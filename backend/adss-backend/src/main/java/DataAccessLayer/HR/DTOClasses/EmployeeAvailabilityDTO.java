package DataAccessLayer.HR.DTOClasses;

import DataAccessLayer.DTO;
import DataAccessLayer.HR.ControllerClasses.EmployeeAvailabilityController;

public class EmployeeAvailabilityDTO extends DTO {

    private final int employeeId;
    private final int date;
    private final boolean[][] availabilityArray;
    public EmployeeAvailabilityDTO(int employeeId, int date, boolean[][] availabilityArray, EmployeeAvailabilityController controller, boolean fromDB) {
        super(controller, fromDB);
        this.employeeId= employeeId;
        this.availabilityArray=availabilityArray;
        this.date = date;
    }

    public int getEmployeeId(){
        return employeeId;
    }

    public boolean[][] getAvailabilityArray(){
        return availabilityArray;
    }

    public int getDate(){
        return date;
    }

    @Override
    public void persist() throws Exception {
            Object[] toInsert = new Object[16];
            toInsert[0] = employeeId;
            toInsert[1] = date;
            for(int i =0;i<7;i++)
            {
                for(int j=0;j<2;j++)
                {
                    toInsert[i*2+j+2] = availabilityArray[i][j];
                }
            }
            insert(toInsert);
            isPersisted = true;
    }
    public void deleteAvailability() throws Exception {
        if(isPersisted)
        {
            delete(new Object[]{employeeId , date});
            isPersisted = false;
        }
    }
}
