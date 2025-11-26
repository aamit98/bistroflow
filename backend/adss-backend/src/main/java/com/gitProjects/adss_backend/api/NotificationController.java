package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.hr.model.NotificationEntity;
import com.gitProjects.adss_backend.hr.repo.NotificationRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    private Integer currentEmployeeId(Authentication auth) {
        if (auth == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Integer i) {
            return i;
        }
        if (principal instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getMyNotifications(Authentication auth) {
        Integer employeeId = currentEmployeeId(auth);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        List<NotificationEntity> list =
                notificationRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(Authentication auth) {
        Integer employeeId = currentEmployeeId(auth);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        long count = notificationRepository.countByEmployeeIdAndReadFalse(employeeId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id, Authentication auth) {
        Integer employeeId = currentEmployeeId(auth);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        return notificationRepository.findById(id)
                .map(n -> {
                    if (!employeeId.equals(n.getEmployeeId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Cannot modify notifications of other employees"));
                    }
                    n.setRead(true);
                    notificationRepository.save(n);
                    return ResponseEntity.ok(Map.of("status", "ok"));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Notification not found")));
    }

    @PostMapping("/clear")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> clearAll(Authentication auth) {
        Integer employeeId = currentEmployeeId(auth);
        if (employeeId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        notificationRepository.deleteByEmployeeId(employeeId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
