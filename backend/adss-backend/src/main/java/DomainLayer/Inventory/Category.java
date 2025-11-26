package DomainLayer.Inventory;
import java.util.*;

public class Category {

    ///////////// Fields //////////////
    private String categoryName;
    private Map<Integer, Product> categoryProducts;


    ///////////// Methods //////////////

    public Category(String categoryName){
        this.categoryName = categoryName;
        this.categoryProducts = new HashMap<>();
    }

    public List<Product> getCategoryProducts(){
        List<Product> prods = new LinkedList<>();
        for(Product prod: categoryProducts.values()){
            prods.add(prod);
        }
        return prods;
    }

    public String getCategoryName(){
        return categoryName;
    }
    public void addProduct(Product prod){
        categoryProducts.put(prod.getMakat(),prod);
    }
    public void removeProduct(int makat) throws Exception {
        if(!categoryProducts.containsKey(makat)){
            throw new Exception("Product with makat:" + makat + " doesn't exist!");
        }
        categoryProducts.remove(makat);
    }
}
