package com.gitProjects.adss_backend.hr.service;

import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.model.RestaurantEntity;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import com.gitProjects.adss_backend.hr.repo.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing restaurant chains.
 * A restaurant chain is owned by an HR Manager and contains multiple branches.
 */
@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepo;
    private final BranchRepository branchRepo;
    private final EmployeeAccountRepository accountRepo;

    public RestaurantService(
            RestaurantRepository restaurantRepo,
            BranchRepository branchRepo,
            EmployeeAccountRepository accountRepo
    ) {
        this.restaurantRepo = restaurantRepo;
        this.branchRepo = branchRepo;
        this.accountRepo = accountRepo;
    }

    /**
     * Create a new restaurant chain
     */
    @Transactional
    public RestaurantEntity createRestaurant(String name, Integer hrManagerId) {
        RestaurantEntity restaurant = new RestaurantEntity(name, hrManagerId);
        RestaurantEntity savedRestaurant = restaurantRepo.save(restaurant);

        // Update the HR manager to link to this restaurant
        if (hrManagerId != null) {
            accountRepo.findByEmployeeId(hrManagerId).ifPresent(account -> {
                account.setRestaurantId(savedRestaurant.getId());
                accountRepo.save(account);
            });
        }

        return savedRestaurant;
    }

    /**
     * Get all restaurants
     */
    public List<RestaurantEntity> getAllRestaurants() {
        return restaurantRepo.findByActiveTrue();
    }

    /**
     * Get restaurant by ID
     */
    public Optional<RestaurantEntity> getRestaurant(Long id) {
        return restaurantRepo.findById(id);
    }

    /**
     * Get restaurant by ID with all branches loaded
     */
    public Optional<RestaurantEntity> getRestaurantWithBranches(Long id) {
        return restaurantRepo.findByIdWithBranches(id);
    }

    /**
     * Get the restaurant managed by a specific HR Manager
     */
    public Optional<RestaurantEntity> getRestaurantByHrManager(Integer hrManagerId) {
        return restaurantRepo.findByHrManagerId(hrManagerId);
    }

    /**
     * Assign an HR Manager to a restaurant
     */
    @Transactional
    public RestaurantEntity assignHrManager(Long restaurantId, Integer hrManagerId) {
        RestaurantEntity restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        // Update restaurant
        restaurant.setHrManagerId(hrManagerId);
        restaurant = restaurantRepo.save(restaurant);

        // Update HR manager's restaurantId
        if (hrManagerId != null) {
            accountRepo.findByEmployeeId(hrManagerId).ifPresent(account -> {
                account.setRestaurantId(restaurantId);
                accountRepo.save(account);
            });
        }

        return restaurant;
    }

    /**
     * Update restaurant details
     */
    @Transactional
    public RestaurantEntity updateRestaurant(Long id, String name, String businessId, 
                                              String contactEmail, String contactPhone) {
        RestaurantEntity restaurant = restaurantRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        if (name != null) restaurant.setName(name);
        if (businessId != null) restaurant.setBusinessId(businessId);
        if (contactEmail != null) restaurant.setContactEmail(contactEmail);
        if (contactPhone != null) restaurant.setContactPhone(contactPhone);

        return restaurantRepo.save(restaurant);
    }

    /**
     * Soft delete (deactivate) a restaurant
     */
    @Transactional
    public void deactivateRestaurant(Long id) {
        restaurantRepo.findById(id).ifPresent(restaurant -> {
            restaurant.setActive(false);
            restaurantRepo.save(restaurant);
        });
    }

    /**
     * Reactivate a restaurant
     */
    @Transactional
    public void activateRestaurant(Long id) {
        restaurantRepo.findById(id).ifPresent(restaurant -> {
            restaurant.setActive(true);
            restaurantRepo.save(restaurant);
        });
    }

    /**
     * Create a new branch under a restaurant
     */
    @Transactional
    public BranchEntity createBranch(Long restaurantId, String name, String address, String city) {
        RestaurantEntity restaurant = restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));

        BranchEntity branch = new BranchEntity(name, city);
        branch.setRestaurant(restaurant);
        branch.setAddress(address);

        return branchRepo.save(branch);
    }

    /**
     * Get all branches for a restaurant
     */
    public List<BranchEntity> getBranchesForRestaurant(Long restaurantId) {
        return branchRepo.findByRestaurantIdAndActiveTrue(restaurantId);
    }

    /**
     * Get count of branches for a restaurant
     */
    public long getBranchCount(Long restaurantId) {
        return branchRepo.countByRestaurantId(restaurantId);
    }

    /**
     * Get count of employees for a restaurant (across all branches)
     */
    public long getEmployeeCount(Long restaurantId) {
        List<BranchEntity> branches = branchRepo.findByRestaurantId(restaurantId);
        return branches.stream()
                .mapToLong(branch -> accountRepo.findByBranchId(branch.getId()).stream()
                        .filter(a -> !a.isHrManager() && !a.isSuperAdmin())
                        .count())
                .sum();
    }

    /**
     * Check if HR manager has access to a specific branch
     */
    public boolean hrManagerHasAccessToBranch(Integer hrManagerId, Integer branchId) {
        Optional<RestaurantEntity> restaurant = restaurantRepo.findByHrManagerId(hrManagerId);
        if (restaurant.isEmpty()) {
            return false;
        }

        return branchRepo.findById(branchId)
                .map(branch -> branch.getRestaurant() != null && 
                               branch.getRestaurant().getId().equals(restaurant.get().getId()))
                .orElse(false);
    }
}
