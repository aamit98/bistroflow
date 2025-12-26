package com.gitProjects.adss_backend.inventory.api;

import com.gitProjects.adss_backend.inventory.model.*;
import com.gitProjects.adss_backend.inventory.repo.*;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final ProductRepository productRepo;
    private final BranchStockRepository stockRepo;
    private final InventoryOrderRepository orderRepo;
    private final DiscountRepository discountRepo;
    private final HrAccessValidationService accessValidation;

    public InventoryController(
            ProductRepository productRepo,
            BranchStockRepository stockRepo,
            InventoryOrderRepository orderRepo,
            DiscountRepository discountRepo,
            HrAccessValidationService accessValidation
    ) {
        this.productRepo = productRepo;
        this.stockRepo = stockRepo;
        this.orderRepo = orderRepo;
        this.discountRepo = discountRepo;
        this.accessValidation = accessValidation;
    }

    // ---------- Auth Helper Methods ----------

    private boolean isHrManager(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_HR_MANAGER".equals(a));
    }

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
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

    public record UpdateStockDto(
            int quantityOnHand,
            int reorderThreshold
    ) {}

    public record AddStockDto(
            Long productId,
            int quantityOnHand,
            int reorderThreshold
    ) {}

    // ---------- PRODUCTS ----------

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(Authentication auth) {
        // Products are a global catalog - HR managers can view
        if (!isHrManager(auth) && !isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }
        
        List<ProductDto> products = productRepo.findAll().stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getSku(),
                        p.getName(),
                        p.getCategory(),
                        p.getUnit()
                ))
                .toList();
        return ResponseEntity.ok(products);
    }

        @PostMapping("/products")
        public ResponseEntity<?> createProduct(@RequestBody ProductDto body, Authentication auth) {
                // Allow HR managers and super admins to expand the catalog
                if (!isSuperAdmin(auth) && !isHrManager(auth)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(Map.of("error", "HR manager or super admin access required"));
                }

                if (body == null || body.sku() == null || body.sku().isBlank()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "SKU is required"));
                }
                if (body.name() == null || body.name().isBlank()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Product name is required"));
                }
                if (body.category() == null || body.category().isBlank()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Category is required"));
                }
                if (body.unit() == null || body.unit().isBlank()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("error", "Unit is required"));
                }

                String normalizedSku = body.sku().trim();
                String normalizedName = body.name().trim();
                String normalizedCategory = body.category().trim();
                String normalizedUnit = body.unit().trim();

                if (normalizedSku.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "SKU cannot be blank"));
                }
                if (normalizedName.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Product name cannot be blank"));
                }
                if (normalizedCategory.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Category cannot be blank"));
                }
                if (normalizedUnit.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("error", "Unit cannot be blank"));
                }

                if (productRepo.findBySku(normalizedSku).isPresent()) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                        .body(Map.of("error", "A product with this SKU already exists"));
                }

                ProductEntity p = new ProductEntity();
                p.setSku(normalizedSku);
                p.setName(normalizedName);
                p.setCategory(normalizedCategory);
                p.setUnit(normalizedUnit);

                ProductEntity saved = productRepo.save(p);

                ProductDto dto = new ProductDto(
                                saved.getId(),
                                saved.getSku(),
                                saved.getName(),
                                saved.getCategory(),
                                saved.getUnit()
                );
                return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }

    // ---------- STOCK BY BRANCH ----------

    @GetMapping("/branches/{branchId}/stock")
    public ResponseEntity<?> getBranchStock(@PathVariable int branchId, Authentication auth) {
        if (!isHrManager(auth) && !isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }
        
        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }
        
        List<StockDto> stock = stockRepo.findByBranchId(branchId).stream()
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
        return ResponseEntity.ok(stock);
    }

    @PutMapping("/stock/{stockId}")
    public ResponseEntity<?> updateStock(
            @PathVariable Long stockId,
            @RequestBody UpdateStockDto body,
            Authentication auth
    ) {
        if (!isHrManager(auth) && !isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }
        
        return stockRepo.findById(stockId)
                .map(stock -> {
                    // Validate HR manager has access to this stock's branch
                    String accessError = accessValidation.validateBranchAccess(auth, stock.getBranchId());
                    if (accessError != null) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body((Object) Map.of("error", accessError));
                    }
                    
                    stock.setQuantityOnHand(body.quantityOnHand());
                    stock.setReorderThreshold(body.reorderThreshold());
                    BranchStockEntity saved = stockRepo.save(stock);
                    
                    return ResponseEntity.ok((Object) new StockDto(
                            saved.getId(),
                            saved.getBranchId(),
                            new ProductDto(
                                    saved.getProduct().getId(),
                                    saved.getProduct().getSku(),
                                    saved.getProduct().getName(),
                                    saved.getProduct().getCategory(),
                                    saved.getProduct().getUnit()
                            ),
                            saved.getQuantityOnHand(),
                            saved.getReorderThreshold()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/branches/{branchId}/stock")
    public ResponseEntity<?> addStockItem(
            @PathVariable int branchId,
            @RequestBody AddStockDto body,
            Authentication auth
    ) {
        if (!isHrManager(auth) && !isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }
        
        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }
        
        return productRepo.findById(body.productId())
                .map(product -> {
                    BranchStockEntity stock = new BranchStockEntity();
                    stock.setBranchId(branchId);
                    stock.setProduct(product);
                    stock.setQuantityOnHand(body.quantityOnHand());
                    stock.setReorderThreshold(body.reorderThreshold());
                    
                    BranchStockEntity saved = stockRepo.save(stock);
                    
                    return ResponseEntity.ok((Object) new StockDto(
                            saved.getId(),
                            saved.getBranchId(),
                            new ProductDto(
                                    saved.getProduct().getId(),
                                    saved.getProduct().getSku(),
                                    saved.getProduct().getName(),
                                    saved.getProduct().getCategory(),
                                    saved.getProduct().getUnit()
                            ),
                            saved.getQuantityOnHand(),
                            saved.getReorderThreshold()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
