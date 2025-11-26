package ServiceLayer.HR;

import DomainLayer.HR.Employee;
import DomainLayer.HR.HRManagerFacade;
import DomainLayer.HR.WeeklyRoleConstraints;
import GlobalClasses.EmployeeToSend;
import GlobalClasses.Logger;
import GlobalClasses.Role;
import ServiceLayer.Response;


public class HRManagerService {


    private final HRManagerFacade hrManagerFacade;

    public HRManagerService(HRManagerFacade hrManagerFacade) {
        this.hrManagerFacade = hrManagerFacade;
    }

    public Response addEmployee(int managerId, EmployeeToSend employee, String password) {
        try {
            hrManagerFacade.addEmployee(managerId, employee,password);
            Logger.info("Employee added: " + employee.name);
            return new Response(null, "Added successfully!");
        } catch (Exception e) {
            Logger.error("Failed to add employee: " + employee.name, e);
            return new Response("Failed to add employee: " + e.getMessage(), null);
        }
    }


    public Response addDriver(int managerId,EmployeeToSend employee,String password){
        try{
            hrManagerFacade.addDriver(managerId,employee,password);
            Logger.info("Driver Employee added: " + employee.name);
            return new Response(null, "Drive Added successfully!");
        }catch(Exception e){
            Logger.error("Failed to add driver employee: " + employee.name, e);
            return new Response("Failed to add driver employee: " + e.getMessage(), null);
        }
    }

    public Response assignRole(int managerId, int employeeId, Role role) {
        try {
            hrManagerFacade.assignRole(managerId, employeeId, role);
            Logger.info("Role " + role + " assigned to employee with ID: " + employeeId);
            return new Response(null, "Role assigned successfully");
        } catch (Exception e) {
            Logger.error("Failed to assign role to employee with ID: " + employeeId, e);
            return new Response("Failed to assign role: " + e.getMessage(), null);
        }
    }

    public Response removeRole(int managerId, int employeeId, Role role) {
        try {
            hrManagerFacade.removeRole(managerId, employeeId, role);
            Logger.info("Role " + role + " removed from employee with ID: " + employeeId);
            return new Response(null, "Role removed successfully");
        } catch (Exception e) {
            Logger.error("Failed to remove role from employee with ID: " + employeeId, e);
            return new Response("Failed to remove role: " + e.getMessage(), null);
        }
    }

    public Response removeAccess(int managerId, int employeeId) {
        try {
            hrManagerFacade.removeAccess(managerId, employeeId);
            Logger.info("Removed access to employee with ID: " + employeeId);
            return new Response(null, "Access removed successfully");
        } catch (Exception e) {
            Logger.error("Failed to remove access to employee with ID: " + employeeId, e);
            return new Response("Failed to remove access: " + e.getMessage(), null);
        }
    }

    public Response reinstateAccess(int managerId, int employeeId) {
        try {
            hrManagerFacade.reinstateAccess(managerId, employeeId);
            Logger.info("Reinstated access to employee with ID: " + employeeId);
            return new Response(null, "Access reinstated successfully");
        } catch (Exception e) {
            Logger.error("Failed to reinstate access to employee with ID: " + employeeId, e);
            return new Response("Failed to reinstate access: " + e.getMessage(), null);
        }
    }

    public Response postWorkSchedule(int managerId, int branchId, int date, int[][][] employeesIdsArray) {
        try {
            hrManagerFacade.postWorkSchedule(managerId, branchId, date, employeesIdsArray);

                Logger.info("Posted work schedule for branch " + branchId + ",date " + date);
                return new Response(null, null);

        }
        catch (Exception e) {
            Logger.error("Failed to post work schedule for: " + date, e);
            return new Response("Failed to post work schedule: " + e.getMessage(), null);
        }
    }

    public Response getEmployee(int managerId, int employeeId) {
        try {
            Employee employee = hrManagerFacade.getEmployee(managerId, employeeId);
            return new Response(null, new EmployeeToSend(employee));
        } catch (Exception e) {
            Logger.error("Failed to get employee with ID: " + employeeId, e);
            return new Response("Failed to get employee: " + e.getMessage(), null);
        }
    }
    public Response getEmployeesAvailability(int managerId, int date, int branchId)
    {
        try
        {
            Response r = new Response(null,hrManagerFacade.getEmployeesAvailability(managerId,date,branchId));
            Logger.info("employeeAvailability for date " + date + " branch id " + "was given");
            return r;
        }
        catch (Exception e)
        {
            Logger.error("failed to give employeeAvailability for date " + date + " branch id ", e);
            return new Response(e.getMessage(),null);
        }
    }
    public Response setWeeklyRoleConstraints(int managerId,int branchId, int date , Role[][][] weeklyRoleConstraints)
    {
        try
        {
            hrManagerFacade.setWeeklyRoleConstraints(managerId,branchId,date,weeklyRoleConstraints);
            return new Response(null,null);
        }
        catch (Exception e)
        {
            Logger.error("failed to set weekly role constraints ", e);
            return new Response(e.getMessage(),null);
        }
    }
    public Response restoreWeeklyRoleConstraintsToDefault(int managerId,int branchId, int date)
    {
        try
        {
            hrManagerFacade.restoreWeeklyRoleConstraintsToDefault(managerId,branchId,date);
            return new Response(null,null);
        }
        catch (Exception e)
        {
            Logger.error("failed to restore weekly role constraints to default", e);
            return new Response(e.getMessage(),null);
        }
    }

    public Response getWeeklyRoleConstraints(int managerId, int branchId, int date) {
        try {
            WeeklyRoleConstraints constraints = hrManagerFacade.getWeeklyRoleConstraints(managerId, branchId, date);
            Role[][][] ans = new Role[7][2][];
            for(int i =0;i<7;i++)
            {
                for(int j=0;j<2;j++)
                {
                    ans[i][j] = constraints.getConstraints(i,j);
                }
            }
            return new Response(null, ans);
        } catch (Exception e) {
            Logger.error("Failed to get weekly role constraints for branch " + branchId + " and date " + date, e);
            return new Response("Failed to get weekly role constraints: " + e.getMessage(), null);
        }
    }

    public Response getAllEmployeesInBranch(int managerId, int branchId) {
        try {
            EmployeeToSend[] employees = hrManagerFacade.getAllEmployeesInBranch(managerId, branchId)
                    .stream()
                    .map(EmployeeToSend::new)
                    .toArray(EmployeeToSend[]::new); // Specify the type of the array
            return new Response(null, employees);
        } catch (Exception e) {
            Logger.error("Failed to get employees in branch " + branchId, e);
            return new Response("Failed to get employees in branch: " + e.getMessage(), null);
        }
    }
    public Response updateEmployee(int managerId,EmployeeToSend employee )
    {
        try
        {
            hrManagerFacade.updateEmployee(managerId,employee);
            return new Response(null,null);
        }
        catch (Exception e)
        {
            return new Response(e.getMessage(),null);
        }
    }

    /**
     * for testing
     */
    public void initialData() {
        hrManagerFacade.initialData();
    }
    public void loadDataFromDB() throws Exception {
        hrManagerFacade.loadDataFromDB();
    }
}
