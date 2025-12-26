package com.gitProjects.adss_backend.service;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository.HrManagerContact;
import com.gitProjects.adss_backend.api.dto.TimeOffDecisionResultDto;
import com.gitProjects.adss_backend.api.dto.TimeOffRequestDto;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import com.gitProjects.adss_backend.hr.model.ScheduleAssignment;
import com.gitProjects.adss_backend.hr.model.ShiftAssignmentEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import com.gitProjects.adss_backend.hr.repo.BranchScheduleStatusRepository;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import com.gitProjects.adss_backend.hr.repo.ScheduleAssignmentRepository;
import com.gitProjects.adss_backend.hr.repo.ShiftAssignmentRepository;
import com.gitProjects.adss_backend.hr.repo.TimeOffRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TimeOffRequestService {

    private static final Logger log = LoggerFactory.getLogger(TimeOffRequestService.class);

    private final TimeOffRequestRepository timeOffRepo;
    private final NotificationRepository notificationRepo;
    private final EmployeeAccountRepository accountRepo;
    private final ShiftAssignmentRepository shiftAssignmentRepo;
    private final ScheduleAssignmentRepository scheduleAssignmentRepo;
    private final BranchScheduleStatusRepository scheduleStatusRepo;
    private final BranchRepository branchRepo;
    private final SimpMessagingTemplate messagingTemplate;

    public TimeOffRequestService(
            TimeOffRequestRepository timeOffRepo,
            NotificationRepository notificationRepo,
            EmployeeAccountRepository accountRepo,
            ShiftAssignmentRepository shiftAssignmentRepo,
            ScheduleAssignmentRepository scheduleAssignmentRepo,
            BranchScheduleStatusRepository scheduleStatusRepo,
            BranchRepository branchRepo,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.timeOffRepo = timeOffRepo;
        this.notificationRepo = notificationRepo;
        this.accountRepo = accountRepo;
        this.shiftAssignmentRepo = shiftAssignmentRepo;
        this.scheduleAssignmentRepo = scheduleAssignmentRepo;
        this.scheduleStatusRepo = scheduleStatusRepo;
        this.branchRepo = branchRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public TimeOffRequestDto createTimeOffRequest(
            int employeeId,
            LocalDate date,
            ShiftEnums.ShiftType shiftType,
            String rawReason
    ) {
        EmployeeAccount account = accountRepo.findByEmployeeId(employeeId)
                .orElseThrow(() -> new TimeOffRequestException(HttpStatus.NOT_FOUND, "Employee not found"));

        String reason = sanitizeReason(rawReason);
        TimeOffRequestEntity entity = new TimeOffRequestEntity(
                employeeId,
                account.getBranchId(),
                date,
                shiftType,
                reason
        );
        timeOffRepo.save(entity);

        String requesterName = account.getName() != null && !account.getName().isBlank()
                ? account.getName()
                : "Employee #" + account.getEmployeeId();
        notifyHrManagers(account.getBranchId(), entity, reason, requesterName);
        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public Page<TimeOffRequestDto> getEmployeeRequests(int employeeId, Pageable pageable) {
        return timeOffRepo.findByEmployeeId(employeeId, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public TimeOffRequestEntity getRequest(Long requestId) {
        return timeOffRepo.findById(requestId)
                .orElseThrow(() -> new TimeOffRequestException(HttpStatus.NOT_FOUND, "Request not found"));
    }

    @Transactional(readOnly = true)
    public Page<TimeOffRequestDto> getBranchRequests(
            Integer branchId,
            TimeOffRequestEntity.Status status,
            Pageable pageable
    ) {
        return timeOffRepo.findByBranchIdAndStatus(branchId, status, pageable)
                .map(this::toDto);
    }

    @Transactional
    public TimeOffDecisionResultDto decideTimeOffRequest(
            Long requestId,
            boolean approve,
            String decisionComment,
            Integer reviewerEmployeeId
    ) {
        TimeOffRequestEntity entity = getRequest(requestId);

        if (entity.getBranchId() == null) {
            throw new TimeOffRequestException(HttpStatus.BAD_REQUEST, "Request missing branch reference");
        }

        entity.setStatus(approve ? TimeOffRequestEntity.Status.APPROVED : TimeOffRequestEntity.Status.REJECTED);
        entity.setReviewedByEmployeeId(reviewerEmployeeId);
        entity.setReviewedAt(LocalDateTime.now());
        if (decisionComment != null && !decisionComment.isBlank()) {
            entity.setDecisionComment(decisionComment.trim());
        }
        timeOffRepo.save(entity);

        boolean schedulePublished = false;
        boolean shiftRemoved = false;
        if (approve) {
            LocalDate weekStart = entity.getDate().with(DayOfWeek.SUNDAY);
            schedulePublished = scheduleStatusRepo
                    .findByBranchIdAndWeekStart(entity.getBranchId(), weekStart)
                    .map(BranchScheduleStatusEntity::isPublished)
                    .orElse(false);
            shiftRemoved = removeShiftAssignments(entity);
        }

        sendDecisionNotification(entity, approve, shiftRemoved, schedulePublished);
        return new TimeOffDecisionResultDto(toDto(entity), shiftRemoved, schedulePublished);
    }

    private void notifyHrManagers(Integer branchId,
                                  TimeOffRequestEntity entity,
                                  String reason,
                                  String requesterName) {
        List<HrManagerContact> hrManagers = resolveHrManagers(branchId);
        if (hrManagers.isEmpty()) {
            log.debug("No HR managers found for branch {} when handling time-off request {}", branchId, entity.getId());
            return;
        }

        for (HrManagerContact hrAccount : hrManagers) {
            NotificationEntity notification = new NotificationEntity(
                    hrAccount.getEmployeeId(),
                    "New time-off request",
                    requesterName + " requested time off on " + entity.getDate() + " (" + entity.getShiftType() + ")"
                            + (!reason.isBlank() ? " Reason: " + reason : ""),
                    "TIME_OFF_REQUEST"
            );
            notificationRepo.save(notification);
            log.debug("Created time-off notification {} for HR manager {}", notification.getId(), hrAccount.getEmployeeId());

            Map<String, Object> payload = new HashMap<>();
            payload.put("type", "TIME_OFF_REQUEST");
            payload.put("employeeId", entity.getEmployeeId());
            payload.put("date", entity.getDate().toString());
            payload.put("shiftType", entity.getShiftType().name());
            payload.put("requestId", notification.getId());
            payload.put("reason", reason);

            if (hrAccount.getBranchId() != null) {
                messagingTemplate.convertAndSend("/topic/hr/branch/" + hrAccount.getBranchId(), (Object) payload);
            }
            messagingTemplate.convertAndSendToUser(
                    hrAccount.getEmployeeId().toString(),
                    "/queue/notifications",
                    payload
            );
        }
    }

    private void sendDecisionNotification(TimeOffRequestEntity entity,
                                          boolean approved,
                                          boolean shiftRemoved,
                                          boolean schedulePublished) {
        StringBuilder body = new StringBuilder("Your request for ")
                .append(entity.getDate())
                .append(" (")
                .append(entity.getShiftType())
                .append(") was ")
                .append(approved ? "approved" : "rejected")
                .append('.');

        if (shiftRemoved) {
            body.append(" Your shift assignment has been automatically removed.");
            if (schedulePublished) {
                body.append(" Note: The schedule was already published, so HR may need to find a replacement.");
            }
        }

        if (entity.getDecisionComment() != null && !entity.getDecisionComment().isBlank()) {
            body.append(" Comment: ").append(entity.getDecisionComment());
        }

        NotificationEntity notification = new NotificationEntity(
                entity.getEmployeeId(),
                approved ? "Time-off request approved" : "Time-off request rejected",
                body.toString(),
                "TIME_OFF_DECISION"
        );
        notificationRepo.save(notification);

        Map<String, Object> payload = Map.of(
                "type", "TIME_OFF_DECISION",
                "requestId", entity.getId(),
                "approved", approved,
                "date", entity.getDate().toString(),
                "shiftType", entity.getShiftType().name(),
                "shiftRemoved", shiftRemoved
        );
        messagingTemplate.convertAndSendToUser(
                entity.getEmployeeId().toString(),
                "/queue/notifications",
                payload
        );
    }

    private boolean removeShiftAssignments(TimeOffRequestEntity entity) {
        boolean shiftRemoved = false;

        shiftRemoved = shiftAssignmentRepo
                .findByEmployeeIdAndDateAndShiftType(entity.getEmployeeId(), entity.getDate(), entity.getShiftType())
                .map(assignment -> {
                    shiftAssignmentRepo.delete(assignment);
                    log.info("Removed legacy shift assignment for employee {} on {} {}", entity.getEmployeeId(), entity.getDate(), entity.getShiftType());
                    return true;
                })
                .orElse(false);

        List<ScheduleAssignment> scheduleAssignments = scheduleAssignmentRepo
                .findByBranchIdAndShiftDateAndShiftType(entity.getBranchId(), entity.getDate(), entity.getShiftType());
        for (ScheduleAssignment assignment : scheduleAssignments) {
            if (assignment.getEmployeeId().equals(entity.getEmployeeId())) {
                scheduleAssignmentRepo.delete(assignment);
                shiftRemoved = true;
                log.info("Removed schedule assignment for employee {} on {} {}", entity.getEmployeeId(), entity.getDate(), entity.getShiftType());
            }
        }

        return shiftRemoved;
    }

    private List<HrManagerContact> resolveHrManagers(Integer branchId) {
        if (branchId == null) {
            return List.of();
        }

        List<HrManagerContact> hrManagers = new ArrayList<>();
        BranchEntity branch = branchRepo.findById(branchId).orElse(null);
        if (branch != null) {
            Long restaurantId = branch.getRestaurantId();
            if (restaurantId != null) {
                hrManagers = accountRepo.findHrManagerContactsByRestaurantId(restaurantId);
                if (!hrManagers.isEmpty()) {
                    log.debug("Found {} HR managers for restaurant {}", hrManagers.size(), restaurantId);
                    return hrManagers;
                }
            }
        }

        hrManagers = accountRepo.findHrManagerContactsByBranchId(branchId);
        if (!hrManagers.isEmpty()) {
            log.debug("Fallback: found {} HR managers in branch {}", hrManagers.size(), branchId);
        }
        return hrManagers;
    }

    private String sanitizeReason(String rawReason) {
        if (rawReason == null) {
            throw new TimeOffRequestException(HttpStatus.BAD_REQUEST, "Reason is required");
        }
        String reason = rawReason.trim();
        if (reason.isEmpty()) {
            throw new TimeOffRequestException(HttpStatus.BAD_REQUEST, "Reason must not be blank");
        }
        return reason;
    }

    private TimeOffRequestDto toDto(TimeOffRequestEntity entity) {
        return new TimeOffRequestDto(
                entity.getId(),
                entity.getEmployeeId(),
                entity.getBranchId(),
                entity.getDate(),
                entity.getShiftType(),
                entity.getReason(),
                entity.getStatus(),
                entity.getReviewedByEmployeeId(),
                entity.getReviewedAt(),
                entity.getDecisionComment(),
                entity.getCreatedAt()
        );
    }

    public static class TimeOffRequestException extends RuntimeException {
        private final HttpStatus status;

        public TimeOffRequestException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
