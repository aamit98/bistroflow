package DomainLayer.Inventory;

import DataAccessLayer.Inventory.ControllerClasses.ItemController;
import DataAccessLayer.Inventory.DTOClasses.ItemDTO;

import java.util.Date;

public class Item{

    //////////// Fields ////////////
    private int sn;
    private boolean inStorage;
    private Date expireDate;
    private boolean isDefective;
    private ItemDTO dto;

    //////////// Constructors ////////////
    //For Products with expiration date example: food, medicine
    public Item(int sn, boolean inStorage, Date expireDate, ItemController ic, boolean fromDB) throws Exception{
        this.sn = sn;
        this.inStorage = inStorage;
        this.expireDate = expireDate;
        this.isDefective = false;
        this.dto = new ItemDTO(sn,inStorage,expireDate,isDefective,ic,fromDB);
        if(!fromDB)
            dto.persist();
    }

    //////////// Getters ////////////
    public int getId(){return sn;}
   
    public boolean getInStorage(){return  inStorage;}

    public Date getExpireDate(){ return expireDate;}
    
    //////////// Methods ////////////
    public void moveToStorage() throws Exception{
        inStorage =true;
        dto.updateInStorage(inStorage);
    }

    public void moveToShelf() throws Exception{
        inStorage = false;
        dto.updateInStorage(inStorage);
    }

    public void setDefective(boolean isDefective) throws Exception{
        this.isDefective = isDefective;
        dto.updateDefective(isDefective);
    }

    public boolean isDefective() {return  (isDefective&(expireDate.after(new Date())))  ;} //if the item is defective and not expired

    public void deleteDTO() throws Exception{
        dto.deleteItem();
    }

    public String toString(){
        return "sn: " + sn + " in storage: " + inStorage + " expire date:" + expireDate.toString() + " is defective: " + isDefective + "\n";
    }
}