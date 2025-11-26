package DomainLayer.HR;

import DataAccessLayer.HR.ControllerClasses.EmployeeController;
import DataAccessLayer.HR.ControllerClasses.RoleController;
import DataAccessLayer.HR.DTOClasses.EmployeeDTO;
import DataAccessLayer.HR.DTOClasses.RoleDTO;
import GlobalClasses.Role;

import java.util.*;

public class Employee {

    private boolean hasAccess;
    private String password;
    private boolean isHRManager;
    private boolean isDriver;
    private final int id;
    private int branchId;
    String termsOfEmployment;
    private String name;
    private int startDate;
    private boolean loggedIn;
    private int bankCode;
    private int bankBranchCode;
    private int bankAccount;
    private int hourlyRate;
    private int monthlyRate;
    private Set<Role> roles;
    private List<RoleDTO> rolesDTO;
    private EmployeeDTO employeeDTO;
    private RoleController roleController;


    public Employee(String password, int employeeId, int branchId, String name,
                    Role[] roles, String termsOfEmployment,
                    int startDate, int bankCode,
                    int bankBranchCode, int bankAccount, int hourlyRate, int monthlyRate,
                    EmployeeController employeeController, RoleController roleController) throws Exception {
        hasAccess = true;
        loggedIn = false;
        isHRManager = false;
        this.isDriver = false;
        employeeDTO = new EmployeeDTO(employeeId,hasAccess,password,isHRManager,isDriver,branchId,termsOfEmployment,
                name,startDate,bankCode,bankBranchCode,bankAccount,hourlyRate,monthlyRate,employeeController,false);
        this.id = employeeId;
        this.name = name;
        this.branchId = branchId;
        this.startDate = startDate;
        this.termsOfEmployment = termsOfEmployment;
        setPassword(password);
        this.roleController = roleController;
        setRoles(roles,roleController,false);
        setBankDetails(bankCode, bankBranchCode, bankAccount);
        setRate(hourlyRate, monthlyRate);



    }

    /**
     * from data base constructor.
     */
    public Employee(String password, int employeeId, int branchId, String name,
                    List<RoleDTO> rolesDTO, String termsOfEmployment,
                    int startDate, int bankCode,
                    int bankBranchCode, int bankAccount, int hourlyRate, int monthlyRate,
                    boolean hasAccess,boolean isHRManager, boolean isDriver,
                    EmployeeController employeeController, RoleController roleController) throws Exception {
        employeeDTO = new EmployeeDTO(employeeId,hasAccess,password,isHRManager,isDriver,branchId,termsOfEmployment,
                name,startDate,bankCode,bankBranchCode,bankAccount,hourlyRate,monthlyRate,employeeController,true);
        this.id = employeeId;
        this.name = name;
        this.branchId = branchId;
        this.startDate = startDate;
        this.termsOfEmployment = termsOfEmployment;
        setPassword(password);
        this.roleController = roleController;
        this.rolesDTO = rolesDTO;
        roles = new HashSet<>();
        Role[] possibleRoles = Role.values();
        for(RoleDTO r : rolesDTO)
        {
            roles.add(possibleRoles[r.getRoleOrdinal()]);
        }
        setBankDetails(bankCode, bankBranchCode, bankAccount);
        setRate(hourlyRate, monthlyRate);
        this.hasAccess = hasAccess;
        loggedIn = false;
        this.isHRManager = isHRManager;
        this.isDriver = isDriver;


    }


    public boolean isDriver(){
        return isDriver;

    }

    public void setDriver(boolean isDriver){
        this.isDriver=isDriver;
    }




    public void update(Employee other) throws Exception {
        if(!name.equals(other.name))
        {
            name = other.name;
            employeeDTO.updateName(name);

        }
        if(branchId!=other.branchId)
        {
            branchId = other.branchId;
            employeeDTO.updateBranchId(branchId);

        }
        if(startDate!= other.startDate)
        {
            startDate = other.startDate;
            employeeDTO.updateStartDate(startDate);
        }
        if(!termsOfEmployment.equals(other.termsOfEmployment))
        {
            termsOfEmployment = other.termsOfEmployment;
            employeeDTO.updateTermsOfEmployment(termsOfEmployment);
        }
        if(bankAccount != other.bankAccount)
        {
            bankAccount = other.bankAccount;
            employeeDTO.updateBankAccount(bankAccount);
        }
        if(bankCode != other.bankCode)
        {
            bankCode = other.bankCode;
            employeeDTO.updateBankCode(bankCode);
        }
        if(bankBranchCode != other.bankBranchCode)
        {
            bankBranchCode = other.bankBranchCode;
            employeeDTO.updateBankBranchCode(bankBranchCode);
        }
        for(RoleDTO r : rolesDTO)
        {
            r.delete();
        }
        this.roles = other.roles;
        this.rolesDTO = other.rolesDTO;
        for(RoleDTO r : rolesDTO)
        {
            r.persist();
        }
    }
    public boolean getHasAccess()
    {
        return hasAccess;
    }
    public void setHasAccess(boolean val) throws Exception {
        hasAccess = val;
        employeeDTO.updateHasAccess(hasAccess);
    }
    public void setHRManager(boolean val) throws Exception {
        this.isHRManager = val;
        employeeDTO.updateIsHrManager(isHRManager);
    }
    public boolean getIsHrManager()
    {
        return isHRManager;
    }


    private void setRate(int hourly,int monthly) throws Exception {
        if(hourly<0||monthly<0)
        {
            throw new Exception("Invalid hourly/monthly rate");
        }
        hourlyRate = hourly;
        monthlyRate = monthly;
        employeeDTO.updateHourlyRate(hourlyRate);
        employeeDTO.updateMonthlyRate(monthlyRate);
    }
    private void setPassword(String password) throws Exception {
        if(password==null)
        {
            throw new Exception("password cannot be null");
        }

        this.password = password;
        employeeDTO.updatePassword(password);
    }
    public void changePassword(String oldPassword,String newPassword) throws Exception {
        if(authenticate(oldPassword))
        {
            setPassword(newPassword);
        }
        else
        {
            throw new Exception("wrong old password");
        }
    }
    private void setBankDetails(int bankCode,int branchCode,int bankAccount) throws Exception {
        if(bankCode<0||branchCode<0||bankAccount<0)
        {
            throw new Exception("Invalid bank details");
        }
        this.bankCode = bankCode;
        this.bankBranchCode = branchCode;
        this.bankAccount = bankAccount;
        employeeDTO.updateBankCode(bankCode);
        employeeDTO.updateBankBranchCode(branchCode);
        employeeDTO.updateBankAccount(bankAccount);
    }
    private void setRoles(Role[] roles,RoleController roleController,boolean fromDB) {
        this.roles = new HashSet<Role>();
        this.roles.addAll(Arrays.asList(roles));
        this.rolesDTO = new LinkedList<>();
        for(Role r : roles)
        {
            rolesDTO.add(new RoleDTO(id,r.ordinal(),roleController,fromDB));
        }
    }

    private boolean authenticate(String password)
    {
        return this.password.equals(password);
    }

    public boolean login(String password) throws Exception {

        if(authenticate(password))
        {
            if(loggedIn)
            {
                throw new Exception("Employee in already logged in");

            }
            loggedIn = true;
            return true;
        }
        return false;
    }

    public void logout() throws Exception {
        if(loggedIn)
        {
            loggedIn = false;
        }
        else
        {
            throw new Exception("Employee is not logged in");
        }
    }

    public int getId(){
        return id;
    }
    public String getName(){
        return name;
    }
    public String getTermsOfEmployment(){
        return termsOfEmployment;

    }


    public int getStartDate(){
        return startDate;
    }
    public int getBankBranchCode(){return bankBranchCode;}
    public int getBankCode(){return bankCode;}
    public int getBankAccount(){return bankAccount;}
    public int getHourlyRate(){return hourlyRate;}
    public int getMonthlyRate(){return monthlyRate;}

    public boolean hasRole(Role role) {
        return this.roles.contains(role);
    }
    public Role[] getRoles()
    {
        return roles.toArray(new Role[0]);
    }
    public void addRole(Role role) throws Exception {
        if (!roles.contains(role)){
            roles.add(role);
            RoleDTO roleDTO = new RoleDTO(id,role.ordinal(),roleController,false);
            roleDTO.persist();
            rolesDTO.add(roleDTO);
        }
        else
        {
            throw new Exception("This employee already have this role");
        }
    }
    public void removeRole(Role role) throws Exception {
        if (roles.contains(role)){
            roles.remove(role);
            RoleDTO roleToRemove = null;
            for(RoleDTO r : rolesDTO)
            {
                if(r.getRoleOrdinal()==role.ordinal())
                {
                    roleToRemove = r;
                    break;
                }
            }
            if(roleToRemove!=null)
            {
                roleToRemove.delete();
                rolesDTO.remove(roleToRemove);
            }
            else
            {
                throw new Exception("unexpectedly couldn't find role in rolesDTO");
            }
        }
        else
        {
            throw new Exception("This employee doesn't have this role");
        }

    }
    public void persistDTO() throws Exception {
        employeeDTO.persist();
        for(RoleDTO r : rolesDTO)
        {
            r.persist();
        }
    }
    public boolean loggedIn()
    { return loggedIn;}
    public int getBranchId(){return branchId;}

}
