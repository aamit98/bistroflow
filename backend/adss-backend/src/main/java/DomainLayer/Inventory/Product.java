package DomainLayer.Inventory;

import DataAccessLayer.Inventory.ControllerClasses.ItemController;
import DataAccessLayer.Inventory.ControllerClasses.ProductController;
import DataAccessLayer.Inventory.DTOClasses.ProductDTO;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Product{


    //Constants:
    final int MAXPRECENTAGE = 100;

//////////////////////////Fields//////////////////////////
    private int makat;
    private String name;
    private String place; //TODO: check moodle
    private String manufacturer;
    private double costPrice;
    private double currentPrice;
    private String[] categories;
    private int popularity;
    private double supplierPrecentage;
    private double storePrecentage;
    private List<Item> items;
    private int minimalAmount;

    private ProductDTO dto;

//////////////////////////Constructors//////////////////////////
    public Product(int makat, int minimalAmount, String name, String place, String manufacturer, double costPrice, double currentPrice, String[] categories, int popularity, List<Item> items, ProductController pc, boolean fromDB) throws Exception{
        this.makat = makat;
        this.name = name;
        this.place = place;
        this.manufacturer = manufacturer;
        this.costPrice = costPrice;
        this.currentPrice = currentPrice;
        this.categories = categories;
        this.popularity = popularity;
        this.items = items;
        this.storePrecentage = 0;
        this.supplierPrecentage = 0;
        this.minimalAmount = minimalAmount;


        List<Integer> temp = new LinkedList<>();
        for(Item item: items){
            temp.add(item.getId());
        }
        this.dto = new ProductDTO(makat,name,place,manufacturer,costPrice,currentPrice,categories,popularity,0,0,temp,minimalAmount,pc,fromDB);
        if(!fromDB)
            dto.persist();

    }
 
    public Product(int makat,int minimalAmount,String name,String place,String manufacturer,double costPrice, double currentPrice,String[] categories, int popularity,ProductController pc, boolean fromDB) throws Exception{
        this.makat = makat;
        this.name = name;
        this.place = place;
        this.manufacturer = manufacturer;
        this.costPrice = costPrice;
        this.currentPrice = currentPrice;
        this.categories = categories;
        this.popularity = popularity;
        this.items = new LinkedList<Item>();
        this.storePrecentage = 0;
        this.supplierPrecentage = 0;
        this.minimalAmount = minimalAmount;

        this.dto = new ProductDTO(makat,name,place,manufacturer,costPrice,currentPrice,categories,popularity,0,0,new LinkedList<>(),minimalAmount,pc,fromDB);
        if(!fromDB)
            dto.persist();
    }


//////////////////////////Getters//////////////////////////


    public int getInStorageAmount(){
        Iterator<Item> iterator = items.iterator();
        int storageAmount = 0;
        while (iterator.hasNext()){
            Item item = iterator.next();
            if (item.getInStorage() ) { storageAmount++;}
        }
        return storageAmount;
    }

    public int getInShelfAmount(){
        Iterator<Item> iterator = items.iterator();
        int shelfAmount = 0;
        while (iterator.hasNext()){
            Item item = iterator.next();
            if (!item.getInStorage() )
            { shelfAmount++;}
        }
        return shelfAmount;
    }

    public int getAmount(){
        return getInShelfAmount()+getInStorageAmount();
    }

    public int getMinimalAmount(){
        return minimalAmount;
    }
    
    public int getMakat() {
        return makat;
    }

    public int getAmountDefectives() {
        int count = 0;
        for(Item item : items){
            if(item.isDefective())
                count++;
        }
        return count;
    }

    public String getName(){ return name;}

    public double getCurrentPrice(){
        return currentPrice * (MAXPRECENTAGE - this.storePrecentage);
    }
   
    public double getCostPrice(){return costPrice * (MAXPRECENTAGE - this.supplierPrecentage);}

    public List<Item> getItems() {
        return items;
    }
    public String getManufacturer(){return manufacturer;   }
   

//////////////////////////Methods//////////////////////////
    public void addItem(int sn,Date date, boolean inStorage, ItemController ic, boolean fromDB) throws Exception {
    for(Item item: items){
        if(item.getId() == sn)
            throw new Exception("Item already exists!");
    }
    items.add(new Item(sn,inStorage,date,ic,fromDB));
    List<Integer> temp = new LinkedList<>();
    for(Item item: items){
        temp.add(item.getId());
    }
    dto.updateItems(temp);
    }
    public void addItem(Item item){
        items.add(item);
    }


    public void removeItem(int sn, CallBack callBack) throws Exception{
    Iterator<Item> iterator = items.iterator();
    while (iterator.hasNext()) {
        Item item = iterator.next();
        if (item.getId() == sn) {
            iterator.remove();
            item.deleteDTO();
            break;
        }
    }

    int currAmount = getAmount();
    if(currAmount < minimalAmount){
        callBack.call("Minimal amount exceeded! added order for this product for minimal amount!");
    }
    List<Integer> temp = new LinkedList<>();
    for(Item item: items){
        temp.add(item.getId());
    }
    dto.updateItems(temp);
}

    public void moveToStorage(int amount) throws Exception {
        if(amount < 0){
            throw new Exception("invalid amount!");
        }
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (!item.getInStorage()) {
                item.moveToStorage();
                amount--;
                if(amount == 0)
                    break;
            }
        }
        if(amount > 0){
            throw new Exception("invalid amount!");
        }
}

    public void moveToShelf(int amount) throws Exception {
        if(amount < 0){
           throw new Exception("invalid amount!");
        }
        Iterator<Item> iterator = items.iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (item.getInStorage()) {
                item.moveToShelf();
                amount--;
                if(amount == 0)
                    break;
            }
        }
        if(amount > 0){
            throw new Exception("invalid amount!");
        }
}

    public void updatePriceStoreDiscount(double discountPrecentage) throws Exception{
        this.storePrecentage = Math.max(this.storePrecentage,discountPrecentage);
        this.currentPrice = currentPrice * (100/storePrecentage);
        dto.updateCurrentPrice(currentPrice);
        dto.updateStorePercentage(this.storePrecentage);
    }

    public void updatePriceSupplierDiscount(double discountPrecentage) throws Exception{
        this.supplierPrecentage = Math.max(this.supplierPrecentage,discountPrecentage);
        this.currentPrice = currentPrice * (100/supplierPrecentage);
        dto.updateCurrentPrice(currentPrice);
        dto.updateSupplierPercentage(this.supplierPrecentage);

    }

    public void removeStoreDiscount() throws Exception{
        this.storePrecentage = 0;
        dto.updateStorePercentage(0);
    }
   
    public void removeSupplierDiscount() throws Exception{
        this.supplierPrecentage = 0;
        dto.updateSupplierPercentage(0);
    }
   
    public void reportDefective(int amount) throws Exception {
        for(Item item : items){
            if(!item.isDefective()){
                item.setDefective(true);
                amount--;
            }
            if(amount == 0)
                break;
        }
        if(amount != 0){
            throw new Exception("Problem: not enough items to report them defective! did as much as possible.");
        }
    }

    public boolean hasDefective(){
        for(Item i : this.items)
        {
            if(i.isDefective())
                return true;
        }
        return false;
    }

    public String toString(){

    return "Name: " + this.name + " ," +
            "Location: " + this.place + " ," +
            "Manufacturer: " + this.manufacturer +" ," +
            "Amount: " + getAmount() + " ," +
            "Storage Amount: " + getInStorageAmount() +" ," +
            "Shelf Amount: " + getInShelfAmount() +" ," +
            "Categories: " + String.join(",",categories)+ "\n";}

    public void deleteDTO() throws Exception{
        for(Item item: items){
            item.deleteDTO();
        }
        dto.deleteProduct();
    }

}


