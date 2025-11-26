package DomainLayer.HR;

import DataAccessLayer.HR.ControllerClasses.WorkScheduleController;
import DataAccessLayer.HR.DTOClasses.WorkScheduleDTO;

import java.util.List;

public class WorkSchedule {

    public final List<Employee>[][] schedule;
    private final WorkScheduleDTO workScheduleDTO;

    public WorkSchedule(List<Employee>[][] schedule,int branchId,int date, WorkScheduleController controller, boolean fromDB)
    {
        this.schedule = schedule;
        int[][][] idsArray = new int[7][2][];
        for(int i =0;i<7;i++) {
            for (int j = 0; j < 2; j++)
            {
                idsArray[i][j] = new int[schedule[i][j].size()];
                int index = 0;
                for(Employee e : schedule[i][j])
                {
                    idsArray[i][j][index] = e.getId();
                    index++;
                }
            }
        }
        workScheduleDTO = new WorkScheduleDTO(branchId,date,idsArray,controller,fromDB);
    }
    public void persistDTO() throws Exception {
        workScheduleDTO.persist();
    }
}

