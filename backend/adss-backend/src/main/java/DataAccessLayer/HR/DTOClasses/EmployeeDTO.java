package DataAccessLayer.HR.DTOClasses;

import DataAccessLayer.DTO;
import DataAccessLayer.HR.ControllerClasses.EmployeeController;

public class EmployeeDTO extends DTO {

    private final int id;
    private boolean hasAccess;
    private String password;
    private boolean isHRManager;
    private boolean isDriver;
    private int branchId;
    String termsOfEmployment;
    private String name;
    private int startDate;
    private int bankCode;
    private int bankBranchCode;
    private int bankAccount;
    private int hourlyRate;
    private int monthlyRate;

    public EmployeeDTO(int id, boolean hasAccess, String password, boolean isHRManager, boolean isDriver, int branchId, String termsOfEmployment,
                       String name, int startDate, int bankCode,
                       int bankBranchCode, int bankAccount, int hourlyRate, int monthlyRate
                        , EmployeeController controller, boolean fromDB) {
        super(controller, fromDB);
        this.id = id;
        this.hasAccess = hasAccess;
        this.password = password;
        this.isHRManager = isHRManager;
        this.isDriver = isDriver;
        this.termsOfEmployment = termsOfEmployment;
        this.branchId = branchId;
        this.name = name;
        this.startDate = startDate;
        this.bankCode = bankCode;
        this.bankBranchCode = bankBranchCode;
        this.bankAccount = bankAccount;
        this.hourlyRate = hourlyRate;
        this.monthlyRate = monthlyRate;
    }

    @Override
    public void persist() throws Exception {
            insert(new Object[]{id, hasAccess, password,isHRManager,isDriver,branchId,termsOfEmployment,
            name,startDate,bankCode,bankBranchCode,bankAccount,hourlyRate,monthlyRate});
            isPersisted = true;
    }


    public int getId() {
        return id;
    }

    public boolean isHasAccess() {
        return hasAccess;
    }

    public String getPassword() {
        return password;
    }

    public boolean isHRManager() {
        return isHRManager;
    }

    public boolean isDriver() {
        return isDriver;
    }

    public int getBranchId() {
        return branchId;
    }

    public String getTermsOfEmployment() {
        return termsOfEmployment;
    }

    public String getName() {
        return name;
    }

    public int getStartDate() {
        return startDate;
    }

    public int getBankCode() {
        return bankCode;
    }

    public int getBankBranchCode() {
        return bankBranchCode;
    }

    public int getBankAccount() {
        return bankAccount;
    }

    public int getHourlyRate() {
        return hourlyRate;
    }

    public int getMonthlyRate() {
        return monthlyRate;
    }

    public void updateName(String newName) throws Exception
    {
        name= newName;
        update("name", newName);
    }
    public void updateBranchId(int newBranchId) throws Exception
    {
        branchId = newBranchId;
        update("branchId",newBranchId);
    }
    public void updateStartDate(int newStartDate) throws Exception
    {
        startDate = newStartDate;
        update("startDate",newStartDate);
    }
    public void updateTermsOfEmployment(String newTermsOfEmployment) throws Exception
    {
        termsOfEmployment = newTermsOfEmployment;
        update("termsOfEmployment",newTermsOfEmployment);
    }
    public void updateBankAccount(int newBankAccount) throws Exception
    {
        bankAccount = newBankAccount;
        update("bankAccount",newBankAccount);
    }
    public void updateBankCode(int newBankCode) throws Exception
    {
        bankCode = newBankCode;
        update("bankCode",newBankCode);
    }
    public void updateBankBranchCode(int newBankBranchCode) throws Exception
    {
        bankBranchCode = newBankBranchCode;
        update("bankBranchCode",newBankBranchCode);
    }
    public void updateHasAccess(boolean newHasAccess) throws Exception{
        hasAccess = newHasAccess;
        update("hasAccess",newHasAccess);}
    public void updateIsHrManager(boolean newIsHRManager) throws Exception{
        isHRManager = newIsHRManager;
        update("isHRManager",isHRManager);}
    public void updateHourlyRate(int newHourlyRate) throws Exception{
        hourlyRate = newHourlyRate;
        update("hourlyRate",newHourlyRate) ;}
    public void updateMonthlyRate(int newMonthlyRate) throws Exception{
        monthlyRate = newMonthlyRate;
        update("monthlyRate",newMonthlyRate);}
    public void updatePassword(String newPassword) throws Exception{
        password = newPassword;
        update("password",newPassword);}
    private void update(String varToUpdate, Object valueToUpdate) throws Exception {
            if (isPersisted) {
                update(new Object[]{id}, varToUpdate, valueToUpdate);
            }
    }
    public void delete() throws Exception {
        if(isPersisted)
        {
            delete(new Object[]{id});
            isPersisted = false;
        }
    }
}
