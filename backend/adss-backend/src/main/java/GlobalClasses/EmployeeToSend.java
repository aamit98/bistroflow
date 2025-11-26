package GlobalClasses;
import DomainLayer.HR.Employee;

public class EmployeeToSend {


    public final boolean isHRManager;
    public final int id;
    public final int branchId;
    public final String termsOfEmployment;
    public final String name;
    public final int startDate;
    public final int bankCode;
    public final int bankBranchCode;
    public final int bankAccount;
    public final int hourlyRate;
    public final int monthlyRate;
    public final Role[] roles;



    public EmployeeToSend(Employee employee)
    {
        isHRManager = employee.getIsHrManager();
        id = employee.getId();
        branchId = employee.getBranchId();
        termsOfEmployment = employee.getTermsOfEmployment();
        name = employee.getName();
        startDate = employee.getStartDate();
        bankCode = employee.getBankCode();
        bankBranchCode = employee.getBankBranchCode();
        bankAccount = employee.getBankAccount();
        hourlyRate = employee.getHourlyRate();
        monthlyRate = employee.getMonthlyRate();
        roles = employee.getRoles();
    }


    

    public EmployeeToSend(int id,int branchId, String termsOfEmployment,String name, int startDate,
                          int bankCode,int bankBranchCode,int bankAccount,int hourlyRate,int monthlyRate,Role[] roles)
    {
        isHRManager = false;
        this.id = id;
        this.branchId =branchId;
        this.termsOfEmployment =termsOfEmployment;
        this.name =name;
        this.startDate = startDate;

        this.bankCode =bankCode;
        this.bankBranchCode = bankBranchCode;
        this.bankAccount = bankAccount;
        this.hourlyRate = hourlyRate;
        this.monthlyRate = monthlyRate;
        this.roles = roles;
    }




}


