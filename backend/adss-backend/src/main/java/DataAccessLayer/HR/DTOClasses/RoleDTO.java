package DataAccessLayer.HR.DTOClasses;

import DataAccessLayer.DTO;
import DataAccessLayer.HR.ControllerClasses.RoleController;

public class RoleDTO extends DTO {
    private final int employeeId;
    private final int roleOrdinal;

    public RoleDTO(int employeeId, int roleOrdinal, RoleController controller, boolean fromDB) {
        super(controller, fromDB);
        this.employeeId = employeeId;
        this.roleOrdinal = roleOrdinal;
    }
    public int getRoleOrdinal(){return roleOrdinal;}
    @Override
    public void persist() throws Exception {
        insert(new Object[]{employeeId,roleOrdinal});
        isPersisted = true;
    }
    public void delete() throws Exception {
        if(isPersisted)
        {
            delete(new Object[]{employeeId,roleOrdinal});
            isPersisted = false;
        }
    }
}
