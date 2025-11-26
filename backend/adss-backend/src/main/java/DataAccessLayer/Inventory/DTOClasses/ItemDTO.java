package DataAccessLayer.Inventory.DTOClasses;


import DataAccessLayer.Controller;
import DataAccessLayer.DTO;

import java.util.Date;

public class ItemDTO extends DTO {

    private int sn;
    private boolean inStorage;
    private Date expireDate;
    private boolean isDefective;

    // Constructor
    public ItemDTO(int sn, boolean inStorage, Date expireDate, boolean isDefective, Controller controller, boolean fromDB) {
        super(controller, fromDB);
        this.sn = sn;
        this.inStorage = inStorage;
        this.expireDate = expireDate;
        this.isDefective = isDefective;
    }

    // Getters (assuming standard naming convention)
    public int getSn() {
        return sn;
    }

    public boolean isInStorage() {
        return inStorage;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public boolean isDefective() {
        return isDefective;
    }

    public void persist() throws Exception{
        insert(new Object[] {  sn, inStorage, expireDate, isDefective});
        isPersisted = true;

    }


    public void updateInStorage(boolean inStorage) throws Exception {
        if (isPersisted) {
            this.inStorage = inStorage;
            update(new Object[] { sn }, "inStorage", inStorage);


        }
    }
    public void updateDefective(boolean isDefective) throws Exception{
        if (isPersisted) {
            this.isDefective = isDefective;
            update(new Object[] { sn }, "isDefective", isDefective);

        }
    }

    public void deleteItem() throws Exception {

        if (isPersisted) {
                delete(new Object[]{sn});
                isPersisted = false;
            }
    }
    public void unpersist() {
        isPersisted = false;
    }

}

