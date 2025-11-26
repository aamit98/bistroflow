package com.gitProjects.adss_backend.api;

import DataAccessLayer.Inventory.ControllerClasses.ProductController;
import DataAccessLayer.Inventory.DTOClasses.ProductDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class InventoryApiController {

    @GetMapping("/products")
    public ResponseEntity<?> getAllProducts() {
        try {
            // Legacy DAL controller
            ProductController pc = new ProductController();
            List<ProductDTO> dtos = pc.selectAllProductsDTOs();

            List<ProductResponse> result = new ArrayList<>();
            for (ProductDTO dto : dtos) {
                result.add(ProductResponse.fromDto(dto));
            }

            return ResponseEntity.ok(result);
        } catch (ClassNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("JDBC driver not found: " + e.getMessage()));
        } catch (SQLException e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("DB error: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Unexpected error: " + e.getMessage()));
        }
    }

    // ---------- DTOs ----------

    public static class ProductResponse {
        public int makat;
        public String name;
        public String place;
        public String manufacturer;
        public double costPrice;
        public double currentPrice;
        public List<String> categories;
        public Integer popularity;
        public double supplierPercentage;
        public double storePercentage;
        public int minimalAmount;
        public List<Integer> items;

        public static ProductResponse fromDto(ProductDTO dto) {
            ProductResponse p = new ProductResponse();
            p.makat = dto.getMakat();
            p.name = dto.getName();
            p.place = dto.getPlace();
            p.manufacturer = dto.getManufacturer();
            p.costPrice = dto.getCostPrice();
            p.currentPrice = dto.getCurrentPrice();
            p.categories = dto.getCategories() != null
                    ? Arrays.asList(dto.getCategories())
                    : List.of();
            p.popularity = dto.getPopularity();
            p.supplierPercentage = dto.getSupplierPercentage();
            p.storePercentage = dto.getStorePercentage();
            p.minimalAmount = dto.getMinimalAmount();
            p.items = dto.getItems(); // this is a List<Integer> in ProductDTO
            return p;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}
