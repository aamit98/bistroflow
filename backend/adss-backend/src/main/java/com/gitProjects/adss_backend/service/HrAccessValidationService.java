package com.gitProjects.adss_backend.service;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service to validate HR manager access to branches.
 * 
 * Access model:
 * - Super admins can access ALL branches
 * - HR managers can only access their assigned branch (branchId)
 * - If restaurantId is set, HR managers can access all branches in that restaurant
 */
@Service
public class HrAccessValidationService {

    private final EmployeeAccountRepository accountRepo;
    private final BranchRepository branchRepo;

    public HrAccessValidationService(EmployeeAccountRepository accountRepo, BranchRepository branchRepo) {
        this.accountRepo = accountRepo;
        this.branchRepo = branchRepo;
    }

    /**
     * Extract employee ID from authentication principal
     */
    public Integer getEmployeeId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Integer i) return i;
        if (principal instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /**
     * Check if the authenticated user is an HR manager
     */
    public boolean isHrManager(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_HR_MANAGER".equals(a));
    }

    /**
     * Check if the authenticated user is a super admin
     */
    public boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
    }

    /**
     * Validate that the HR manager has access to the specified branch.
     * Super admins have access to all branches.
     * 
     * @return null if access is allowed, or an error message if access is denied
     */
    public String validateBranchAccess(Authentication auth, int branchId) {
        Integer employeeId = getEmployeeId(auth);
        if (employeeId == null) {
            return "Unauthorized";
        }

        // Super admins can access any branch
        if (isSuperAdmin(auth)) {
            return null;
        }

        // Get the HR manager's account
        Optional<EmployeeAccount> hrAccount = accountRepo.findByEmployeeId(employeeId);
        if (hrAccount.isEmpty()) {
            return "Account not found";
        }

        EmployeeAccount account = hrAccount.get();
        
        // Check if HR manager has restaurantId set (restaurant-level access)
        Long hrRestaurantId = account.getRestaurantId();
        if (hrRestaurantId != null) {
            // Restaurant-level access: check if branch belongs to HR's restaurant
            Optional<BranchEntity> branch = branchRepo.findById(branchId);
            if (branch.isEmpty()) {
                return "Branch not found";
            }
            
            if (branch.get().getRestaurant() != null && 
                hrRestaurantId.equals(branch.get().getRestaurant().getId())) {
                return null; // Access granted - branch belongs to HR's restaurant
            }
        }
        
        // Branch-level access: check if this is the HR manager's assigned branch
        Integer hrBranchId = account.getBranchId();
        // Allow access if branchId matches (treat 0 as unassigned, not a valid branch)
        if (hrBranchId != null && hrBranchId > 0 && hrBranchId.equals(branchId)) {
            return null; // Access granted - this is HR's own branch
        }

        return "You don't have access to this branch";
    }

    /**
     * Check if HR manager can access the branch (returns boolean)
     */
    public boolean canAccessBranch(Authentication auth, int branchId) {
        return validateBranchAccess(auth, branchId) == null;
    }

    /**
     * Get the restaurant ID for an HR manager
     */
    public Long getHrRestaurantId(Authentication auth) {
        Integer employeeId = getEmployeeId(auth);
        if (employeeId == null) return null;
        
        return accountRepo.findByEmployeeId(employeeId)
                .map(EmployeeAccount::getRestaurantId)
                .orElse(null);
    }
    
    /**
     * Get the branch ID for an HR manager (direct branch assignment)
     */
    public Integer getHrBranchId(Authentication auth) {
        Integer employeeId = getEmployeeId(auth);
        if (employeeId == null) return null;
        
        return accountRepo.findByEmployeeId(employeeId)
                .map(EmployeeAccount::getBranchId)
                .orElse(null);
    }
}
