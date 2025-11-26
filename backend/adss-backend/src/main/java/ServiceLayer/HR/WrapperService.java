package ServiceLayer.HR;
import DomainLayer.HR.*;

import java.util.*;

public class WrapperService {
    public final HRManagerService hrManagerService;
    public final EmployeeService employeeService;


    public WrapperService() throws ClassNotFoundException {
        int maxBranchId = 9; // 0 .. 9
        HashMap<Integer, Employee> employeesDict = new HashMap<>();
        HashMap<Integer, WorkSchedule>[] workScheduleHashMap = new HashMap[maxBranchId + 2];
        HashMap<Integer, HashMap<Employee, EmployeeAvailability>>[] employeesAvailabilityDict = new HashMap[maxBranchId + 2];
        HashMap<Integer, WeeklyRoleConstraints>[] weeklyRoleConstraintsDict = new HashMap[maxBranchId + 2];
        for (int i = 0; i <= maxBranchId; i++) {
            workScheduleHashMap[i] = new HashMap<>();
            employeesAvailabilityDict[i] = new HashMap<>();
            weeklyRoleConstraintsDict[i] = new HashMap<>();
        }
        hrManagerService = new HRManagerService(new HRManagerFacade(employeesDict, workScheduleHashMap, employeesAvailabilityDict, weeklyRoleConstraintsDict, maxBranchId));
        employeeService = new EmployeeService(new EmployeeFacade(employeesDict, workScheduleHashMap, employeesAvailabilityDict));
    }

    /**
     * for tests
     */
    protected WrapperService(HRManagerService hrManagerService , EmployeeService employeeService) throws ClassNotFoundException {
        this.hrManagerService = hrManagerService;
        this.employeeService = employeeService;
    }
    public void loadDataFromDB() throws Exception {
        hrManagerService.loadDataFromDB();

    }

}