package DomainLayer.Inventory;
import DataAccessLayer.Inventory.ControllerClasses.DiscountController;
import DataAccessLayer.Inventory.ControllerClasses.ItemController;
import DataAccessLayer.Inventory.ControllerClasses.OrderController;
import DataAccessLayer.Inventory.ControllerClasses.ProductController;
import DataAccessLayer.Inventory.DTOClasses.DiscountDTO;
import DataAccessLayer.Inventory.DTOClasses.ItemDTO;
import DataAccessLayer.Inventory.DTOClasses.OrderDTO;
import DataAccessLayer.Inventory.DTOClasses.ProductDTO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;


public class InventoryFacade {

    //////////// Fields ////////////

    private Map<Integer,Product> allProducts;
    private Map<String,Category> allCategories;
    private List<Order> orders;
    private List<Discount> allDiscounts;
    private int defectives;

    private int discountCounter;
    private int orderCounter;
    CallBack callBackTomorrow;

    //////////// Controllers /////////////
    private ItemController itemController;
    private ProductController productController;
    private DiscountController discountController;
    private OrderController orderController;
    //////////// Constructors ////////////
    public InventoryFacade(CallBack callBackSunday,CallBack callBack) throws ClassNotFoundException {
        this.allDiscounts = new LinkedList<>();
        this.orders = new LinkedList<>();
        this.allProducts = new HashMap<>();
        this.allCategories = new HashMap<>();
        checkIfSunday(callBackSunday);
        this.callBackTomorrow = callBack;
        this.defectives = 0;
        this.discountCounter = 1;
        this.orderCounter = 1;


        this.discountController = new DiscountController();
        this.productController = new ProductController();
        this.itemController = new ItemController();
        this.orderController = new OrderController();
    }



    //////////// Methods ////////////
    public static void checkIfSunday(CallBack callBack) {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            callBack.call("");
        }
    }

    private Product locateProduct(int makat) throws Exception {
        Product prod = allProducts.get(makat);
        if(prod == null){
            throw new Exception("Product with makat: " + makat + " doesn't exist!");
        }
        return prod;
    }
   
    private Category locateCategory(String[] category) throws Exception {
        Category cat = allCategories.get(String.join(",",category));
        if(cat == null){
            throw new Exception("Category" + String.join(",", category) + " doesn't exist!");
        }
        return cat;
    }
   
    public void addItem(int makat, int sn, Date expiredDate, boolean inStorage,boolean fromDB) throws Exception {
        locateProduct(makat).addItem(sn,expiredDate,inStorage,itemController,fromDB);
        updateInventory();
    }
   
    public void removeItem(int makat, int sn, CallBack callback) throws Exception {
        Product prod = locateProduct(makat);
        prod.removeItem(sn,callback);
        if(prod.getAmount() < prod.getMinimalAmount()){
            orders.add(new Order(orderCounter ,prod,prod.getMinimalAmount(),-1,orderController,false));
            orderCounter++;
        }
        updateInventory();
    }
   
    public void addProduct(int makat,int minimalAmount,String name,String place,String manufacturer,double costPrice, double currentPrice,String[] categories, int popularity, boolean fromDB) throws Exception {
        if(allProducts.containsKey(makat)){
            throw new Exception("Product already exists! try again.");
        }
        Popularity p = switch (popularity) {
           case 1 -> Popularity.LOW;
           case 2 -> Popularity.MEDIUM;
           case 3 -> Popularity.HIGH;
           default -> null;
       };
        Product prod = new Product(makat,minimalAmount,name,place,manufacturer,costPrice,currentPrice,categories,popularity,productController,fromDB);
        Category cat = allCategories.get(String.join(",",categories));
        if(cat == null){
            cat = new Category(String.join(",",categories));
            allCategories.put(String.join(",",categories),cat);
        }
        cat.addProduct(prod);
        allProducts.put(makat,prod);
        
    }
   
    public void removeProduct(int makat,String[] categories) throws Exception {
        locateCategory(categories).removeProduct(makat);
        Product product = allProducts.remove(makat);
        product.deleteDTO();
        updateInventory();
    }
   
    public void moveItemToShelf(int makat, int amount) throws Exception {
        locateProduct(makat).moveToShelf(amount);
    }
   
    public void moveItemToStorage(int makat, int amount) throws Exception {
        locateProduct(makat).moveToStorage(amount);
    }

    public String reportDefective(int makat,int amount) throws Exception {
        try{
            locateProduct(makat).reportDefective(amount);
            defectives += amount;
            if(defectives >= 5){
                List<Integer> makats = new LinkedList<>();
                for(Product product: allProducts.values()){
                    if(product.hasDefective()){
                        makats.add(product.getMakat());
                    }
                }
                return makeReport(null,makats,"Report for defective products- \n");
            }
            return null;
        }
        catch (Exception e){
            throw e;
        }

    }

    public String makeReport(List<String[]> catNames, List<Integer> prodMakats,String title) throws Exception
    {
        List<Product> products =extractProducts(prodMakats, catNames);
        return new Report(products,title).toString();
    }
   
    public void addDiscountFromSupplier(List<Integer> prodMakats,List<String[]> catNames,int precentage, boolean fromDB) throws Exception {
        List<Product> products =extractProducts(prodMakats, catNames);
        if(prodMakats == null){
            List<Category> cats = new LinkedList<>();
            for(String[] str : catNames){
                cats.add(locateCategory(str));
            }
            allDiscounts.add(new Discount(discountCounter,cats,null,precentage,true,discountController,fromDB));
        }
        else{
            allDiscounts.add(new Discount(discountCounter,null,products,precentage,true,discountController,fromDB));

        }
        discountCounter++;
        for(Product prod: products){
            prod.updatePriceSupplierDiscount(precentage);
        }
    }
   
    public void addDiscountFromStore(List<Integer> prodMakats,List<String[]> catNames,int precentage,boolean fromDB) throws Exception {
        List<Product> products =extractProducts(prodMakats, catNames);
        if(prodMakats == null){
            List<Category> cats = new LinkedList<>();
            for(String[] str : catNames){
                cats.add(locateCategory(str));
            }
            allDiscounts.add(new Discount(discountCounter,cats,null,precentage,false,discountController,fromDB));
        }
        else{
            allDiscounts.add(new Discount(discountCounter,null,products,precentage,false,discountController,fromDB));
        }
        discountCounter++;
        for(Product prod: products){
            prod.updatePriceStoreDiscount(precentage);
        }
    }
    public String getDiscounts(){
        StringBuilder toReturn = new StringBuilder();
        int counter = 1;
        for(Discount disc: allDiscounts){
            toReturn.append(counter).append(". ").append(disc.toString());
            counter++;
        }

        return toReturn.toString();
    }


    public void removeDiscountFromSupplier(int discountId) throws Exception {
        if(discountCounter == 0){
            throw new Exception("No discounts! try again after adding discounts.");
        }
        Discount disc = allDiscounts.remove(discountId-1);
        disc.deleteDTO();
        List<Product> productsOnDiscount = disc.getProductsOnDiscount();
        for(Product prod: productsOnDiscount){
            prod.removeSupplierDiscount();
        }
    }
    public void removeDiscountFromStore(int discountId) throws Exception {
        if(discountCounter == 0){
            throw new Exception("No discounts! try again after adding discounts.");
        }
        Discount disc = allDiscounts.remove(discountId-1);
        disc.deleteDTO();
        List<Product> productsOnDiscount = disc.getProductsOnDiscount();
        for(Product prod: productsOnDiscount){
            prod.removeStoreDiscount();
        }
    }

    private List<Product> extractProducts(List<Integer> prodMakats, List<String[]> catNames) throws Exception {
        List<Product> products = new LinkedList<>();
        if(catNames != null){
            for (String[] cat: catNames){
                products.addAll(locateCategory(cat).getCategoryProducts());
            }
        }
        if(prodMakats != null){
            for(int makat: prodMakats){
                products.add(locateProduct(makat));
            }
        }
        return  products;
    }

    public String getAllProducts() throws Exception {
        if(allProducts.values().size() == 0){
            throw new Exception("No Products! try adding products first");
        }
        StringBuilder sb = new StringBuilder();
        for(Product product: allProducts.values()){
            sb.append(product.toString());
        }
        return sb.toString();
    }

    public String getAllItems(int makat) throws Exception {
        List<Item> items = locateProduct(makat).getItems();
        if(items.size() == 0){
            throw new Exception("No Items Found! try adding items");
        }
        StringBuilder sb = new StringBuilder();
        for(Item item: items){
            sb.append(item.toString());
        }
        return sb.toString();
    }

    // part 2


    public void updateOrder(int orderId,int newAmount) throws Exception {
        Order order = orders.get(orderId-1);
        if(order.isToday())
            throw new Exception("Cant update the order one day before!");
        order.updateAmount(newAmount);
    }

    public void addOrder(int productMakat, int amount , int dayOfMonth ,boolean fromDB) throws Exception {
        Product prod = locateProduct(productMakat);
        orders.add(new Order(orderCounter,prod,amount,dayOfMonth,orderController,fromDB));
        orderCounter++;
    }

    //Delete and remove the order from the system
    public void deleteOrder(int orderId) throws Exception {
        if(orderCounter == 0){
            throw new Exception("No orders! try again after adding orders.");
        }
        Order order = orders.remove(orderId-1);
        order.deleteDTO();
    }


    public String getOrders(){
        StringBuilder toReturn = new StringBuilder();
        int counter = 1;
        for(Order order: orders){
            toReturn.append(counter).append(". ").append(order.toString());
            counter++;
        }

        return toReturn.toString();
    }



    // Generic method that verify all products
     public void updateInventory() throws Exception {
        for(Product product : allProducts.values()){
            for(Item item : product.getItems()) {
                if(item.isDefective()) item.setDefective(true);
            }
        }
    }
    public void checkIfTomorrowOrders(){
        for (Order o: orders){
            if(o.isTomorrow() && !o.isFilled()) {
                callBackTomorrow.call("order of " + o.getProductMakat() + " is for tomorrow and there weren't enough for the minimal amount," +
                        " order was updated, please notify the supplier");
                o.updateAmountToMinimal();
            }
        }
    }






    public void loadInitialData() {
        try {
            //load Items
            List<ItemDTO> itemDTOs = itemController.selectAllItemsDTOs();
            List<Item> items = new LinkedList<>();
            for (ItemDTO itemDTO : itemDTOs) {
                Item item = new Item(
                        itemDTO.getSn(),
                        itemDTO.isInStorage(),
                        itemDTO.getExpireDate(),
                        itemController,
                        true
                );
                item.setDefective(itemDTO.isDefective());
                items.add(item);
            }
            // Load products
            List<ProductDTO> productDTOs = productController.selectAllProductsDTOs();
            for (ProductDTO productDTO : productDTOs) {
                addProduct(
                        productDTO.getMakat(),
                        productDTO.getMinimalAmount(),
                        productDTO.getName(),
                        productDTO.getPlace(),
                        productDTO.getManufacturer(),
                        productDTO.getCostPrice(),
                        productDTO.getCurrentPrice(),
                        productDTO.getCategories(),
                        productDTO.getPopularity(),
                        true
                );
                Product product = allProducts.get(productDTO.getMakat());
                List<Integer> itemsSN = productDTO.getItems();
                for(Item item : items){
                    if(itemsSN.contains(item.getId()))
                        product.addItem(item);
                }
            }


            // Load discounts
            List<DiscountDTO> discountDTOs = discountController.selectAllDiscountsDTOs();
            for (DiscountDTO discountDTO : discountDTOs) {
                List<String> categoryNames = discountDTO.getCategoryNames();
                List<Integer> productMakats = discountDTO.getProductMakats();
                boolean fromSupplier = discountDTO.isFromSupplier();
                double discountPercentage = discountDTO.getDiscountPercentage();
                List<Category> categories = new ArrayList<>();
                List<Product> products = new ArrayList<>();

                if (categoryNames != null) {
                    for (String categoryName : categoryNames) {
                        Category category = allCategories.get(categoryName);
                        categories.add(category);
                    }
                }

                if (productMakats != null) {
                    for (int makat : productMakats) {
                        Product product = allProducts.get(makat);
                        if (product != null) {
                            products.add(product);
                        }
                    }
                }

                allDiscounts.add(new Discount(
                        discountDTO.getId(),
                        categories.isEmpty() ? null : categories,
                        products.isEmpty() ? null : products,
                        discountPercentage,
                        fromSupplier,
                        discountController,
                        true
                ));
                if (discountDTO.getId() >= discountCounter) {
                    discountCounter = discountDTO.getId() + 1;
                }
            }

            // Load orders
            List<OrderDTO> orderDTOs = orderController.selectAllOrdersDTOs();
            for (OrderDTO orderDTO : orderDTOs) {
                Product product = allProducts.get(orderDTO.getProductMakat());
                if (product != null) {
                    orders.add(new Order(
                            orderDTO.getId(),
                            product,
                            orderDTO.getAmount(),
                            orderDTO.getDayOfMonth(),
                            orderController,
                            true
                    ));
                    if (orderDTO.getId() >= orderCounter) {
                        orderCounter = orderDTO.getId() + 1;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    public void deleteAllFromDB() throws Exception {
        productController.deleteAllInventory();
        this.allDiscounts = new LinkedList<>();
        this.orders = new LinkedList<>();
        this.allProducts = new HashMap<>();
        this.allCategories = new HashMap<>();
        this.defectives = 0;
        this.discountCounter = 1;
        this.orderCounter = 1;
    }
    public void simulateInventoryOperations() throws Exception {
        // Add categories
        String[] electronicsCategory = {"Electronics", "Devices", "Gadgets"};
        String[] foodCategory = {"Food", "Groceries", "Perishables"};
        String[] clothingCategory = {"Clothing", "Apparel", "Wearables"};

        // Add products
        addProduct(1, 10, "Smartphone", "Aisle 1", "TechCorp", 500, 699, electronicsCategory, 2, false);
        addProduct(2, 5, "Laptop", "Aisle 2", "CompTech", 1000, 1299, electronicsCategory, 3, false);
        addProduct(3, 20, "Apple", "Aisle 3", "FreshFarms", 1, 1.5, foodCategory, 1, false);
        addProduct(4, 15, "Jeans", "Aisle 4", "FashionCo", 30, 50, clothingCategory, 2, false);
        addProduct(5, 10, "T-Shirt", "Aisle 4", "FashionCo", 10, 20, clothingCategory, 3, false);

        // Add items to the products
        addItem(1, 1001, java.sql.Date.valueOf("2024-12-31"), true, false);
        addItem(1, 1002, java.sql.Date.valueOf("2024-12-31"), true, false);
        addItem(2, 1003, java.sql.Date.valueOf("2025-01-31"), true, false);
        addItem(3, 1004, java.sql.Date.valueOf("2024-06-30"), true, false);
        addItem(4, 1005, java.sql.Date.valueOf("2025-12-31"), true, false);
        addItem(5, 1006, java.sql.Date.valueOf("2025-06-30"), true, false);
        addItem(5, 1007, java.sql.Date.valueOf("2025-06-30"), true, false);

        // Add an order for a product
        addOrder(1, 5, 15, false);
        addOrder(2, 2, 20, false);
        addOrder(3, 10, 25, false);
        addOrder(4, 8, 10, false);

        // Add discounts for products
        List<Integer> productMakatList = new LinkedList<>(Arrays.asList(1, 2, 3));
        List<String[]> categoryList = new LinkedList<>();
        categoryList.add(electronicsCategory);
        categoryList.add(clothingCategory);

        addDiscountFromSupplier(productMakatList, null, 10, false); // 10% discount from supplier on products
        addDiscountFromStore(null, categoryList, 15, false); // 15% discount from store on categories

        // Add a discount for a single product from store
        List<Integer> singleProductMakat = new LinkedList<>(Arrays.asList(5));
        addDiscountFromStore(singleProductMakat, null, 20, false); // 20% discount from store on a single product

    }

    }
