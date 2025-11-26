package DomainLayer.Inventory;

import java.util.List;

public class Report {
    
    //////////// Fields ////////////
    private List<Product> productList;
    private String title;

    //////////// Constructors ////////////
    public Report(List<Product> products, String title){
        this.productList = products;
        this.title = title;

    }
    
    //////////// Methods ////////////
    public String toString(){
        StringBuilder str = new StringBuilder(title + "\n");
        int counter = 0;
        for(Product product: productList){
            str.append(counter).append(" ").append(product.toString());
            counter++;
        }
        str.append("\n");
        return str.toString();
    }

}
