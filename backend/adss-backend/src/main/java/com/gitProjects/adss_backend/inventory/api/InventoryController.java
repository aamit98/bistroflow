package com.gitProjects.adss_backend.inventory.api;

import com.gitProjects.adss_backend.inventory.model.*;
import com.gitProjects.adss_backend.inventory.repo.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ProductRepository productRepo;
    private final BranchStockRepository stockRepo;
    private final InventoryOrderRepository orderRepo;
    private final DiscountRepository discountRepo;

    public InventoryController(
            ProductRepository productRepo,
            BranchStockRepository stockRepo,
            InventoryOrderRepository orderRepo,
            DiscountRepository discountRepo
    ) {
        this.productRepo = productRepo;
        this.stockRepo = stockRepo;
        this.orderRepo = orderRepo;
        this.discountRepo = discountRepo;
    }

    // ---------- DTOs ----------

    public record ProductDto(
            Long id,
            String sku,
            String name,
            String category,
            String unit
    ) {}

    public record StockDto(
            Long id,
            int branchId,
            ProductDto product,
            int quantityOnHand,
            int reorderThreshold
    ) {}

    // ---------- PRODUCTS ----------

    @GetMapping("/products")
    public List<ProductDto> getProducts() {
        return productRepo.findAll().stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getCategory(),
                        p.getUnit()
                ))
                .toList();
    }

    @PostMapping("/products")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto body) {
        ProductEntity p = new ProductEntity();
        p.setSku(body.sku());
        p.setName(body.name());
        p.setCategory(body.category());
        p.setUnit(body.unit());

        ProductEntity saved = productRepo.save(p);

        ProductDto dto = new ProductDto(
                saved.getId(),
                saved.getSku(),
                saved.getName(),
                saved.getCategory(),
                saved.getUnit()
        );
        return ResponseEntity.ok(dto);
    }

    // ---------- STOCK BY BRANCH ----------

    @GetMapping("/branches/{branchId}/stock")
    public List<StockDto> getBranchStock(@PathVariable int branchId) {
        return stockRepo.findByBranchId(branchId).stream()
                .map(s -> new StockDto(
                        s.getId(),
                        s.getBranchId(),
                        new ProductDto(
                                s.getProduct().getId(),
                                s.getProduct().getSku(),
                                s.getProduct().getName(),
                                s.getProduct().getCategory(),
                                s.getProduct().getUnit()
                        ),
                        s.getQuantityOnHand(),
                        s.getReorderThreshold()
                ))
                .toList();
    }
}
