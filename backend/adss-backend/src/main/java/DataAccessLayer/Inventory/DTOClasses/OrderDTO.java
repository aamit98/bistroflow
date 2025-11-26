package DataAccessLayer.Inventory.DTOClasses;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;


public class OrderDTO extends DTO {

    private int makat;
    private int amount;
    private int dayOfMonth;
    private int id;

    // Constructor
    public OrderDTO(int makat, int amount, int dayOfMonth, int id, Controller controller, boolean fromDB) {
        super(controller, fromDB);
        this.makat = makat;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.id = id;
    }

    public int getProductMakat() {
        return makat;
    }

    public int getAmount() {
        return amount;
    }

    public int getDayOfMonth() {
        return dayOfMonth;
    }

    public int getId() {
        return id;
    }

    public void updateAmount(int amount) {
        if (isPersisted) {
            this.amount = amount;
            try {
                update(new Object[] { id }, "amount", amount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteOrder() throws Exception{
        if (isPersisted) {
            delete(new Object[] { id });
            isPersisted = false;

        }
    }
    public void unpersist() {
        isPersisted = false;
    }
    public void persist() throws Exception{
        insert(new Object[] { makat,amount, dayOfMonth,id });
        isPersisted = true;

    }
}
