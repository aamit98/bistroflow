package DataAccessLayer.HR.DTOClasses;

import DataAccessLayer.DTO;
import DataAccessLayer.HR.ControllerClasses.WeeklyRoleConstraintsController;
import GlobalClasses.Role;

public class WeeklyRoleConstraintsDTO extends DTO {
    private final int[][][] roleIdsArray;
    private final int branchId;
    private final int date;
    public WeeklyRoleConstraintsDTO(int[][][] roleIdsArray, int branchId, int date, WeeklyRoleConstraintsController controller, boolean fromDB) {
        super(controller, fromDB);
        this.roleIdsArray = roleIdsArray;
        this.branchId = branchId;
        this.date = date;
    }


    public int[][][] getRoleIdsArray() {
        return roleIdsArray;
    }

    public int getBranchId() {
        return branchId;
    }

    public int getDate() {
        return date;
    }

    @Override
    public void persist() throws Exception {
            int rolesAmount = Role.values().length;
            for(int i =0 ; i<7;i++)
            {
                for(int j=0;j<2;j++)
                {
                    int[] roleCounter = new int[rolesAmount];
                    for(int roleId : roleIdsArray[i][j])
                    {
                        roleCounter[roleId]++;
                    }
                    for(int k = 0;k<rolesAmount;k++)
                    {
                        if(roleCounter[k]!=0)
                        {
                            insert(new Object[]{date,branchId,i*2+j,k,roleCounter[k]});
                        }
                    }

                }
            }
            isPersisted = true;
        }

    public void delete() throws Exception {
        if(isPersisted)
        {
                delete(new Object[]{date,branchId});
                isPersisted = false;
        }
    }
}
