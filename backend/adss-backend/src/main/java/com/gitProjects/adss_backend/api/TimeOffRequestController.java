package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.api.dto.PagedResponse;
import com.gitProjects.adss_backend.api.dto.TimeOffDecisionResultDto;
import com.gitProjects.adss_backend.api.dto.TimeOffRequestDto;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import com.gitProjects.adss_backend.service.TimeOffRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TimeOffRequestController {

    private static final Logger log = LoggerFactory.getLogger(TimeOffRequestController.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final TimeOffRequestService timeOffService;
    private final HrAccessValidationService accessValidation;

    public TimeOffRequestController(
            TimeOffRequestService timeOffService,
            HrAccessValidationService accessValidation
    ) {
        this.timeOffService = timeOffService;
        this.accessValidation = accessValidation;
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

    private boolean isHrManager(Authentication auth) {
        if (auth == null) {
            log.debug("TimeOff isHrManager check failed: authentication is null");
            return false;
        }

        boolean hasHrRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_HR_MANAGER".equals(a));

        log.debug("TimeOff isHrManager check for {} -> {}", auth.getName(), hasHrRole);

        return hasHrRole;
    }

    private ResponseEntity<Map<String, String>> handleServiceError(TimeOffRequestService.TimeOffRequestException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getMessage()));
    }

    private Pageable buildPageable(int page, int size, Sort sort) {
        int sanitizedPage = Math.max(page, 0);
        int sanitizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(sanitizedPage, sanitizedSize, sort);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    @PostMapping("/employees/{employeeId}/time-off-requests")
    public ResponseEntity<?> createTimeOffRequest(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam ShiftEnums.ShiftType shiftType,
            @Valid @RequestBody CreateTimeOffRequest body,
            Authentication auth
    ) {
        Integer currentId = currentEmployeeId(auth);
        boolean hr = isHrManager(auth);

        if (currentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (!hr && currentId != employeeId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only create requests for yourself"));
        }

        try {
            TimeOffRequestDto dto = timeOffService.createTimeOffRequest(employeeId, date, shiftType, body.reason());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (TimeOffRequestService.TimeOffRequestException ex) {
            return handleServiceError(ex);
        }
    }

    @GetMapping("/employees/{employeeId}/time-off-requests")
    public ResponseEntity<?> getEmployeeTimeOffRequests(
            @PathVariable int employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth
    ) {
        Integer currentId = currentEmployeeId(auth);
        boolean hr = isHrManager(auth);

        if (currentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        if (!hr && currentId != employeeId) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You can only view your own requests"));
        }

        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<TimeOffRequestDto> requests = timeOffService.getEmployeeRequests(employeeId, pageable);
        return ResponseEntity.ok(toPagedResponse(requests));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @GetMapping("/hr/branches/{branchId}/time-off-requests")
    public ResponseEntity<?> listTimeOffRequests(
            @PathVariable int branchId,
            @RequestParam(defaultValue = "PENDING") TimeOffRequestEntity.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth
    ) {
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        Pageable pageable = buildPageable(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Page<TimeOffRequestDto> list = timeOffService.getBranchRequests(branchId, status, pageable);
        return ResponseEntity.ok(toPagedResponse(list));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping("/hr/time-off-requests/{id}/decision")
    public ResponseEntity<?> decideTimeOffRequest(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestBody(required = false) @Valid TimeOffDecisionRequest body,
            Authentication auth
    ) {
        Integer currentId = currentEmployeeId(auth);
        if (currentId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        TimeOffRequestEntity request;
        try {
            request = timeOffService.getRequest(id);
        } catch (TimeOffRequestService.TimeOffRequestException ex) {
            return handleServiceError(ex);
        }

        Integer branchId = request.getBranchId();
        if (branchId == null) {
            log.warn("Time-off request {} missing branch reference", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Request missing branch reference"));
        }

        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        try {
            TimeOffDecisionResultDto result = timeOffService.decideTimeOffRequest(
                    id,
                    approve,
                    body != null ? body.comment() : null,
                    currentId
            );
            return ResponseEntity.ok(result.request());
        } catch (TimeOffRequestService.TimeOffRequestException ex) {
            return handleServiceError(ex);
        }
    }

    public record CreateTimeOffRequest(
            @NotBlank(message = "reason is required")
            @Size(max = 500, message = "reason must be under 500 characters")
            String reason
    ) {}

    public record TimeOffDecisionRequest(
            @Size(max = 500, message = "comment must be under 500 characters")
            String comment
    ) {}
}
