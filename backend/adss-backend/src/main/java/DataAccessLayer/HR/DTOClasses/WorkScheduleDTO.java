package DataAccessLayer.HR.DTOClasses;

import DataAccessLayer.DTO;
import DataAccessLayer.HR.ControllerClasses.WorkScheduleController;

public class WorkScheduleDTO extends DTO {
    private final int branchId;
    private final int date;
    private final int[][][] employeesIds;
    public WorkScheduleDTO(int branchId, int date, int[][][] employeesIds, WorkScheduleController controller, boolean fromDB) {
        super(controller, fromDB);
        this.branchId = branchId;
        this.date = date;
        this.employeesIds = employeesIds;
    }

    public int[][][] getEmployeesIds() {
        return employeesIds;
    }

    public int getBranchId(){
        return branchId;
    }

    public int getDate(){
        return date;
    }
    @Override
    public void persist() throws Exception {
            for(int i =0;i<7;i++) {
                for (int j = 0; j < 2; j++)
                {
                    for(int id : employeesIds[i][j])
                    {
                        insert(new Object[]{branchId,date,i*2+j,id});
                    }
                }
            }
            isPersisted = true;
    }
    public void delete() throws Exception {
        if (isPersisted) {
            delete(new Object[]{branchId, date});
            isPersisted = false;
        }
    }
}
