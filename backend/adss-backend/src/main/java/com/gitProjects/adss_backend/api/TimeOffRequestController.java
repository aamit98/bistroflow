package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import com.gitProjects.adss_backend.hr.repo.TimeOffRequestRepository;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TimeOffRequestController {

    private final TimeOffRequestRepository timeOffRepo;
    private final NotificationRepository notificationRepo;
    private final EmployeeAccountRepository accountRepo;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public TimeOffRequestController(
            TimeOffRequestRepository timeOffRepo,
            NotificationRepository notificationRepo,
            EmployeeAccountRepository accountRepo,
            org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate
    ) {
        this.timeOffRepo = timeOffRepo;
        this.notificationRepo = notificationRepo;
        this.accountRepo = accountRepo;
        this.messagingTemplate = messagingTemplate;
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

    private boolean isHrManager(Authentication auth) {
        if (auth == null) return false;
        Object cred = auth.getCredentials();
        if (cred instanceof Boolean b) return b;
        return false;
    }

    // Employee creates a time-off request (for emergency, complaints, etc.)
    @PostMapping("/employees/{employeeId}/time-off-requests")
    public ResponseEntity<?> createTimeOffRequest(
            @PathVariable int employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam ShiftEnums.ShiftType shiftType,
            @RequestBody Map<String, String> body,
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

        Optional<EmployeeAccount> accountOpt = accountRepo.findByEmployeeId(employeeId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Employee not found"));
        }

        EmployeeAccount account = accountOpt.get();
        String reason = body.getOrDefault("reason", "");

        TimeOffRequestEntity entity = new TimeOffRequestEntity(
                employeeId,
                account.getBranchId(),
                date,
                shiftType,
                reason
        );
        timeOffRepo.save(entity);

        // Notify HR managers in this branch
        List<EmployeeAccount> hrManagers =
                accountRepo.findByBranchId(account.getBranchId()).stream()
                        .filter(EmployeeAccount::isHrManager)
                        .toList();
        for (EmployeeAccount hrAcc : hrManagers) {
            NotificationEntity n = new NotificationEntity(
                hrAcc.getEmployeeId(),
                "New time-off request",
                "Employee #" + employeeId + " requested time off on " + date + " (" + shiftType + ")" +
                    (reason != null && !reason.isBlank() ? " Reason: " + reason : ""),
                "TIME_OFF_REQUEST"
            );
            notificationRepo.save(n);
            // send real-time message to branch HR topic and user queue
            var payload = java.util.Map.of(
                "type", "TIME_OFF_REQUEST",
                "employeeId", employeeId,
                "date", date.toString(),
                "shiftType", shiftType.name(),
                "requestId", n.getId(),
                "reason", reason
            );
            messagingTemplate.convertAndSend("/topic/hr/branch/" + hrAcc.getBranchId(), (Object) payload);
            messagingTemplate.convertAndSendToUser(hrAcc.getEmployeeId().toString(), "/queue/notifications", (Object) payload);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }

    // HR: list requests in a branch by status
    @GetMapping("/hr/branches/{branchId}/time-off-requests")
    public ResponseEntity<?> listTimeOffRequests(
            @PathVariable int branchId,
            @RequestParam(defaultValue = "PENDING") TimeOffRequestEntity.Status status,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        List<TimeOffRequestEntity> list =
                timeOffRepo.findByBranchIdAndStatusOrderByCreatedAtAsc(branchId, status);
        return ResponseEntity.ok(list);
    }

    // HR: approve / reject a request
    @PostMapping("/hr/time-off-requests/{id}/decision")
    public ResponseEntity<?> decideTimeOffRequest(
            @PathVariable Long id,
            @RequestParam boolean approve,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth
    ) {
        Integer currentId = currentEmployeeId(auth);
        if (currentId == null || !isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        Optional<TimeOffRequestEntity> opt = timeOffRepo.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Request not found"));
        }

        TimeOffRequestEntity entity = opt.get();
        entity.setStatus(approve ? TimeOffRequestEntity.Status.APPROVED : TimeOffRequestEntity.Status.REJECTED);
        entity.setReviewedByEmployeeId(currentId);
        entity.setReviewedAt(java.time.LocalDateTime.now());
        if (body != null && body.containsKey("comment")) {
            entity.setDecisionComment(body.get("comment"));
        }
        timeOffRepo.save(entity);

        // Notify employee about decision
        String decisionBody = "Your request for " + entity.getDate() + " (" + entity.getShiftType() + ") was "
                + (approve ? "approved" : "rejected") + ".";
        if (entity.getDecisionComment() != null && !entity.getDecisionComment().isBlank()) {
            decisionBody += " Comment: " + entity.getDecisionComment();
        }

        NotificationEntity n = new NotificationEntity(
            entity.getEmployeeId(),
            approve ? "Time-off request approved" : "Time-off request rejected",
            decisionBody,
            "TIME_OFF_DECISION"
        );
        notificationRepo.save(n);

        // send real-time notification to employee
        var payload = java.util.Map.of(
            "type", "TIME_OFF_DECISION",
            "requestId", entity.getId(),
            "approved", approve,
            "date", entity.getDate().toString(),
            "shiftType", entity.getShiftType().name()
        );
        messagingTemplate.convertAndSendToUser(entity.getEmployeeId().toString(), "/queue/notifications", (Object) payload);

        return ResponseEntity.ok(entity);
    }
}
