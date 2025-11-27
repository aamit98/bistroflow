package com.gitProjects.adss_backend.api;

import GlobalClasses.EmployeeToSend;
import GlobalClasses.Role;
import ServiceLayer.HR.WrapperService;
import ServiceLayer.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/hr")
public class HrEmployeeManagementController {

    private final WrapperService wrapperService;

    public HrEmployeeManagementController(
            WrapperService wrapperService
    ) {
        this.wrapperService = wrapperService;
    }

    private boolean isHr(Authentication auth) {
        if (auth == null) {
            System.out.println("[DEBUG] isHr: auth is null");
            return false;
        }
        Object cred = auth.getCredentials();
        System.out.println("[DEBUG] isHr: credentials type = " + (cred != null ? cred.getClass().getName() : "null") + ", value = " + cred);
        if (cred instanceof Boolean b) {
            System.out.println("[DEBUG] isHr: credentials is Boolean = " + b);
            return b;
        }
        System.out.println("[DEBUG] isHr: credentials is not Boolean, returning false");
        return false;
    }

    private Integer currentEmployeeId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Integer i) return i;
        if (principal instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    // HR: get employees in own branch with enriched profile info
    @GetMapping("/branches/{branchId}/employees")
    public ResponseEntity<?> getEmployeesForBranch(
            @PathVariable int branchId,
            Authentication auth
    ) {
        System.out.println("[DEBUG] getEmployeesForBranch called");
        System.out.println("[DEBUG] auth = " + auth);
        System.out.println("[DEBUG] auth != null? " + (auth != null));
        
        if (auth != null) {
            System.out.println("[DEBUG] auth.getCredentials() = " + auth.getCredentials());
            System.out.println("[DEBUG] auth.getPrincipal() = " + auth.getPrincipal());
            System.out.println("[DEBUG] auth.isAuthenticated() = " + auth.isAuthenticated());
            System.out.println("[DEBUG] auth.getAuthorities() = " + auth.getAuthorities());
        }
        
        if (!isHr(auth)) {
            System.out.println("[DEBUG] isHr(auth) returned false, returning 403");
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            managerId = 1; // fallback for legacy service
        }

        Response res = wrapperService.hrManagerService.getAllEmployeesInBranch(managerId, branchId);
        if (res.errorOccurred()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", res.getErrorMsg()));
        }

        // The service returns an array, not a List
        Object returnValue = res.getReturnValue();
        EmployeeToSend[] employeesArray;
        
        if (returnValue instanceof EmployeeToSend[]) {
            employeesArray = (EmployeeToSend[]) returnValue;
        } else if (returnValue instanceof List) {
            @SuppressWarnings("unchecked")
            List<EmployeeToSend> list = (List<EmployeeToSend>) returnValue;
            employeesArray = list.toArray(new EmployeeToSend[0]);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Unexpected return type from service"));
        }

        List<Map<String, Object>> body = Arrays.stream(employeesArray)
                .map(e -> {
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", e.id);
                    dto.put("name", e.name);
                    dto.put("branchId", e.branchId);
                    dto.put("isHRManager", e.isHRManager);
                    dto.put("hourlyRate", e.hourlyRate);
                    dto.put("monthlyRate", e.monthlyRate);
                    dto.put("termsOfEmployment", e.termsOfEmployment);
                    dto.put("bankCode", e.bankCode);
                    dto.put("bankBranchCode", e.bankBranchCode);
                    dto.put("bankAccount", e.bankAccount);
                    dto.put("startDate", e.startDate);
                    dto.put("roles", Arrays.stream(e.roles)
                            .map(Enum::name)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(body);
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

    // HR: add a new employee (including account)
    @PostMapping("/branches/{branchId}/employees")
    public ResponseEntity<?> addEmployee(
            @PathVariable int branchId,
            @RequestBody CreateEmployeeRequest body,
            Authentication auth
    ) {
        if (!isHr(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        Integer managerId = currentEmployeeId(auth);
        if (managerId == null) {
            managerId = 1;
        }

        if (body == null || body.password == null || body.password.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Password is required"));
        }

        if (body.branchId == 0) {
            body.branchId = branchId;
        }

        Role[] rolesArr;
        if (body.roles == null || body.roles.isEmpty()) {
            rolesArr = new Role[0];
        } else {
            rolesArr = body.roles.stream()
                    .map(r -> {
                        try {
                            return Role.valueOf(r.toUpperCase());
                        } catch (IllegalArgumentException ex) {
                            throw new RuntimeException("Unknown role: " + r);
                        }
                    })
                    .toArray(Role[]::new);
        }

        EmployeeToSend toSend = new EmployeeToSend(
                body.id,
                body.branchId,
                body.termsOfEmployment != null ? body.termsOfEmployment : "",
                body.name != null ? body.name : "",
                body.startDate,
                body.bankCode,
                body.bankBranchCode,
                body.bankAccount,
                body.hourlyRate,
                body.monthlyRate,
                rolesArr
        );

        Response res = wrapperService.hrManagerService.addEmployee(managerId, toSend, body.password);
        if (res.errorOccurred()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", res.getErrorMsg()));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Employee created successfully"));
    }
}
