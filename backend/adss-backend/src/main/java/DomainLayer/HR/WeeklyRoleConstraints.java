package DomainLayer.HR;

import DataAccessLayer.HR.ControllerClasses.WeeklyRoleConstraintsController;
import DataAccessLayer.HR.DTOClasses.WeeklyRoleConstraintsDTO;
import GlobalClasses.Role;
//import GlobalClasses.Role;
/**
 * this class is used to describe constraints on how many employees of each role are needed for each shift of the
 * given week. for example, if constraintsArray[0][1] = {Role.MANAGER,Role.STOREKEEPER,Role.CASHIER,Role.CASHIER}
 * the sunday evening shift should have at least 1 manager, 1 storekeeper and 2 cashiers.
 */
public class WeeklyRoleConstraints {
    private static  WeeklyRoleConstraints defaultConstraints = null;
    private Role[][][] constraintsArray;
    private WeeklyRoleConstraintsDTO weeklyRoleConstraintsDTO;

    /**
     *
     * @param constraintsArray - should be size [7][2][any]. first index-day.second index-shift(morning/evening) third-actual array.
     * @throws Exception if given wrong size / null in any array.
     */
    public WeeklyRoleConstraints(Role[][][] constraintsArray, int branchId, int date, WeeklyRoleConstraintsController controller,
                                boolean fromDB ) throws Exception {
        if(constraintsArray==null || constraintsArray.length==7)
        {
            for(int i =0;i<7;i++)
            {
                if(constraintsArray[i]==null || constraintsArray[i].length!=2)
                {
                    throw new Exception("wrong array size!");
                }
                for(int j =0;j<2;j++)
                {
                    if(constraintsArray[i][j]==null)
                    {
                        throw new Exception("wrong array size!");
                    }
                    for(int k=0;k<constraintsArray[i][j].length;k++)
                    {
                        if(constraintsArray[i][j][k]==null)
                        {
                            throw new Exception("contains null role!");
                        }
                    }
                }
            }
        }
        else
        {
            throw new Exception("wrong array size!");
        }
        this.constraintsArray = constraintsArray;
        int[][][] rolesIdsArray = new int[7][2][];
        for(int i =0;i<7;i++)
        {
            for(int j=0;j<2;j++)
            {
                rolesIdsArray[i][j] = new int[constraintsArray[i][j].length];
                for(int k=0;k<constraintsArray[i][j].length;k++)
                {
                    rolesIdsArray[i][j][k] = constraintsArray[i][j][k].ordinal();
                }
            }
        }
        weeklyRoleConstraintsDTO = new WeeklyRoleConstraintsDTO(rolesIdsArray,branchId,date,controller,fromDB);

    }
    public static WeeklyRoleConstraints getDefaultConstraints()
    {
        if(defaultConstraints==null)
        {
            Role[] roleArray = new Role[]{Role.MANAGER,Role.STOREKEEPER,Role.CASHIER};
            Role[][][] constraintsArr = new Role[7][2][];
            for(int i =0;i<7;i++)
            {
                for (int j=0;j<2;j++)
                {
                    constraintsArr[i][j] = roleArray;
                }
            }
            try {
                defaultConstraints = new WeeklyRoleConstraints(constraintsArr,-1,-1,null,true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return defaultConstraints;
    }

    /**
     *
     * @param day 0=sunday .. 6=saturday.
     * @param shift 0=morning 1=evening.
     * @return minimum roles needed for said shift.
     */
    public Role[] getConstraints(int day,int shift) throws Exception {
        if(day>6||day<0)
        {
            throw new Exception("day must be between 0-6");
        }
        if(shift>1||shift<0)
        {
            throw new Exception("day must be between 0-1");
        }
        return constraintsArray[day][shift].clone();
    }


    public Role[][][] getConstraintsArray() {
        return constraintsArray.clone();
    }
    public void persistDTO() throws Exception {
        weeklyRoleConstraintsDTO.persist();
    }
    public void deleteDTO() throws Exception {
        weeklyRoleConstraintsDTO.delete();
    }


}
