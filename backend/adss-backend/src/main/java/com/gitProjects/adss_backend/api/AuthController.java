package com.gitProjects.adss_backend.api;
import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.security.JwtService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    public AuthController(
            JwtService jwtService,
            EmployeeAccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Endpoints ----------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Validate request
        if (request.employeeId <= 0) {
            log.warn("Login failed: invalid employeeId {}", request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Employee ID is required and must be a valid positive number"));
        }
        
        if (request.password == null || request.password.isBlank()) {
            log.warn("Login failed: password is missing");
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse("Password is required"));
        }
        
        log.info("Login attempt for employeeId {}", request.employeeId);

        // 1) Find account by employeeId
        Optional<EmployeeAccount> maybeAccount =
                accountRepository.findByEmployeeId(request.employeeId);

        if (maybeAccount.isEmpty()) {
            log.warn("Login failed for {}: account not found", request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        EmployeeAccount account = maybeAccount.get();

        // 2a) Check if account is active
        if (!account.isActive()) {
            log.warn("Login failed for {}: account deactivated", request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Account has been deactivated"));
        }

        // 2b) Verify password
        if (!passwordEncoder.matches(request.password, account.getPasswordHash())) {
            log.warn("Login failed for {}: invalid credentials", request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        // 3) Build JWT claims from Account
        List<String> roleNames = account.getRoles() != null
                ? account.getRoles()
                : Collections.emptyList();

        Map<String, Object> claims = new HashMap<>();
        boolean hrManagerFlag = account.isHrManager();
        boolean superAdminFlag = account.isSuperAdmin();
        Integer branchId = account.getBranchId();

        claims.put("hrManager", hrManagerFlag);
        claims.put("superAdmin", superAdminFlag);
        claims.put("branchId", branchId);
        claims.put("roles", roleNames);
        claims.put("employeeId", account.getEmployeeId());

        String subject = account.getUsername() != null
                ? account.getUsername()
                : String.valueOf(account.getEmployeeId());

        String token = jwtService.generateToken(subject, claims);

        // DTO for frontend - fill what we have from Account, rest is 0/empty
        LoginEmployeeDto dto = new LoginEmployeeDto(
                account.getEmployeeId(),
                account.getUsername(),
                branchId != null ? branchId : 0,
                hrManagerFlag,
                superAdminFlag,
                roleNames,
                0.0,        // hourlyRate
                0.0,        // monthlyRate
                "",         // termsOfEmployment
                0, 0, 0,    // bankCode, bankBranchCode, bankAccount
                ""          // startDate
        );

        LoginResponse response = new LoginResponse(token, dto);
        log.info("Login succeeded for employee {} (HR={}, SuperAdmin={})", account.getEmployeeId(), hrManagerFlag, superAdminFlag);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        // Future: if you want JWT blacklist, add it here
        return ResponseEntity.ok(new MessageResponse("Logout successful"));
    }

    /**
     * Debug endpoint: check current auth state without requiring auth
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAuth(org.springframework.security.core.Authentication auth) {
        if (auth == null) {
            return ResponseEntity.ok(Map.of(
                "authenticated", false,
                "message", "No authentication in context"
            ));
        }

        Object principal = auth.getPrincipal();
        Object credentials = auth.getCredentials();

        return ResponseEntity.ok(Map.of(
            "authenticated", auth.isAuthenticated(),
            "principal", principal,
            "credentials", credentials,
            "authorities", auth.getAuthorities()
        ));
    }

    // ---------- DTOs ----------

    public static class LoginRequest {
        public int employeeId;
        public String password;

        public LoginRequest() {
        }
    }

    public static class LoginEmployeeDto {
        public int id;
        public String name;
        public int branchId;
        public boolean isHRManager;
        public boolean isSuperAdmin;
        public List<String> roles;
        public double hourlyRate;
        public double monthlyRate;
        public String termsOfEmployment;
        public int bankCode;
        public int bankBranchCode;
        public int bankAccount;
        public String startDate;

        public LoginEmployeeDto(
                int id,
                String name,
                int branchId,
                boolean isHRManager,
                boolean isSuperAdmin,
                List<String> roles,
                double hourlyRate,
                double monthlyRate,
                String termsOfEmployment,
                int bankCode,
                int bankBranchCode,
                int bankAccount,
                String startDate
        ) {
            this.id = id;
            this.name = name;
            this.branchId = branchId;
            this.isHRManager = isHRManager;
            this.isSuperAdmin = isSuperAdmin;
            this.roles = roles;
            this.hourlyRate = hourlyRate;
            this.monthlyRate = monthlyRate;
            this.termsOfEmployment = termsOfEmployment;
            this.bankCode = bankCode;
            this.bankBranchCode = bankBranchCode;
            this.bankAccount = bankAccount;
            this.startDate = startDate;
        }
    }

    public static class LoginResponse {
        public String token;
        public LoginEmployeeDto employee;

        public LoginResponse(String token, LoginEmployeeDto employee) {
            this.token = token;
            this.employee = employee;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class MessageResponse {
        public String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    public static class LogoutRequest {
        public int employeeId;

        public LogoutRequest() {
        }
    }
}