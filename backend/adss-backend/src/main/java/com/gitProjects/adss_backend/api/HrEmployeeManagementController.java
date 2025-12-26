package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.api.dto.EmployeeSummaryDto;
import com.gitProjects.adss_backend.api.dto.PagedResponse;
import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.repo.BranchRoleRepository;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import com.gitProjects.adss_backend.service.HrEmployeeManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/hr")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrEmployeeManagementController {
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRoleRepository branchRoleRepository;
    private final HrAccessValidationService accessValidation;
    private final HrEmployeeManagementService employeeService;
    private static final Logger log = LoggerFactory.getLogger(HrEmployeeManagementController.class);

    // Israeli minimum wage: 3350 agorot = ₪33.50/hr
    private static final int MIN_WAGE_AGOROT = 3350;

    public HrEmployeeManagementController(
            EmployeeAccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            BranchRoleRepository branchRoleRepository,
            HrAccessValidationService accessValidation,
            HrEmployeeManagementService employeeService
    ) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.branchRoleRepository = branchRoleRepository;
        this.accessValidation = accessValidation;
        this.employeeService = employeeService;
    }

    private Integer currentEmployeeId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Integer i) return i;
        if (principal instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @GetMapping("/branches/{branchId}/employees")
    public ResponseEntity<?> getEmployeesForBranch(
            @PathVariable int branchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Authentication auth
    ) {
        log.debug("getEmployeesForBranch called for branch {} by {}", branchId, auth != null ? auth.getName() : "anonymous");

        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        PagedResponse<EmployeeSummaryDto> response = employeeService
                .getEmployeesForBranch(branchId, page, size);

        return ResponseEntity.ok(response);
    }

    public static class CreateEmployeeRequest {
        public int id;
        public int branchId;
        public String name;
        public String termsOfEmployment;
        public int startDate; // yyyymmdd
        public int bankCode;
        public int bankBranchCode;
        public int bankAccount;
        public int hourlyRate;
        public int monthlyRate;
        public List<String> roles;
        public String password;
    }

    @PostMapping("/branches/{branchId}/employees")
    public ResponseEntity<?> addEmployee(
            @PathVariable int branchId,
            @RequestBody CreateEmployeeRequest body,
            Authentication auth
    ) {
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        if (body.password == null || body.password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password is required"));
        }

        if (body.branchId != 0 && body.branchId != branchId) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Branch mismatch between path and payload"));
        }
        body.branchId = branchId;

        if (body.id <= 0 || String.valueOf(body.id).length() > 9) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid Israeli ID (must be 1-9 digits)"));
        }

        if (body.hourlyRate > 0 && body.hourlyRate < MIN_WAGE_AGOROT) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Hourly rate must be at least " + MIN_WAGE_AGOROT + " agorot (₪33.50 minimum wage)"));
        }

        if (body.roles == null || body.roles.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Employee must have at least one role assigned"));
        }

        List<String> invalidRoles = new ArrayList<>();
        for (String roleCode : body.roles) {
            boolean exists = branchRoleRepository.findByBranchIdAndCode(branchId, roleCode)
                    .map(role -> role.isActive())
                    .orElse(false);
            if (!exists) {
                invalidRoles.add(roleCode);
            }
        }
        if (!invalidRoles.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Invalid or inactive role codes: " + String.join(", ", invalidRoles)));
        }

        Optional<EmployeeAccount> existingAccountOpt = accountRepository.findByEmployeeId(body.id);
        EmployeeAccount account;
        boolean reactivated = false;

        if (existingAccountOpt.isPresent()) {
            EmployeeAccount existingAccount = existingAccountOpt.get();
            if (existingAccount.isActive()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Employee with this ID already exists"));
            }
            account = existingAccount;
            reactivated = true;
        } else {
            account = new EmployeeAccount();
            account.setEmployeeId(body.id);
        }

        String username = (body.name != null && !body.name.isBlank())
                ? body.name
                : "employee" + body.id;
        account.setUsername(username);
        account.setName(body.name);
        account.setPasswordHash(passwordEncoder.encode(body.password));
        account.setHrManager(false);
        account.setBranchId(body.branchId);
        account.setActive(true);
        account.setRoles(body.roles != null ? new ArrayList<>(body.roles) : new ArrayList<>());

        account.setHourlyRate(body.hourlyRate);
        account.setMonthlyRate(body.monthlyRate);
        account.setTermsOfEmployment(body.termsOfEmployment);
        account.setBankCode(body.bankCode);
        account.setBankBranchCode(body.bankBranchCode);
        account.setBankAccount(body.bankAccount);

        if (body.startDate > 0) {
            String dateStr = String.valueOf(body.startDate);
            if (dateStr.length() == 8) {
                try {
                    int year = Integer.parseInt(dateStr.substring(0, 4));
                    int month = Integer.parseInt(dateStr.substring(4, 6));
                    int day = Integer.parseInt(dateStr.substring(6, 8));
                    account.setStartDate(java.time.LocalDate.of(year, month, day));
                } catch (Exception e) {
                    log.warn("Invalid start date {} provided for employee {}", dateStr, body.id);
                }
            }
        }

        accountRepository.save(account);
        if (reactivated) {
            log.info("Reactivated employee {} in branch {}", body.id, body.branchId);
            return ResponseEntity.ok(Map.of("message", "Employee reactivated successfully"));
        }

        log.info("Created employee {} in branch {}", body.id, body.branchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Employee created successfully"));
    }

    @DeleteMapping("/branches/{branchId}/employees/{employeeId}")
    public ResponseEntity<?> deleteEmployee(
            @PathVariable int branchId,
            @PathVariable int employeeId,
            Authentication auth
    ) {
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        Optional<EmployeeAccount> maybeAccount = accountRepository.findByEmployeeId(employeeId);
        if (maybeAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found"));
        }

        EmployeeAccount account = maybeAccount.get();

        if (account.getBranchId() != null && !Objects.equals(account.getBranchId(), branchId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Employee does not belong to this branch"));
        }

        Integer currentId = currentEmployeeId(auth);
        if (currentId != null && currentId.equals(employeeId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Cannot delete your own account"));
        }

        if (account.isHrManager()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot delete an HR manager account"));
        }

        account.setActive(false);
        accountRepository.save(account);
        log.info("Employee {} deactivated by {}", employeeId, auth != null ? auth.getName() : "unknown");

        return ResponseEntity.ok(Map.of("message", "Employee deactivated successfully"));
    }
}
