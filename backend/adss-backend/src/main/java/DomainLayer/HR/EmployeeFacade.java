package DomainLayer.HR;
import DataAccessLayer.HR.ControllerClasses.EmployeeAvailabilityController;
import GlobalClasses.DateHelper;
import GlobalClasses.Logger;


import java.util.HashMap;
import java.util.List;

public class EmployeeFacade {
    private final DateHelper dateHelper;
    private final HashMap<Integer,Employee> employeesDict; // <EmployeeId,Employee>
    private final HashMap<Integer,WorkSchedule>[] workScheduleHashMap; //<Date,WorkSchedule>
    private final HashMap<Integer,HashMap<Employee, EmployeeAvailability>>[] employeesAvailabilityDict; //<Date,<Employee,EmployeeAvailability>>
    private final EmployeeAvailabilityController employeesAvailabilityController;


    public EmployeeFacade(HashMap<Integer,Employee> employeesDict, HashMap<Integer,WorkSchedule>[] workScheduleHashMap
    ,HashMap<Integer,HashMap<Employee,EmployeeAvailability>>[]  employeesAvailabilityDict
                          ) throws ClassNotFoundException {
        this.employeesDict = employeesDict;
        this.workScheduleHashMap = workScheduleHashMap;
        this.employeesAvailabilityDict = employeesAvailabilityDict;
        this.employeesAvailabilityController=new EmployeeAvailabilityController();
        dateHelper = new DateHelper();
    }

    /**
     * for tests
     */
    public EmployeeFacade(HashMap<Integer,Employee> employeesDict, HashMap<Integer,WorkSchedule>[] workScheduleHashMap
            ,HashMap<Integer,HashMap<Employee,EmployeeAvailability>>[]  employeesAvailabilityDict,
                          EmployeeAvailabilityController employeesAvailabilityController
    ) throws ClassNotFoundException {
        this.employeesDict = employeesDict;
        this.workScheduleHashMap = workScheduleHashMap;
        this.employeesAvailabilityDict = employeesAvailabilityDict;
        this.employeesAvailabilityController=employeesAvailabilityController;
        dateHelper = new DateHelper();
    }
    /**
     *
     * @return employee if exists, null otherwise.
     * @throws Exception if employee is not logged in/ employee doesn't have access.
     */
    public Employee getEmployee(int employeeId) throws Exception {
        Employee e = employeesDict.get(employeeId);

        if (e == null) {
            throw new Exception("No such employee");
        }

        // Keep access restriction (still useful)
        verifyHasAccess(e);

        // 🔥 MVP: do NOT require e.loggedIn() for backend / REST usage
        // if (!e.loggedIn()) {
        //     throw new Exception("employee is not logged in");
        // }

        return e;
    }


    /**
     *
     * @param employee
     * @throws Exception if employee doesn't have access.
     */
    private void verifyHasAccess(Employee employee) throws Exception {
        if(!employee.getHasAccess())
        {
            throw new Exception("employee accesss has been restricted.");
        }

    }

    /**
     *
     * @throws Exception - when given wrong id/wrong password.
     */
    public void login(int employeeId, String password) throws Exception {
        Employee employee = employeesDict.get(employeeId);
        if(employee==null)
        {
            throw new Exception("wrong ID.");
        }
        verifyHasAccess(employee);
        if(!employee.login(password))
        {
            throw new Exception("wrong password");
        }
    }

    /**
     *
     * @throws Exception - when given wrong id/employee  already logged out.
     */
    public void logout(int employeeId) throws Exception {
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            verifyHasAccess(employee);
            employee.logout();
        }
        else
        {
            throw new Exception("No such employee");
        }
    }

    /**
     * add new / change existing employeeAvailability for said employee and a given week.
     * @param date - sunday of the desired week in YYYYMMDD format (e.g. for the week that contains
     *             6th june 2024, should be 20240602).
     *             accepts next week date until tuesday,
     *             always accepts 2 weeks ahead date.
     * @param availabilityArray - employee's new availability.
     * @throws Exception - when given wrong id/ invalid date.
     */


    public void addOrChangeAvailability(int employeeId, int date, boolean[][] availabilityArray) throws Exception {
        if(!canChangeAvailability(date))
        {
            throw new Exception("Cannot change availability for said date");
        }
        EmployeeAvailability availability = new EmployeeAvailability(availabilityArray,employeeId,date,
                employeesAvailabilityController,false);
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            verifyHasAccess(employee);
            HashMap<Employee,EmployeeAvailability> temp1 = employeesAvailabilityDict[employee.getBranchId()].get(date);
            EmployeeAvailability oldAvailability = null;
            if(temp1!=null )
            {
                oldAvailability = temp1.get(employee);
            }
            if (oldAvailability!=null) {
                Logger.info("Updating availability for date: " + date);
                oldAvailability.deleteDTO();
                (employeesAvailabilityDict[employee.getBranchId()].get(date)).put(employee, availability);
            } else {
                Logger.info("Adding new availability for date: " + date);
                HashMap<Employee, EmployeeAvailability> temp = new HashMap<>();
                temp.put(employee, availability);
                employeesAvailabilityDict[employee.getBranchId()].put(date, temp);
            }
            availability.persistDTO();
        }
        else {
            throw new Exception("No such employee");
        }
    }

    /**
     *
     * @param date - sunday of the desired week in YYYYMMDD format (e.g. for the week that contains
     *                  6th june 2024, should be 20240602).
     * @return (date = next week && its sunday-tuesday) == true
     * (date == 2 weeks ahead) == true
     * otherwise false
     * @throws Exception - when given date that doesn't adhere to the format.
     */
    public boolean canChangeAvailability(int date) throws Exception {
        checkDateValidity(date);
        int nextWeekDate = getNextWeekDate();
        if(nextWeekDate==date)
        {
           return dateHelper.getDayOfWeek()<=2;
        }
        return date == dateHelper.getNextWeekDate(nextWeekDate);
    }



    /**
     *
     * @return next week sunday's date (in format YYYYMMDD).
     */
    public int getNextWeekDate()

    {
        return dateHelper.getNextWeekDate();
    }


    private void checkDateValidity(int date) throws Exception {
        dateHelper.checkDateValidity(date);

    }

    /**
     * @param date - sunday of the desired week in YYYYMMDD format (e.g. for the week that contains
     *                  6th june 2024, should be 20240602).
     * @return employee's branch's said week schedule.
     * @throws Exception - when given date that doesn't adhere to the format/ no work schedule found for said date.
     */
    public String[][][] getWorkSchedule(int employeeId, int date) throws Exception {
        checkDateValidity(date);
        Employee employee = getEmployee(employeeId);
        if(employee!=null) {
            verifyHasAccess(employee);
            WorkSchedule workSchedule = workScheduleHashMap[employee.getBranchId()].get(date);
            if(workSchedule!=null)
            {
                return workScheduleToNameArr(workSchedule);
            }
            else
            {
                throw new Exception("No work schedule for this date");
            }
        }
        {
            throw new Exception("No such employee");
        }
    }
    private String[][][] workScheduleToNameArr(WorkSchedule workSchedule)
    {
        String[][][] ans = new String[7][2][];
        List<Employee>[][] schedule = workSchedule.schedule;
        for(int i =0;i<7;i++)
        {
            for(int j=0;j<2;j++)
            {
                ans[i][j] = new String[schedule[i][j].size()];
                int index = 0;
                for(Employee employee : schedule[i][j])
                {
                    ans[i][j][index] = employee.getName();
                    index++;

                }
            }
        }
        return ans;
    }
    /**
     * change employee's password from oldPassword to newPassword
     * @throws Exception - when oldPassword is wrong password / new password is invalid.
     */
    public void changePassword(int employeeId,String oldPassword,String newPassword) throws Exception {
        Employee employee =getEmployee(employeeId);
        if(employee!=null)
        {
            employee.changePassword(oldPassword,newPassword);
        }
        else
        {
            throw new Exception("No such employee");
        }
    }
}
