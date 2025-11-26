package DataAccessLayer.Inventory.DTOClasses;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;

import java.util.List;

public class ProductDTO extends DTO {
    private int makat;
    private String name;
    private String place;
    private String manufacturer;
    private double costPrice;
    private double currentPrice;
    private String[] categories;
    private Integer popularity;
    private double supplierPercentage;
    private double storePercentage;
    private List<Integer> items;
    private int minimalAmount;

    public ProductDTO(int makat, String name, String place, String manufacturer, double costPrice, double currentPrice,
                      String[] categories, Integer popularity, double supplierPercentage, double storePercentage,
                      List<Integer> items, int minimalAmount, Controller controller, boolean fromDB) {
        super(controller, fromDB);
        this.makat = makat;
        this.name = name;
        this.place = place;
        this.manufacturer = manufacturer;
        this.costPrice = costPrice;
        this.currentPrice = currentPrice;
        this.categories = categories;
        this.popularity = popularity;
        this.supplierPercentage = supplierPercentage;
        this.storePercentage = storePercentage;
        this.items = items;
        this.minimalAmount = minimalAmount;
    }

    public int getMakat() {
        return makat;
    }

    public String getName() {
        return name;
    }

    public String getPlace() {
        return place;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String[] getCategories() {
        return categories;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public double getSupplierPercentage() {
        return supplierPercentage;
    }

    public double getStorePercentage() {
        return storePercentage;
    }

    public List<Integer> getItems() {
        return items;
    }

    public int getMinimalAmount() {
        return minimalAmount;
    }

    @Override
    public void persist() throws Exception{
        try {
            StringBuilder sb = new StringBuilder();

            if (items != null && !items.isEmpty()) {
                for (int i = 0; i < items.size() - 1; i++) {
                    sb.append(items.get(i)).append(", ");
                }
                sb.append(items.get(items.size() - 1));
            }
            insert(new Object[] { makat, name, place, manufacturer, costPrice, currentPrice, String.join(",",categories), popularity,
                    supplierPercentage, storePercentage, sb.toString(), minimalAmount });
            isPersisted = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void updateSupplierPercentage(double supplierPercentage) throws Exception{
        if (isPersisted) {
            this.supplierPercentage = supplierPercentage;
            try {
                update(new Object[] { makat }, "supplierPercentage", supplierPercentage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateStorePercentage(double storePercentage) throws Exception{
        if (isPersisted) {
            this.storePercentage = storePercentage;
            try {
                update(new Object[] { makat }, "storePercentage", storePercentage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateItems(List<Integer> items) throws Exception{
        if (isPersisted) {
            this.items = items;
            try {
                StringBuilder sb = new StringBuilder();

                if (items != null && !items.isEmpty()) {
                    for (int i = 0; i < items.size() - 1; i++) {
                        sb.append(items.get(i)).append(", ");
                    }
                    sb.append(items.get(items.size() - 1));
                }
                update(new Object[] { makat }, "items", sb.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void deleteProduct()  throws Exception{
        if (isPersisted) {
            try {
                delete(new Object[] { makat });
                isPersisted = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unpersist() {
        isPersisted = false;
    }

    public void updateCurrentPrice(double currentPrice) throws Exception {
        if (isPersisted) {
            this.currentPrice = currentPrice;
            try {
                update(new Object[] { makat }, "currentPrice", currentPrice);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}