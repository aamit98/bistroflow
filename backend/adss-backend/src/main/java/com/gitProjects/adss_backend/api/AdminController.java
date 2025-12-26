package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.model.RestaurantEntity;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import com.gitProjects.adss_backend.hr.repo.RestaurantRepository;
import com.gitProjects.adss_backend.hr.service.RestaurantService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Super Admin API for managing the entire system:
 * - All restaurant chains across the organization
 * - All HR managers (who each own a restaurant chain)
 * - System-wide statistics
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final BranchRepository branchRepo;
    private final RestaurantRepository restaurantRepo;
    private final EmployeeAccountRepository accountRepo;
    private final RestaurantService restaurantService;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            BranchRepository branchRepo,
            RestaurantRepository restaurantRepo,
            EmployeeAccountRepository accountRepo,
            RestaurantService restaurantService,
            PasswordEncoder passwordEncoder
    ) {
        this.branchRepo = branchRepo;
        this.restaurantRepo = restaurantRepo;
        this.accountRepo = accountRepo;
        this.restaurantService = restaurantService;
        this.passwordEncoder = passwordEncoder;
    }

    // ---- DTOs ----

    public record RestaurantDto(
            Long id,
            String name,
            String businessId,
            String contactEmail,
            String contactPhone,
            boolean active,
            Integer hrManagerId,
            String hrManagerName,
            int branchCount,
            int employeeCount,
            LocalDateTime createdAt
    ) {}

    public record BranchSummaryDto(
            Integer id,
            String name,
            String address,
            String city,
            boolean active,
            Long restaurantId,
            String restaurantName,
            int employeeCount
    ) {}

    public record HrManagerDto(
            Integer id,
            String name,
            Long restaurantId,
            String restaurantName,
            int branchCount
    ) {}

    public record SystemStatsDto(
            int totalRestaurants,
            int activeRestaurants,
            int totalBranches,
            int activeBranches,
            int totalHrManagers,
            int totalEmployees
    ) {}

    public record CreateRestaurantRequest(
            String name,
            String businessId,
            String contactEmail,
            String contactPhone
    ) {}

    public record CreateBranchRequest(
            String name,
            String address,
            String city,
            Long restaurantId
    ) {}

    public record CreateHrManagerRequest(
            Integer id,
            String name,
            String password,
            Long restaurantId  // HR managers are assigned to restaurants, not branches
    ) {}

    public record UpdateRestaurantAssignmentRequest(
            Long restaurantId
    ) {}

    // ---- RESTAURANT ENDPOINTS ----

    /**
     * Get all restaurant chains
     */
    @GetMapping("/restaurants")
    public ResponseEntity<?> getAllRestaurants(Authentication auth) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        List<RestaurantDto> restaurants = restaurantRepo.findAll().stream()
                .map(this::toRestaurantDto)
                .toList();

        return ResponseEntity.ok(restaurants);
    }

    /**
     * Create a new restaurant chain
     */
    @PostMapping("/restaurants")
    public ResponseEntity<?> createRestaurant(
            @RequestBody CreateRestaurantRequest request,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Restaurant name is required"));
        }

        RestaurantEntity restaurant = restaurantService.createRestaurant(request.name(), null);
        if (request.businessId() != null) restaurant.setBusinessId(request.businessId());
        if (request.contactEmail() != null) restaurant.setContactEmail(request.contactEmail());
        if (request.contactPhone() != null) restaurant.setContactPhone(request.contactPhone());
        restaurant = restaurantRepo.save(restaurant);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toRestaurantDto(restaurant));
    }

    /**
     * Get a specific restaurant
     */
    @GetMapping("/restaurants/{id}")
    public ResponseEntity<?> getRestaurant(
            @PathVariable Long id,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return restaurantRepo.findById(id)
                .map(r -> ResponseEntity.ok(toRestaurantDto(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update restaurant details
     */
    @PutMapping("/restaurants/{id}")
    public ResponseEntity<?> updateRestaurant(
            @PathVariable Long id,
            @RequestBody CreateRestaurantRequest request,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return restaurantRepo.findById(id)
                .map(restaurant -> {
                    if (request.name() != null) restaurant.setName(request.name());
                    if (request.businessId() != null) restaurant.setBusinessId(request.businessId());
                    if (request.contactEmail() != null) restaurant.setContactEmail(request.contactEmail());
                    if (request.contactPhone() != null) restaurant.setContactPhone(request.contactPhone());
                    restaurantRepo.save(restaurant);
                    return ResponseEntity.ok(toRestaurantDto(restaurant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deactivate a restaurant (cascades to all branches)
     */
    @PutMapping("/restaurants/{id}/deactivate")
    public ResponseEntity<?> deactivateRestaurant(
            @PathVariable Long id,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return restaurantRepo.findById(id)
                .map(restaurant -> {
                    restaurant.setActive(false);
                    restaurantRepo.save(restaurant);
                    
                    // Cascade: deactivate all branches of this restaurant
                    List<BranchEntity> branches = branchRepo.findByRestaurantId(id);
                    for (BranchEntity branch : branches) {
                        branch.setActive(false);
                        branchRepo.save(branch);
                    }
                    
                    return ResponseEntity.ok(toRestaurantDto(restaurant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Activate a restaurant (optionally re-activates branches if requested)
     */
    @PutMapping("/restaurants/{id}/activate")
    public ResponseEntity<?> activateRestaurant(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean activateBranches,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return restaurantRepo.findById(id)
                .map(restaurant -> {
                    restaurant.setActive(true);
                    restaurantRepo.save(restaurant);
                    
                    // Optionally re-activate all branches
                    if (activateBranches) {
                        List<BranchEntity> branches = branchRepo.findByRestaurantId(id);
                        for (BranchEntity branch : branches) {
                            branch.setActive(true);
                            branchRepo.save(branch);
                        }
                    }
                    
                    return ResponseEntity.ok(toRestaurantDto(restaurant));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- BRANCH ENDPOINTS ----

    /**
     * Get all branches with summary information
     */
    @GetMapping("/branches")
    public ResponseEntity<?> getAllBranches(Authentication auth) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        List<BranchSummaryDto> branches = branchRepo.findAll().stream()
                .map(this::toBranchSummaryDto)
                .toList();

        return ResponseEntity.ok(branches);
    }

    /**
     * Create a new branch under a restaurant
     */
    @PostMapping("/branches")
    public ResponseEntity<?> createBranch(
            @RequestBody CreateBranchRequest request,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Branch name is required"));
        }

        BranchEntity branch = new BranchEntity();
        branch.setName(request.name());
        branch.setAddress(request.address() != null ? request.address() : "");
        branch.setCity(request.city() != null ? request.city() : "");
        branch.setActive(true);

        // Link to restaurant if provided
        if (request.restaurantId() != null) {
            restaurantRepo.findById(request.restaurantId()).ifPresent(branch::setRestaurant);
        }

        BranchEntity saved = branchRepo.save(branch);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toBranchSummaryDto(saved));
    }

    /**
     * Get branches for a specific restaurant
     */
    @GetMapping("/restaurants/{restaurantId}/branches")
    public ResponseEntity<?> getBranchesForRestaurant(
            @PathVariable Long restaurantId,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        List<BranchSummaryDto> branches = branchRepo.findByRestaurantId(restaurantId).stream()
                .map(this::toBranchSummaryDto)
                .toList();

        return ResponseEntity.ok(branches);
    }

    /**
     * Activate a branch
     */
    @PutMapping("/branches/{id}/activate")
    public ResponseEntity<?> activateBranch(
            @PathVariable Integer id,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return branchRepo.findById(id)
                .map(branch -> {
                    branch.setActive(true);
                    branchRepo.save(branch);
                    return ResponseEntity.ok(toBranchSummaryDto(branch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deactivate a branch
     */
    @PutMapping("/branches/{id}/deactivate")
    public ResponseEntity<?> deactivateBranch(
            @PathVariable Integer id,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        return branchRepo.findById(id)
                .map(branch -> {
                    branch.setActive(false);
                    branchRepo.save(branch);
                    return ResponseEntity.ok(toBranchSummaryDto(branch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---- HR MANAGER ENDPOINTS ----

    /**
     * Get all HR managers
     */
    @GetMapping("/hr-managers")
    public ResponseEntity<?> getAllHrManagers(Authentication auth) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        List<HrManagerDto> managers = accountRepo.findAll().stream()
                .filter(a -> a.isHrManager() && !a.isSuperAdmin())
                .map(this::toHrManagerDto)
                .toList();

        return ResponseEntity.ok(managers);
    }

    /**
     * Create a new HR manager and optionally assign to a restaurant
     */
    @PostMapping("/hr-managers")
    public ResponseEntity<?> createHrManager(
            @RequestBody CreateHrManagerRequest request,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        // Validate Israeli ID (9 digits)
        if (request.id() == null || request.id().toString().length() != 9) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee ID must be a 9-digit Israeli ID"));
        }

        // Check if ID already exists
        if (accountRepo.findByEmployeeId(request.id()).isPresent()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee ID already exists"));
        }

        // Validate restaurant exists if provided
        if (request.restaurantId() != null && !restaurantRepo.existsById(request.restaurantId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Restaurant not found"));
        }

        // Create the HR manager account
        EmployeeAccount account = new EmployeeAccount();
        account.setEmployeeId(request.id());
        account.setName(request.name());
        account.setUsername(request.name().toLowerCase().replace(" ", "."));
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setHrManager(true);
        account.setSuperAdmin(false);
        account.setRestaurantId(request.restaurantId()); // HR manages restaurant, not branch
        account.setStartDate(LocalDate.now());
        account.setTermsOfEmployment("FULL_TIME");
        account.setRoles(List.of());

        EmployeeAccount saved = accountRepo.save(account);

        // Update the restaurant's HR manager reference
        if (request.restaurantId() != null) {
            restaurantRepo.findById(request.restaurantId()).ifPresent(restaurant -> {
                restaurant.setHrManagerId(saved.getEmployeeId());
                restaurantRepo.save(restaurant);
            });
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toHrManagerDto(saved));
    }

    /**
     * Update HR manager's restaurant assignment
     */
    @PutMapping("/hr-managers/{id}/restaurant")
    public ResponseEntity<?> updateHrManagerRestaurant(
            @PathVariable Integer id,
            @RequestBody UpdateRestaurantAssignmentRequest request,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        Optional<EmployeeAccount> optAccount = accountRepo.findByEmployeeId(id);
        if (optAccount.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EmployeeAccount account = optAccount.get();
        if (!account.isHrManager()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee is not an HR manager"));
        }

        // Validate restaurant exists
        if (request.restaurantId() != null && !restaurantRepo.existsById(request.restaurantId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Restaurant not found"));
        }

        // Remove from old restaurant
        if (account.getRestaurantId() != null) {
            restaurantRepo.findById(account.getRestaurantId()).ifPresent(oldRestaurant -> {
                if (oldRestaurant.getHrManagerId() != null && 
                    oldRestaurant.getHrManagerId().equals(account.getEmployeeId())) {
                    oldRestaurant.setHrManagerId(null);
                    restaurantRepo.save(oldRestaurant);
                }
            });
        }

        // Assign to new restaurant
        account.setRestaurantId(request.restaurantId());
        accountRepo.save(account);

        if (request.restaurantId() != null) {
            restaurantRepo.findById(request.restaurantId()).ifPresent(restaurant -> {
                restaurant.setHrManagerId(account.getEmployeeId());
                restaurantRepo.save(restaurant);
            });
        }

        return ResponseEntity.ok(toHrManagerDto(account));
    }

    /**
     * Remove HR manager (demote to regular employee)
     */
    @DeleteMapping("/hr-managers/{id}")
    public ResponseEntity<?> removeHrManager(
            @PathVariable Integer id,
            Authentication auth
    ) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        Optional<EmployeeAccount> optAccount = accountRepo.findByEmployeeId(id);
        if (optAccount.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        EmployeeAccount account = optAccount.get();
        if (!account.isHrManager()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Employee is not an HR manager"));
        }

        // Remove from restaurant
        if (account.getRestaurantId() != null) {
            restaurantRepo.findById(account.getRestaurantId()).ifPresent(restaurant -> {
                if (restaurant.getHrManagerId() != null && 
                    restaurant.getHrManagerId().equals(account.getEmployeeId())) {
                    restaurant.setHrManagerId(null);
                    restaurantRepo.save(restaurant);
                }
            });
        }

        // Demote to regular employee
        account.setHrManager(false);
        account.setRestaurantId(null);
        accountRepo.save(account);

        return ResponseEntity.noContent().build();
    }

    // ---- STATS ENDPOINT ----

    /**
     * Get system-wide statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getSystemStats(Authentication auth) {
        if (!isSuperAdmin(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Super admin access required"));
        }

        List<RestaurantEntity> allRestaurants = restaurantRepo.findAll();
        List<BranchEntity> allBranches = branchRepo.findAll();
        List<EmployeeAccount> allAccounts = accountRepo.findAll();

        int totalRestaurants = allRestaurants.size();
        int activeRestaurants = (int) allRestaurants.stream().filter(RestaurantEntity::isActive).count();
        int totalBranches = allBranches.size();
        int activeBranches = (int) allBranches.stream().filter(BranchEntity::isActive).count();
        int totalHrManagers = (int) allAccounts.stream()
                .filter(a -> a.isHrManager() && !a.isSuperAdmin())
                .count();
        int totalEmployees = (int) allAccounts.stream()
                .filter(a -> !a.isHrManager() && !a.isSuperAdmin())
                .count();

        return ResponseEntity.ok(new SystemStatsDto(
                totalRestaurants,
                activeRestaurants,
                totalBranches,
                activeBranches,
                totalHrManagers,
                totalEmployees
        ));
    }

    // ---- Helper methods ----

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
    }

    private RestaurantDto toRestaurantDto(RestaurantEntity restaurant) {
        String hrManagerName = null;
        if (restaurant.getHrManagerId() != null) {
            hrManagerName = accountRepo.findByEmployeeId(restaurant.getHrManagerId())
                    .map(EmployeeAccount::getName)
                    .orElse(null);
        }

        int branchCount = (int) branchRepo.countByRestaurantId(restaurant.getId());
        int employeeCount = (int) restaurantService.getEmployeeCount(restaurant.getId());

        return new RestaurantDto(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getBusinessId(),
                restaurant.getContactEmail(),
                restaurant.getContactPhone(),
                restaurant.isActive(),
                restaurant.getHrManagerId(),
                hrManagerName,
                branchCount,
                employeeCount,
                restaurant.getCreatedAt()
        );
    }

    private BranchSummaryDto toBranchSummaryDto(BranchEntity branch) {
        int employeeCount = (int) accountRepo.findByBranchId(branch.getId()).stream()
                .filter(a -> !a.isHrManager() && !a.isSuperAdmin())
                .count();

        Long restaurantId = branch.getRestaurantId();
        String restaurantName = null;
        if (branch.getRestaurant() != null) {
            restaurantName = branch.getRestaurant().getName();
        }

        return new BranchSummaryDto(
                branch.getId(),
                branch.getName(),
                branch.getAddress(),
                branch.getCity(),
                branch.isActive(),
                restaurantId,
                restaurantName,
                employeeCount
        );
    }

    private HrManagerDto toHrManagerDto(EmployeeAccount account) {
        String restaurantName = null;
        int branchCount = 0;
        
        if (account.getRestaurantId() != null) {
            Optional<RestaurantEntity> restaurant = restaurantRepo.findById(account.getRestaurantId());
            if (restaurant.isPresent()) {
                restaurantName = restaurant.get().getName();
                branchCount = (int) branchRepo.countByRestaurantId(account.getRestaurantId());
            }
        }

        return new HrManagerDto(
                account.getEmployeeId(),
                account.getName(),
                account.getRestaurantId(),
                restaurantName,
                branchCount
        );
    }
}
