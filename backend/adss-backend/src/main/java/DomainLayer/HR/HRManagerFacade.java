package DomainLayer.HR;

import DataAccessLayer.HR.ControllerClasses.*;
import DataAccessLayer.HR.DTOClasses.*;
import GlobalClasses.DateHelper;
import GlobalClasses.EmployeeToSend;
import GlobalClasses.Role;

import java.util.*;

public class HRManagerFacade {

    private final DateHelper dateHelper;
    private final int maxBranchId; //= max index of hashmap.
    private final HashMap<Integer, Employee> employeesDict; // <EmployeeId,Employee>
    //10 hashMaps,1 per branch.
    private final HashMap<Integer, WorkSchedule>[] workScheduleHashMap; //<Date,WorkSchedule>
    //10 hashMaps,1 per branch.
    private final HashMap<Integer, HashMap<Employee, EmployeeAvailability>>[] employeesAvailabilityDict; //<Date,<Employee,EmployeeAvailability>>
    private final HashMap<Integer, WeeklyRoleConstraints>[] weeklyRoleConstraintsDict; //<Date,WeeklyRoleConstraints>

    private final RoleController roleController;
    private final WorkScheduleController workScheduleController;
    private final WeeklyRoleConstraintsController weeklyRoleConstraintsController;
    private final EmployeeController employeeController;


    public HRManagerFacade(HashMap<Integer, Employee> employeesDict, HashMap<Integer, WorkSchedule>[] workScheduleHashMap
            , HashMap<Integer, HashMap<Employee, EmployeeAvailability>>[] employeesAvailabilityDict,
                           HashMap<Integer, WeeklyRoleConstraints>[] weeklyRoleConstraintsDict, int maxBranchId) throws ClassNotFoundException {
        this.employeesDict = employeesDict;
        this.workScheduleHashMap = workScheduleHashMap;
        this.employeesAvailabilityDict = employeesAvailabilityDict;
        this.maxBranchId = maxBranchId;
        this.weeklyRoleConstraintsDict = weeklyRoleConstraintsDict;
        this.roleController=new RoleController();
        this.workScheduleController= new WorkScheduleController();
        this.weeklyRoleConstraintsController= new WeeklyRoleConstraintsController();
        employeeController = new EmployeeController();
        dateHelper = new DateHelper();
        //initialData();
    }

    /**
     *for tests
     */
    public HRManagerFacade(HashMap<Integer, Employee> employeesDict, HashMap<Integer, WorkSchedule>[] workScheduleHashMap
            , HashMap<Integer, HashMap<Employee, EmployeeAvailability>>[] employeesAvailabilityDict,
                           HashMap<Integer, WeeklyRoleConstraints>[] weeklyRoleConstraintsDict, int maxBranchId,
                           RoleController roleController, WorkScheduleController workScheduleController,
                           WeeklyRoleConstraintsController weeklyRoleConstraintsController,
                           EmployeeController employeeController
                           ) throws ClassNotFoundException {
        this.employeesDict = employeesDict;
        this.workScheduleHashMap = workScheduleHashMap;
        this.employeesAvailabilityDict = employeesAvailabilityDict;
        this.maxBranchId = maxBranchId;
        this.weeklyRoleConstraintsDict = weeklyRoleConstraintsDict;
        this.roleController = roleController;
        this.workScheduleController=  workScheduleController;
        this.weeklyRoleConstraintsController= weeklyRoleConstraintsController;
        this.employeeController = employeeController;
        dateHelper = new DateHelper();
    }


    public void addDriver(int managerId, EmployeeToSend employeeToSend, String password) throws Exception {
        authManagerId(managerId);
        Employee employee = employeeToSendToEmployee(employeeToSend,password);
        if (employee.getBranchId()!= maxBranchId+1) {
            throw new Exception("Invalid branch id");
        }
        if (!employeesDict.containsKey(employee.getId())) {
            employee.setDriver(true);
            employeesDict.put(employee.getId(), employee);
            employee.persistDTO();

        } else {
            throw new Exception("Employee with this id already exist");
        }
    }

    private Employee employeeToSendToEmployee(EmployeeToSend employee,String password) throws Exception {
        return new Employee(password,employee.id,employee.branchId,employee.name,employee.roles,employee.termsOfEmployment,
                employee.startDate,employee.bankCode,employee.bankBranchCode,employee.bankAccount,
                employee.hourlyRate,employee.monthlyRate,employeeController,roleController);
    }

    public void addEmployee(int managerId, EmployeeToSend employeeToSend, String password) throws Exception {
        authManagerId(managerId);
        Employee employee = employeeToSendToEmployee(employeeToSend,password);
        if (employee.getBranchId() > maxBranchId || employee.getBranchId() < 0) {
            throw new Exception("Invalid branch id");
        }
        if (!employeesDict.containsKey(employee.getId())) {
            employeesDict.put(employee.getId(), employee);
            employee.persistDTO();
        } else {
            throw new Exception("Employee with this id already exist");
        }
    }

    /**
     * @return employee if exists, null otherwise.
     */
    private Employee getEmployee(int employeeId) {
        return employeesDict.get(employeeId);
    }


    /**
     * @return employee if exists, null otherwise.
     * @throws Exception if given wrong manager id.
     */

    public Employee getEmployee(int managerId, int employeeId) throws Exception {
        authManagerId(managerId);
        return getEmployee(employeeId);
    }

    public List<Integer> getAllEmployeesIds(int managerId) throws Exception {
        authManagerId(managerId);
        return employeesDict.values().stream().map(Employee::getId).toList();
    }

    /**
     * assigns role to employee.
     *
     * @param role - role to be assigned.
     * @throws Exception if given wrong manager id/ employee already have role / no employee with this id.
     */
    public void assignRole(int managerId, int employeeId, Role role) throws Exception {
        authManagerId(managerId);
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            employee.addRole(role);
            roleController.insert(new Object[]{employeeId,role.ordinal()});

        } else {
            throw new Exception("No such employee");
        }
    }

    /**
     * remove role from employee roles.
     *
     * @param role role to be removed.
     * @throws Exception if given wrong manager id/ employee doesn't have role / no employee with this id.
     */
    public void removeRole(int managerId, int employeeId, Role role) throws Exception {
        authManagerId(managerId);
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            employee.removeRole(role);
            roleController.delete(new Object[]{employeeId, role.ordinal()});
        } else {
            throw new Exception("No such employee");
        }
    }

    /**
     * remove access to the system for an employee.
     *
     * @throws Exception if given wrong manager id/ employee already doesnt have access/ no employee with this id.
     */
    public void removeAccess(int managerId, int employeeId) throws Exception {
        authManagerId(managerId);
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            if (!employee.getHasAccess()) {
                throw new Exception("This employee access has already been revoked.");
            } else {
                employee.setHasAccess(false);
            }
        } else {
            throw new Exception("No such employee");
        }
    }

    public void reinstateAccess(int managerId, int employeeId) throws Exception {
        authManagerId(managerId);
        Employee employee = getEmployee(employeeId);
        if (employee != null) {
            if (employee.getHasAccess()) {
                throw new Exception("This employee already has access.");
            } else {
                employee.setHasAccess(true);
            }
        } else {
            throw new Exception("No such employee");
        }
    }

    /**
     * posts work schedule for said date.
     *
     * @throws Exception when given wrong manager id/ invalid branch id/ invalid date/ work schedule that
     *                   contains employees from different branches / work schedule that contains employees that are not available
     *                   for shift they are in/ work schedule that does not meet given date weekly role constraints.
     */
    public void postWorkSchedule(int managerId, int branchId, int date, int[][][] employeesIdsArray) throws Exception {
        checkDateValidity(date);
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        WorkSchedule workSchedule = arrayToWorkSchedule(employeesIdsArray,branchId,date,false);
        validateIdsUnique(employeesIdsArray);
        HashMap<Employee, EmployeeAvailability> availabilityHashMap = employeesAvailabilityDict[branchId].get(date);


        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {

                boolean hasDriver=false;
                boolean hasStoreKeeper=false;

                for (Employee e : workSchedule.schedule[i][j]) {
                    if (e.getBranchId() != branchId) {
                        throw new Exception("employee " + e.getId() + "does not belong to this branch");
                    }
                    EmployeeAvailability employeeAvailability;
                    if(availabilityHashMap!=null)
                    {
                        employeeAvailability = availabilityHashMap.get(e);
                    }
                    else
                    {
                        employeeAvailability = null;
                    }
                    if (employeeAvailability==null||!availabilityHashMap.get(e).getAvailabilityArray()[i][j]) {
                        throw new Exception("employee " + e.getId() + " is not available for " + toStringShift(i, j));
                    }
                    if(e.isDriver()){
                        hasDriver=true;
                    }
                    if(e.hasRole(Role.STOREKEEPER)){
                        hasStoreKeeper=true;
                    }
                }
                if(hasDriver&& !hasStoreKeeper){
                    throw new Exception("Driver assigned without a storekeeper in "+toStringShift(i,j));
                }
            }
        }
        WeeklyRoleConstraints constraints = weeklyRoleConstraintsDict[branchId].get(date);
        if (constraints == null) {
            constraints = WeeklyRoleConstraints.getDefaultConstraints();
        }
        String temp = tryScheduleWithConstraints(constraints, workSchedule);
        if (temp.length() != 0) {
            throw new Exception("schedule did not meet constraints: " + temp);
        }
        (workScheduleHashMap[branchId]).put(date, workSchedule);
        workSchedule.persistDTO();
    }

    private void validateIdsUnique(int[][][] employeesIdsArray) throws Exception {
        for(int i =0; i<7 ;i++) {
            for (int j = 0; j < 2; j++) {
                if (Arrays.stream(employeesIdsArray[i][j]).distinct().toArray().length != employeesIdsArray[i][j].length)
                {
                    throw new Exception("an employee was assigned more then once to the same shift");
                }

            }
        }
    }

    private WorkSchedule arrayToWorkSchedule(int[][][] arr,int branchId,
                                             int date,
                                             boolean fromDB) throws Exception {
        if (arr == null || arr.length != 7) {
            throw new Exception("invalid array");
        }
        List<Employee>[][] employeesArr = new LinkedList[7][2];
        for (int i = 0; i < 7; i++) {
            if (arr[i] == null || arr[i].length != 2) {
                throw new Exception("invalid array");
            }
            for (int j = 0; j < 2; j++) {
                if (arr[i][j] == null) {
                    throw new Exception("invalid array");
                }
                employeesArr[i][j] = new LinkedList<>();
                for (int id : arr[i][j]) {
                    Employee employee = employeesDict.get(id);
                    if (employee == null) {
                        throw new Exception("work schedule includes invalid id");
                    }
                    employeesArr[i][j].add(employee);
                }

            }
        }
        return new WorkSchedule(employeesArr,branchId,date,workScheduleController,fromDB);
    }

    private void checkDateValidity(int date) throws Exception {
        dateHelper.checkDateValidity(date);
    }


    private void checkBranchIdValidity(int branchId) throws Exception {
        if (branchId > maxBranchId || branchId < 0) {
            throw new Exception("Invalid branch id");
        }
    }

    /**
     *
     * @return all employees availability for a given branch and week first dimension = day,
     * second dimension = morning/evening shift .
     * @throws Exception when given wrong manager id/ invalid date/ invalid branch id.
     */
    public List<String>[][] getEmployeesAvailability(int managerId, int date, int branchId) throws Exception {
        checkDateValidity(date);
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        LinkedList<String>[][] ans = new LinkedList[7][2];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                ans[i][j] = new LinkedList<>();
            }
        }
        HashMap<Employee, EmployeeAvailability> temp = employeesAvailabilityDict[branchId].get(date);
        if (temp != null) {
            System.out.println("Availability found for date: " + date);
            for (Map.Entry<Employee, EmployeeAvailability> entry : temp.entrySet()) {
                Employee employee = entry.getKey();
                String employeeDesc = employee.getName() + " " + employee.getId() + " " + Arrays.toString(employee.getRoles());
                boolean[][] availabilityArray = entry.getValue().getAvailabilityArray();
                for (int i = 0; i < 7; i++) {
                    for (int j = 0; j < 2; j++) {
                        if (availabilityArray[i][j]) {
                            ans[i][j].add(employeeDesc);
                        }
                    }
                }
            }
        } else {
            System.out.println("No availability found for date: " + date);
        }
        return ans;
    }


    private void authManagerId(int managerId) throws Exception {
        Employee e = employeesDict.get(managerId);
        if (e != null) {
            if (e.getIsHrManager()) {
                return;
            }
        }
        throw new Exception("invalid manager id");
    }

    /**
     * sets weekly role constraints for a given branch and date.
     *
     * @throws Exception when given wrong manager id/ invalid branch id/ invalid date.
     */
    public void setWeeklyRoleConstraints(int managerId, int branchId, int date, Role[][][]
            roles) throws Exception {
        checkDateValidity(date);
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        WeeklyRoleConstraints weeklyRoleConstraints = new WeeklyRoleConstraints(roles,branchId,date,weeklyRoleConstraintsController,
                false);
        WeeklyRoleConstraints oldConstraints = weeklyRoleConstraintsDict[branchId].get(date);
        if(oldConstraints!=null)
        {
            oldConstraints.deleteDTO();
        }
        weeklyRoleConstraintsDict[branchId].put(date, weeklyRoleConstraints);
        weeklyRoleConstraints.persistDTO();


    }


    /**
     * restores weekly role constraints to default for a given branch and date.
     *
     * @throws Exception when given wrong manager id/ invalid branch id/ invalid date/ was already default.
     */
    public void restoreWeeklyRoleConstraintsToDefault(int managerId, int branchId, int date) throws Exception {
        checkDateValidity(date);
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        WeeklyRoleConstraints weeklyRoleConstraints = weeklyRoleConstraintsDict[branchId].remove(date);
        if (weeklyRoleConstraints == null) {
            throw new Exception("weekly role constraints for this date was already default.");
        }
        else
        {
            weeklyRoleConstraints.deleteDTO();
        }
    }

    private String tryScheduleWithConstraints(WeeklyRoleConstraints weeklyRoleConstraints, WorkSchedule workSchedule) throws Exception {
        String ans = "";
        int rolesAmount = Role.values().length;
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 2; j++) {
                int[] neededRoles = roleArrayToIntArray(weeklyRoleConstraints.getConstraints(i, j), rolesAmount);
                boolean[][] availableRoles = employeesRolesToSingleArray(workSchedule.schedule[i][j], rolesAmount);
                if (!bruteForceOuter(availableRoles, rolesAmount, neededRoles)) {
                    ans += toStringShift(i, j) + " ";
                }
            }
        }
        return ans;
    }

    private boolean bruteForceOuter(boolean[][] availableRoles, int rolesAmount, int[] neededRoles) {

        if (availableRoles.length == 0) {
            for (int neededRole : neededRoles) {
                if (neededRole > 0) {
                    return false;
                }
            }
            return true;
        }
        int[] arr = new int[rolesAmount];
        for (int i = 0; i < rolesAmount; i++) {
            if (availableRoles[0][i]) {
                arr[i] = 1;
                if (bruteForceInner(availableRoles, 1, arr, neededRoles)) {
                    return true;
                } else {
                    arr[i]--;
                }
            }
        }
        return false;
    }

    private boolean bruteForceInner(boolean[][] availableRoles, int index, int[] acc, int[] neededRoles) {
        if (index >= availableRoles.length) {
            for (int j = 0; j < neededRoles.length; j++) {
                if (neededRoles[j] > acc[j]) {
                    return false;
                }
            }
            return true;
        }
        for (int i = 0; i < availableRoles[index].length; i++) {
            if (availableRoles[index][i]) {
                acc[i]++;
                if (bruteForceInner(availableRoles, index + 1, acc, neededRoles)) {
                    return true;
                } else {
                    acc[i]--;
                }
            }
        }
        return false;
    }


    private boolean[][] employeesRolesToSingleArray(List<Employee> employeeList, int rolesAmount) {
        boolean[][] ans = new boolean[employeeList.size()][rolesAmount];
        int index = 0;
        for (Employee e : employeeList) {
            for (Role r : e.getRoles()) {
                ans[index][r.ordinal()] = true;
            }
            index++;
        }
        return ans;
    }


    private int[] roleArrayToIntArray(Role[] roles, int rolesAmount) {
        int[] ans = new int[rolesAmount];
        for (Role r : roles) {
            ans[r.ordinal()]++;
        }
        return ans;


    }

    private String toStringShift(int day, int shift) {
        String ans = "";
        switch (day) {
            case 0:
                ans += "sunday ";
                break;
            case 1:
                ans += "monday ";
                break;
            case 2:
                ans += "tuesday ";
                break;
            case 3:
                ans += "wednesday  ";
                break;
            case 4:
                ans += "thursday ";
                break;
            case 5:
                ans += "friday ";
                break;
            case 6:
                ans += "saturday ";
                break;
        }
        switch (shift) {
            case 0:
                ans += "morning";
                break;
            case 1:
                ans += "evening";
                break;
        }
        return ans;
    }

    /**
     * @return weekly role constraints for a given branch and date.
     * @throws Exception when give wrong manager id/ invalid branch id/ invalid date.
     */
    public WeeklyRoleConstraints getWeeklyRoleConstraints(int managerId, int branchId, int date) throws Exception {
        checkDateValidity(date);
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        WeeklyRoleConstraints constraints = weeklyRoleConstraintsDict[branchId].get(date);
        if (constraints == null) {
            constraints = WeeklyRoleConstraints.getDefaultConstraints();

        }
        return constraints;


    }

    /**
     * @return all employees in a given branch.
     * @throws Exception when given wrong manager id.
     */
    public List<Employee> getAllEmployeesInBranch(int managerId, int branchId) throws Exception {
        checkBranchIdValidity(branchId);
        authManagerId(managerId);
        List<Employee> employeesInBranch = new ArrayList<>();
        for (Employee employee : employeesDict.values()) {
            if (employee.getBranchId() == branchId) {
                employeesInBranch.add(employee);
            }
        }
        return employeesInBranch;
    }

    /**
     * updates already existing employee info (name,branchId,startDate,endDate,termsOfEmployment,roles,
     * bankAccount,bankCode,bankBranchCode)
     *
     * @throws Exception when given wrong manager id/ there is no employee with this employee id in the system.
     */
    public void updateEmployee(int managerId, EmployeeToSend employeeToSend) throws Exception {
        authManagerId(managerId);
        Employee employee = employeeToSendToEmployee(employeeToSend,"abcd");
        checkBranchIdValidity(employee.getBranchId());
        Employee e = getEmployee(employee.getId());
        if (e != null) {
            if(e.getBranchId()!=employee.getBranchId())
            {
                removeFutureAvailabilities(e);
            }
            e.update(employee);
        } else {
            throw new Exception("No such employee");
        }

    }
    private void removeFutureAvailabilities(Employee employee) throws Exception {
        int nextWeekDate = dateHelper.getNextWeekDate();
        HashMap<Employee,EmployeeAvailability> temp =  employeesAvailabilityDict[employee.getBranchId()].get(nextWeekDate);
        if(temp!=null)
        {
            temp.remove(employee);
        }
        int nextNextWeekDate = dateHelper.getNextWeekDate(nextWeekDate);
        temp =  employeesAvailabilityDict[employee.getBranchId()].get(nextNextWeekDate);
        if(temp!=null)
        {
            EmployeeAvailability e =  temp.remove(employee);
            if(e!=null)
            {
                e.deleteDTO();
            }

        }
    }

    public void loadDataFromDB() throws Exception{
        try{
            List<EmployeeDTO> dtos = employeeController.getAll();
            for(EmployeeDTO employeeDTO : dtos ){
                List<RoleDTO> employeeRoles = roleController.getAllEmployeeRoles(employeeDTO.getId());
                Employee employee=new Employee(employeeDTO.getPassword(),employeeDTO.getId(), employeeDTO.getBranchId(),
                        employeeDTO.getName(), employeeRoles, employeeDTO.getTermsOfEmployment(),
                        employeeDTO.getStartDate(), employeeDTO.getBankCode(), employeeDTO.getBankBranchCode(),
                        employeeDTO.getBankAccount(),employeeDTO.getHourlyRate(),employeeDTO.getMonthlyRate(),
                        employeeDTO.isHasAccess(),employeeDTO.isHRManager(),employeeDTO.isDriver(),
                        employeeController,roleController);
                employeesDict.put(employee.getId(),employee);

            }
            List<WorkScheduleDTO> workSchedules=workScheduleController.getAll();
            for(WorkScheduleDTO workScheduleDTO : workSchedules) {
                List<Employee>[][] employeesArr = new LinkedList[7][2];
                for (int i = 0; i < 7; i++) {
                    for (int j = 0; j < 2; j++) {
                        employeesArr[i][j] = new LinkedList<>();
                        for (int employeeId : workScheduleDTO.getEmployeesIds()[i][j]) {
                            Employee employee = employeesDict.get(employeeId);
                            if (employee != null) {
                                employeesArr[i][j].add(employee);
                            }
                        }
                    }
                }
                WorkSchedule workSchedule = new WorkSchedule(employeesArr, workScheduleDTO.getBranchId(), workScheduleDTO.getDate(), workScheduleController, true
                );
                workScheduleHashMap[workScheduleDTO.getBranchId()].put(workScheduleDTO.getDate(), workSchedule);
            }
                EmployeeAvailabilityController availabilityController = new EmployeeAvailabilityController();
                List<EmployeeAvailabilityDTO> availabilityDTOs = availabilityController.getAll();
                for(EmployeeAvailabilityDTO availabilityDTO : availabilityDTOs) {
                    Employee employee = employeesDict.get(availabilityDTO.getEmployeeId());
                    if (employee != null) {
                        EmployeeAvailability availability = new EmployeeAvailability(
                                availabilityDTO.getAvailabilityArray(),
                                availabilityDTO.getEmployeeId(),
                                availabilityDTO.getDate(),
                                availabilityController,
                                true

                        );


                        int branchId = employee.getBranchId();
                        int date = availabilityDTO.getDate();

                        if (!employeesAvailabilityDict[branchId].containsKey(date)) {
                            employeesAvailabilityDict[branchId].put(date, new HashMap<>());
                        }

                        employeesAvailabilityDict[branchId].get(date).put(employee, availability);


                    }


                }
            List<WeeklyRoleConstraintsDTO> weeklyRoleConstraintsDTOs = weeklyRoleConstraintsController.getAll();
            for (WeeklyRoleConstraintsDTO roleConstraintsDTO : weeklyRoleConstraintsDTOs) {
                Role[][][] constraintsArray = new Role[7][2][];
                for (int i = 0; i < 7; i++) {
                    for (int j = 0; j < 2; j++) {
                        constraintsArray[i][j] = new Role[roleConstraintsDTO.getRoleIdsArray()[i][j].length];
                        for (int k = 0; k < roleConstraintsDTO.getRoleIdsArray()[i][j].length; k++) {
                            constraintsArray[i][j][k] = Role.values()[roleConstraintsDTO.getRoleIdsArray()[i][j][k]];
                        }
                    }
                }
                int branchId = roleConstraintsDTO.getBranchId();
                int date = roleConstraintsDTO.getDate();
                WeeklyRoleConstraints weeklyRoleConstraints = new WeeklyRoleConstraints(constraintsArray, branchId, date, weeklyRoleConstraintsController, true);
                if (!weeklyRoleConstraintsDict[branchId].containsKey(date)) {
                    weeklyRoleConstraintsDict[branchId].put(date, weeklyRoleConstraints);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("Failed to load data from db"+e.getMessage());
        }
    }

    /**
     * called once to init the database to contain something
     */
    public void initialData()
    {
        try {
            EmployeeController tempController = new EmployeeController();
            tempController.deleteAllHr();
            // HR Manager
            Employee hrManager = new Employee("hrManager", 1, 0, "HR Manager",  new Role[]{Role.STOREKEEPER,Role.CASHIER,Role.MANAGER},
                        "Full-time",20240608, 123, 456, 789, 50, 5000,employeeController,roleController);
                hrManager.persistDTO();
            hrManager.setHRManager(true);
            String password = "abc";
            int employeeIdCounter = 2;
            String name = "John Doe";
            Role[][] possibleRoles = new Role[5][];
            possibleRoles[0] = new Role[0];
            possibleRoles[1] = new Role[]{Role.STOREKEEPER};
            possibleRoles[2] = new Role[]{Role.CASHIER};
            possibleRoles[3] = new Role[]{Role.STOREKEEPER,Role.CASHIER};
            possibleRoles[4] = new Role[]{Role.STOREKEEPER,Role.CASHIER,Role.MANAGER};
            String termsOfEmployment = "Full-time";
            int startDate = 20240605;
            int bankCode = 10;
            int bankBranchCode = 25;
            int bankAccount = 35;
            int hourlyRate = 45;
            int monthlyRate = 3000;
            Employee[][] employeeByBranch = new Employee[4][10];
            int[] branch3Roles = new int[]{2,2,2,1,1,3,3,4,4,4};


            for(int i =0;i<4;i++)
            {
                for(int j = 0;j<10;j++)
                {
                    Employee employee;
                    if(i==3)
                    {
                        employee = new Employee(password + employeeIdCounter, employeeIdCounter, i,
                                name + employeeIdCounter, possibleRoles[branch3Roles[j]], termsOfEmployment, startDate, bankCode,
                                bankBranchCode, bankAccount + employeeIdCounter, hourlyRate, monthlyRate,
                                employeeController, roleController);
                        employee.persistDTO();
                    }
                    else
                    {
                        employee = new Employee(password + employeeIdCounter, employeeIdCounter, i,
                                name + employeeIdCounter, possibleRoles[4], termsOfEmployment, startDate, bankCode,
                                bankBranchCode, bankAccount + employeeIdCounter, hourlyRate, monthlyRate,
                                employeeController, roleController);
                        employee.persistDTO();
                    }
                    if(!employeesDict.containsKey(employeeIdCounter))
                    {  //to avoid overriding new employees added before loading data
                        employeesDict.put(employeeIdCounter, employee);
                        employeeByBranch[i][j] = employee;
                    }
                    employeeIdCounter++;
                }
            }
            Random random = new Random();
            DateHelper dateHelper = new DateHelper();
            int date = dateHelper.getNextWeekDate();
            if(!employeesAvailabilityDict[0].containsKey(date))
            {
                employeesAvailabilityDict[0].put(date,new HashMap<>());
            }
            if(!employeesAvailabilityDict[1].containsKey(date))
            {
                employeesAvailabilityDict[1].put(date,new HashMap<>());
            }
            if(!employeesAvailabilityDict[3].containsKey(date))
            {
                employeesAvailabilityDict[3].put(date,new HashMap<>());
            }
            boolean[][] tempArr;
            for(int i= 0;i<10;i++)
            {
                tempArr = new boolean[7][2];
                for(int j =0;j<7;j++)
                {
                    for(int k=0;k<2;k++)
                    {
                        tempArr[j][k] = random.nextBoolean();
                    }
                }
                if(employeeByBranch[0][i]!=null)
                {
                    EmployeeAvailability employeeAvailability = new EmployeeAvailability(tempArr,employeeByBranch[0][i].getId(),
                            date,new EmployeeAvailabilityController(),false);
                    employeeAvailability.persistDTO();
                    //employeesAvailabilityDict[0].get(date).put(employeeByBranch[0][i],new EmployeeAvailability(tempArr));
                }

            }
            tempArr = new boolean[][] {{true,true},{true,true},{true,true},{true,true},{true,true},{true,true},{true,true}};
            for(int i =0;i<10;i++)
            {
                EmployeeAvailability  employeeAvailability;
                if(employeeByBranch[1][i]!=null)
                {
                    employeeAvailability = new EmployeeAvailability(tempArr,employeeByBranch[1][i].getId(),
                            date,new EmployeeAvailabilityController(),false);
                    employeeAvailability.persistDTO();
                    //employeesAvailabilityDict[1].get(date).put(employeeByBranch[1][i],new EmployeeAvailability(tempArr));
                }
                if(employeeByBranch[3][i]!=null)
                {
                    employeeAvailability = new EmployeeAvailability(tempArr,employeeByBranch[3][i].getId(),
                            date,new EmployeeAvailabilityController(),false);
                    employeeAvailability.persistDTO();
                    //employeesAvailabilityDict[3].get(date).put(employeeByBranch[3][i],new EmployeeAvailability(tempArr));
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}


