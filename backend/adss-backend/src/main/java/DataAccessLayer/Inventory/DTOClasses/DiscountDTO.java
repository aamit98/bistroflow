package DataAccessLayer.Inventory.DTOClasses;
import DataAccessLayer.Controller;
import DataAccessLayer.DTO;


import java.util.List;

public class DiscountDTO extends DTO {

    private int id;
    private List<String> categoryNames; // Assuming category association uses IDs
    private List<Integer> makats;  // Assuming product association uses IDs
    private double discountPercentage;
    private boolean fromSupplier;

    // Constructor (assuming a base class DTO with fromDB flag)
    public DiscountDTO(int id, List<String> categoryNames, List<Integer> makats, double discountPercentage, boolean fromSupplier, Controller controller, boolean fromDB) {
        super(controller, fromDB);
        this.id = id;
        this.categoryNames = categoryNames;
        this.makats = makats;
        this.discountPercentage = discountPercentage;
        this.fromSupplier = fromSupplier;
    }

    // Getters
    public int getId() {
        return id;
    }

    public List<String> getCategoryNames() {
        return categoryNames;
    }

    public List<Integer> getProductMakats() {
        return makats;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public boolean isFromSupplier() {
        return fromSupplier;
    }

    @Override
    public void persist() throws Exception {
        StringBuilder sb = new StringBuilder();

        if (makats != null && !makats.isEmpty()) {
            sb = new StringBuilder();
            for (int i = 0; i < makats.size() - 1; i++) {
                sb.append(makats.get(i)).append(",");
            }
            sb.append(makats.get(makats.size() - 1));
        }

        String cats;
        if (categoryNames == null || categoryNames.isEmpty()) {
            cats = "";
        }
        cats = String.join("#", categoryNames);
        insert(new Object[] { id, cats,  sb.toString(),  discountPercentage, fromSupplier});
        isPersisted = true;
    }
    public void unpersist() {
        isPersisted = false;
    }


    public void deleteDiscount() throws Exception {
        delete(new Object[] { id });
        isPersisted = false;
    }
}
