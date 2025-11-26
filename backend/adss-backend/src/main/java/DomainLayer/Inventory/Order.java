package DomainLayer.Inventory;

import DataAccessLayer.Inventory.ControllerClasses.OrderController;
import DataAccessLayer.Inventory.DTOClasses.OrderDTO;

import java.time.LocalDate;

public class Order {
    private Product product;
    private int amount;
    private int dayOfMonth;
    private int id;

    private OrderDTO dto;
    public Order(int id, Product product , int amount, int dayOfMonth, OrderController oc, boolean fromDB) throws Exception{
        this.id = id;
        this.product = product;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.dto = new OrderDTO(getProductMakat(),amount,dayOfMonth,id,oc,fromDB);
        if(!fromDB)
            dto.persist();
    }
    public boolean isFilled(){
        return product.getAmount() + amount >= product.getMinimalAmount() ;
    }

    public void updateAmount(int newAmount){
        this.amount = newAmount;
        dto.updateAmount(newAmount);
    }
    public int getProductMakat(){
        return product.getMakat();
    }
    public void updateAmountToMinimal(){
        updateAmount(product.getMinimalAmount());
    }
    public boolean isToday(){
        LocalDate today = LocalDate.now();
        return today.getDayOfMonth() == dayOfMonth;
    }
    public boolean isTomorrow(){
        LocalDate today = LocalDate.now();
        if(dayOfMonth == 1){
            return today.getDayOfMonth() == today.lengthOfMonth();
        }
        return today.getDayOfMonth() == dayOfMonth-1 ;
    }
    public String toString(){
        if (dayOfMonth == -1){
            return  "Product Makat: " + product.getMakat() + "\n" +"Amount: " + amount + "\n" + "Manufacturer:" + product.getManufacturer() + "\n";
        }
        return "DayOfMonth: " + dayOfMonth + "\n" + "Product Makat: " + product.getMakat() + "\n" +"Amount: " + amount + "\n";

    }
    public void deleteDTO() throws Exception{
        dto.deleteOrder();
    }

}
