package com.gitProjects.adss_backend.api;

import GlobalClasses.EmployeeToSend;
import ServiceLayer.HR.WrapperService;
import ServiceLayer.Response;
import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final WrapperService wrapperService;
    private final JwtService jwtService;
    private final EmployeeAccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            WrapperService wrapperService,
            JwtService jwtService,
            EmployeeAccountRepository accountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.wrapperService = wrapperService;
        this.jwtService = jwtService;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Endpoints ----------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        System.out.println("[AuthController] Login attempt for employeeId: " + request.employeeId);

        // 1) Find account in H2 by employeeId
        Optional<EmployeeAccount> maybeAccount =
                accountRepository.findByEmployeeId(request.employeeId);

        if (maybeAccount.isEmpty()) {
            System.out.println("[AuthController] Account not found for employeeId: " + request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        EmployeeAccount account = maybeAccount.get();
        System.out.println("[AuthController] Account found, isHrManager: " + account.isHrManager());

        // 2) Check BCrypt password
        if (!passwordEncoder.matches(request.password, account.getPasswordHash())) {
            System.out.println("[AuthController] Password mismatch for employeeId: " + request.employeeId);
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("Invalid credentials"));
        }

        int employeeId = account.getEmployeeId();

        // 3) Fetch employee details from legacy HR layer
        Response empRes = wrapperService.employeeService.getEmployee(employeeId);
        if (empRes.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(empRes.getErrorMsg()));
        }

        EmployeeToSend employee = (EmployeeToSend) empRes.getReturnValue();

        // 4) Build claims for JWT (from account + employee)
        List<String> roleNames = account.getRoles() != null
                ? account.getRoles()
                : Collections.emptyList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("hrManager", account.isHrManager());
        claims.put("branchId", account.getBranchId());
        claims.put("roles", roleNames);

        String token = jwtService.generateToken(
                String.valueOf(employee.id),
                claims
        );

        System.out.println("[AuthController] Token generated for employeeId: " + employeeId + ", isHrManager: " + account.isHrManager());

        // 5) Build response DTO for frontend
        LoginEmployeeDto dto = new LoginEmployeeDto(
                employee.id,
                employee.name,
                employee.branchId,
                employee.isHRManager,
                roleNames, // List<String>
                employee.hourlyRate,
                employee.monthlyRate,
                employee.termsOfEmployment,
                employee.bankCode,
                employee.bankBranchCode,
                employee.bankAccount,
                String.valueOf(employee.startDate)
        );

        LoginResponse response = new LoginResponse(token, dto);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequest request) {
        Response res = wrapperService.employeeService.logout(request.employeeId);

        if (res.errorOccurred()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorResponse(res.getErrorMsg()));
        }

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
