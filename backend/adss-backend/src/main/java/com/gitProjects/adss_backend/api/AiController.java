package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.ai.RequestTriageService;
import com.gitProjects.adss_backend.ai.ScheduleOptimizerService;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * AI-powered endpoints for smart scheduling and request analysis.
 * All processing is done locally - no external API calls.
 */
@RestController
@RequestMapping("/api/ai")
@PreAuthorize("hasRole('HR_MANAGER')")
public class AiController {

    private final RequestTriageService triageService;
    private final ScheduleOptimizerService optimizerService;
        private final HrAccessValidationService accessValidation;

    public AiController(
            RequestTriageService triageService,
                        ScheduleOptimizerService optimizerService,
                        HrAccessValidationService accessValidation
    ) {
        this.triageService = triageService;
        this.optimizerService = optimizerService;
                this.accessValidation = accessValidation;
    }

    /**
     * Analyze a time-off request reason and provide category, priority, and suggestions.
     * Enhanced with real staffing conflict analysis if context is provided.
     */
    @PostMapping("/triage")
    public ResponseEntity<?> analyzeTimeOffRequest(
            @Valid @RequestBody TriageRequest request
    ) {
        String reason = request.reason();
        RequestTriageService.TriageResult result;
        
        // Use enhanced analysis with staffing context if available
        if (request.employeeId() != null && request.branchId() != null && 
            request.date() != null && request.shiftType() != null) {
            result = triageService.analyzeRequestWithContext(
                    reason,
                    request.employeeId(),
                    request.branchId(),
                    request.date(),
                    request.shiftType()
            );
        } else {
            // Fallback to basic analysis
            result = triageService.analyzeRequest(reason);
        }

        Map<String, Object> response = new java.util.HashMap<>(Map.of(
                "category", result.category(),
                "priority", result.priority().name(),
                "priorityScore", result.priority().score,
                "confidence", result.confidence(),
                "detectedKeywords", result.detectedKeywords(),
                "suggestion", result.suggestion(),
                "isLocalAI", true,
                "model", "Rule-based NLP v1.0 with Staffing Analysis"
        ));
        
        // Add staffing analysis if available
        if (!result.staffingAnalysis().isEmpty()) {
            response.put("staffingAnalysis", result.staffingAnalysis());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get workload predictions for a week
     */
    @GetMapping("/predict-workload")
        public ResponseEntity<?> predictWorkload(
                        @RequestParam int branchId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
                        Authentication auth
        ) {
                String accessError = accessValidation.validateBranchAccess(auth, branchId);
                if (accessError != null) {
                        return ResponseEntity.status(403).body(Map.of("error", accessError));
                }

                Map<String, Double> predictions = optimizerService.predictWorkload(branchId, weekStart);

        return ResponseEntity.ok(Map.of(
                "branchId", branchId,
                                "weekStart", weekStart.toString(),
                "predictions", predictions,
                "isLocalAI", true,
                "model", "Moving Average Predictor v1.0",
                "note", "Based on historical patterns and day-of-week factors"
        ));
    }

        public record TriageRequest(
                @NotBlank(message = "reason is required") String reason,
                Integer employeeId,  // Optional: for staffing analysis
                Integer branchId,    // Optional: for staffing analysis
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,  // Optional: for staffing analysis
                ShiftEnums.ShiftType shiftType  // Optional: for staffing analysis
        ) {}
}
