package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.service.HrAccessValidationService;
import com.gitProjects.adss_backend.service.HrDashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/hr/branches")
@PreAuthorize("hasRole('HR_MANAGER')")
public class HrDashboardController {
    private static final Logger log = LoggerFactory.getLogger(HrDashboardController.class);

    private final HrDashboardService dashboardService;
    private final HrAccessValidationService accessValidation;

    public HrDashboardController(
            HrDashboardService dashboardService,
            HrAccessValidationService accessValidation
    ) {
        this.dashboardService = dashboardService;
        this.accessValidation = accessValidation;
    }

    @GetMapping("/{branchId}/dashboard")
    public ResponseEntity<?> getBranchDashboard(
            @PathVariable int branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Authentication auth
    ) {
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        try {
            HrDashboardService.BranchDashboardView view = dashboardService.getBranchDashboard(branchId, weekStart);
            return ResponseEntity.ok(view);
        } catch (HrDashboardService.HrDashboardException ex) {
            log.debug("Dashboard request rejected: {}", ex.getMessage());
            return ResponseEntity.status(ex.getStatus())
                    .body(Map.of("error", ex.getMessage()));
        }
    }
}
