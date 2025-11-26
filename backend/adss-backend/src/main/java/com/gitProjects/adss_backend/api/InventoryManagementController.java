package com.gitProjects.adss_backend.api;

import ServiceLayer.Inventory.InventoryService;
import ServiceLayer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/legacy/inventory")
public class InventoryManagementController {

    private final InventoryService inventoryService;

    public InventoryManagementController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    // ========== PRODUCTS ==========

    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody AddProductRequest body) {
        Response res = inventoryService.addProduct(
                body.makat,
                body.minimalAmount,
                body.name,
                body.place,
                body.manufacturer,
                body.costPrice,
                body.currentPrice,
                body.categories,
                body.popularity
        );

        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Product added");
    }

    @DeleteMapping("/products/{makat}")
    public ResponseEntity<?> removeProduct(@PathVariable int makat,
                                           @RequestBody(required = false) RemoveProductRequest body) {
        String[] categories = (body != null && body.categories != null) ? body.categories : new String[0];
        Response res = inventoryService.removeProduct(makat, categories);

        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Product removed");
    }

    // ========== ITEMS ==========

    @GetMapping("/products/{makat}/items")
    public ResponseEntity<?> getItems(@PathVariable int makat) {
        Response res = inventoryService.getAllItems(makat);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ResponseEntity.ok(res.getReturnValue()); // will be List<ItemDTO> or similar
    }

    @PostMapping("/products/{makat}/items")
    public ResponseEntity<?> addItem(@PathVariable int makat,
                                     @RequestBody AddItemRequest body) {
        Response res = inventoryService.addItem(
                makat,
                body.sn,
                new Date(body.expiredAtMillis),
                body.inStorage
        );
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Item added");
    }

    @DeleteMapping("/products/{makat}/items/{sn}")
    public ResponseEntity<?> removeItem(@PathVariable int makat,
                                        @PathVariable int sn) {
        Response res = inventoryService.removeItem(makat, sn);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Item removed");
    }

    @PostMapping("/products/{makat}/items/move-to-shelf")
    public ResponseEntity<?> moveToShelf(@PathVariable int makat,
                                         @RequestBody MoveItemsRequest body) {
        Response res = inventoryService.moveItemToShelf(makat, body.amount);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Moved items to shelf");
    }

    @PostMapping("/products/{makat}/items/move-to-storage")
    public ResponseEntity<?> moveToStorage(@PathVariable int makat,
                                           @RequestBody MoveItemsRequest body) {
        Response res = inventoryService.moveItemToStorage(makat, body.amount);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Moved items to storage");
    }

    @PostMapping("/products/{makat}/defective")
    public ResponseEntity<?> reportDefective(@PathVariable int makat,
                                             @RequestBody DefectiveRequest body) {
        Response res = inventoryService.reportDefective(makat, body.amount);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Defective items reported");
    }

    // ========== DISCOUNTS ==========

    @GetMapping("/discounts")
    public ResponseEntity<?> getDiscounts() {
        Response res = inventoryService.getDiscounts();
        if (res.errorOccurred()) {
            return error(res);
        }
        return ResponseEntity.ok(res.getReturnValue());
    }

    @PostMapping("/discounts/supplier")
    public ResponseEntity<?> addSupplierDiscount(@RequestBody DiscountRequest body) {
        Response res = inventoryService.addDiscountFromSupplier(body.prodMakats, body.catNames, body.percentage);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Supplier discount added");
    }

    @PostMapping("/discounts/store")
    public ResponseEntity<?> addStoreDiscount(@RequestBody DiscountRequest body) {
        Response res = inventoryService.addDiscountFromStore(body.prodMakats, body.catNames, body.percentage);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Store discount added");
    }

    @DeleteMapping("/discounts/supplier/{id}")
    public ResponseEntity<?> removeSupplierDiscount(@PathVariable int id) {
        Response res = inventoryService.removeDiscountFromSupplier(id);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Supplier discount removed");
    }

    @DeleteMapping("/discounts/store/{id}")
    public ResponseEntity<?> removeStoreDiscount(@PathVariable int id) {
        Response res = inventoryService.removeDiscountFromStore(id);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Store discount removed");
    }

    // ========== ORDERS ==========

    @GetMapping("/orders")
    public ResponseEntity<?> getOrders() {
        Response res = inventoryService.getOrders();
        if (res.errorOccurred()) {
            return error(res);
        }
        return ResponseEntity.ok(res.getReturnValue());
    }

    @PostMapping("/orders")
    public ResponseEntity<?> addOrder(@RequestBody AddOrderRequest body) {
        Response res = inventoryService.addOrder(body.makat, body.amount, body.dayOfMonth);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Order added");
    }

    @PutMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable int orderId,
                                         @RequestBody UpdateOrderRequest body) {
        Response res = inventoryService.updateOrder(orderId, body.newAmount);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Order updated");
    }

    @DeleteMapping("/orders/{orderId}")
    public ResponseEntity<?> deleteOrder(@PathVariable int orderId) {
        Response res = inventoryService.deleteOrder(orderId);
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Order deleted");
    }

    // ========== REPORTS ==========

    @PostMapping("/reports")
    public ResponseEntity<?> makeReport(@RequestBody ReportRequest body) {
        Response res = inventoryService.makeReport(body.catNames, body.prodMakats);
        if (res.errorOccurred()) {
            return error(res);
        }
        // makeReport probably returns a string or Report; either way front can display it
        return ResponseEntity.ok(res.getReturnValue());
    }

    // ========== ADMIN / RESET ==========

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAll() {
        Response res = inventoryService.deleteAll();
        if (res.errorOccurred()) {
            return error(res);
        }
        return ok("Inventory wiped");
    }

    // ========== DTOs ==========

    public static class AddProductRequest {
        public int makat;
        public int minimalAmount;
        public String name;
        public String place;
        public String manufacturer;
        public double costPrice;
        public double currentPrice;
        public String[] categories;
        public int popularity; // 1=LOW, 2=MEDIUM, 3=HIGH
    }

    public static class RemoveProductRequest {
        public String[] categories;
    }

    public static class AddItemRequest {
        public int sn;
        public long expiredAtMillis;
        public boolean inStorage;
    }

    public static class MoveItemsRequest {
        public int amount;
    }

    public static class DefectiveRequest {
        public int amount;
    }

    public static class DiscountRequest {
        public List<Integer> prodMakats;
        public List<String[]> catNames;
        public int percentage;
    }

    public static class AddOrderRequest {
        public int makat;
        public int amount;
        public int dayOfMonth;
    }

    public static class UpdateOrderRequest {
        public int newAmount;
    }

    public static class ReportRequest {
        public List<String[]> catNames;
        public List<Integer> prodMakats;
    }

    public static class ErrorResponse {
        public String error;
        public ErrorResponse(String error) { this.error = error; }
    }

    public static class MessageResponse {
        public String message;
        public MessageResponse(String message) { this.message = message; }
    }

    // ========== helpers ==========

    private ResponseEntity<ErrorResponse> error(Response res) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(res.getErrorMsg()));
    }

    private ResponseEntity<MessageResponse> ok(String msg) {
        return ResponseEntity.ok(new MessageResponse(msg));
    }
}
