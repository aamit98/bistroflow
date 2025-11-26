package ServiceLayer.HR;
import DomainLayer.HR.Employee;
import DomainLayer.HR.EmployeeFacade;
import GlobalClasses.EmployeeToSend;
import GlobalClasses.Logger;
import ServiceLayer.Response;

public class EmployeeService {


    private EmployeeFacade employeeFacade;

    public EmployeeService(EmployeeFacade employeeFacade) {
        this.employeeFacade = employeeFacade;
    }


    public Response getEmployee(int employeeId) {
        try {


            Employee e = employeeFacade.getEmployee(employeeId);
            Logger.info("Employee with id " + employeeId + " found");
            return new Response(null, new EmployeeToSend(e));

        } catch (Exception e) {
            Logger.error("Employee Service Error", e);
            return new Response("getEmployee Failed" + e.getMessage(), null);

        }
    }

    public Response login(int employeeId, String password) {
        try {
            employeeFacade.login(employeeId, password);
            Logger.info("Login successful for user: " + employeeId);
            return new Response(null, "Login successful");
        } catch (Exception e) {
            Logger.error("Login failed for user: " + employeeId, e);
            return new Response("Login failed: " + e.getMessage(), null);
        }
    }

    public Response canChangeAvailability(int date)
    {
        try
        {
            return new Response(null,employeeFacade.canChangeAvailability(date));
        }
        catch (Exception e)
        {
            return new Response(e.getMessage(),null);
        }
    }

    public Response logout(int employeeId) {
        try {
            employeeFacade.logout(employeeId);
            Logger.info("Logout successful for user: " + employeeId);
            return new Response(null, "Logout successful");
        } catch (Exception e) {
            Logger.error("Logout unsuccessful for user: " + employeeId, e);
            return new Response("Logout failed: " + e.getMessage(), null);
        }
    }

    public Response getWorkSchedule(int employeeId, int date) {
        try {
            String[][][] rvalue = employeeFacade.getWorkSchedule(employeeId, date);
            return new Response(null, rvalue);
        } catch (Exception e) {
            Logger.error("Failed to get schedule for employee " + employeeId, e);
            return new Response("Failed to get schedule: " + e.getMessage(), null);
        }
    }

    public Response setAvailability(int employeeId, int date, boolean[][] availabilityArray) {
        try {
            employeeFacade.addOrChangeAvailability(employeeId, date, availabilityArray);
            Logger.info("Availability set for employee " + employeeId);
            return new Response(null, "Availability set");
        } catch (Exception e) {
            Logger.error("Failed to set availability for employee " + employeeId, e);
            return new Response("Failed to set availability: " + e.getMessage(), null);


        }


    }

    public Response changePassword(int employeeId,String oldPassword,String newPassword){
            try{
                employeeFacade.changePassword(employeeId,oldPassword,newPassword);
                Logger.info("Change password successful for employee " + employeeId);
                return new Response(null, "Password changed successfully");
            }catch (Exception e){
                Logger.error("Failed to change password for employee " + employeeId, e);
                return new Response("Failed to change password: " + e.getMessage(), null);
            }
    }
}