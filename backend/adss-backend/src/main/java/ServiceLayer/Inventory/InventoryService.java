package ServiceLayer.Inventory;

import DomainLayer.Inventory.*;
import DomainLayer.Inventory.InventoryFacade;
import ServiceLayer.Response;

import java.util.Date;
import java.util.List;

public class InventoryService {

    /////////// Fields //////////
    InventoryFacade IF;



    //@TODO: default json contains Succesful.

    private CallBack callBackMinimalAmount;

    /////////// Methods //////////

    public InventoryService(CallBack callBackSunday, CallBack callBack) throws ClassNotFoundException {

        IF = new InventoryFacade(callBackSunday, callBack);
        this.callBackMinimalAmount = callBack;
    }

    public Response addItem(int makat, int sn, Date expiredDate, boolean inStorage) {
        try{
            IF.addItem(makat, sn, expiredDate,inStorage,false);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);
        }

    }
   
    public Response removeItem(int makat,  int sn){
        try{
            IF.removeItem(makat, sn, callBackMinimalAmount);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response addProduct(int makat,int minimalAmount,String name,String place,String manufacturer,double costPrice, double currentPrice,String[] categories, int popularity){
        try{
            IF.addProduct(makat,minimalAmount,name, place,manufacturer,costPrice,currentPrice,categories,popularity,false);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
  
    public Response removeProduct(int makat,String[] categories){
        try{
            IF.removeProduct(makat,categories);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response moveItemToShelf(int makat,int amount) {
        try{
            IF.moveItemToShelf(makat,amount);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response moveItemToStorage(int makat,int amount) {
        try{
            IF.moveItemToStorage(makat,amount);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response reportDefective(int makat, int amount){
        try{
            String report  = IF.reportDefective(makat,amount);
            if(report == null){
                return new Response(null,"Succesful.");
            }
            return new Response(null,report);

        }
        catch (Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response makeReport(List<String[]> catNames, List<Integer> prodMakats){
        try{
            String title = "Report for chosen categories and products: ";
            String report = IF.makeReport(catNames,prodMakats,title);
            return new Response(null,report);

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response addDiscountFromSupplier(List<Integer> prodMakats, List<String[]> catNames, int precentage)  {
        try{
            IF.addDiscountFromSupplier(prodMakats,catNames,precentage,false);
            return new Response(null,"Succesful.");
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
   
    public Response addDiscountFromStore(List<Integer> prodMakats,List<String[]> catNames,int precentage) {
        try{
            IF.addDiscountFromStore(prodMakats,catNames,precentage,false);
            return new Response(null,"Succesful.");
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response removeDiscountFromSupplier(int orderId)  {
        try{
            IF.removeDiscountFromSupplier(orderId);
            return new Response(null,"Succesful.");
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
  
    public Response removeDiscountFromStore(int orderId) {
        try{
            IF.removeDiscountFromStore(orderId);
            return new Response(null,"Succesful.");
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
  
    public Response getAllProducts() {
        try{
            String products = IF.getAllProducts();
            if(products != null){                                                   //If i did not get null
            return new Response(null,products);
            }
            else{                                                                   //If i got null and there is no products
                return new Response("No products found.",null);
            }
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response getAllItems(int makat) {
        try{
            String str = IF.getAllItems(makat);
            return new Response(null,str);

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response loadInitialData() {
        try {
            IF.loadInitialData();
            IF.checkIfTomorrowOrders();

 //           IF.deleteAllFromDB();
//            IF.simulateInventoryOperations();
            return new Response(null,"Succesful.");

        } catch (Exception e) {
            return new Response(e.getMessage(),null);
        }
    }

    //part 2

    public Response updateOrder(int orderId,int newAmount){
        try{
            IF.updateOrder(orderId,newAmount);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response addOrder(int makat, int amount, int dayOfMonth){
        try{
            IF.addOrder(makat,amount,dayOfMonth,false);
            return new Response(null,"Succesful.");

        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
    public Response deleteOrder(int orderID){
        try{
            IF.deleteOrder(orderID);
            return new Response(null,"Succesful.");
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }
    public Response getOrders(){
        try{
            String str = IF.getOrders();
            return new Response(null,str);
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response getDiscounts(){
        try{
            String str = IF.getDiscounts();
            return new Response(null,str);
        }
        catch( Exception e){
            return new Response(e.getMessage(),null);

        }
    }

    public Response deleteAll(){
        try{
            IF.deleteAllFromDB();
            return new Response(null,"");
        }
        catch(Exception e){
            return new Response(e.getMessage(),null);

        }
    }

}
