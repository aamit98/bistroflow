package com.gitProjects.adss_backend.ai;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.*;
import com.gitProjects.adss_backend.hr.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Local "AI" service for smart request triage and categorization.
 * Uses rule-based NLP (no external API calls, no tokens).
 * 
 * Categories:
 * - MEDICAL: doctor, hospital, sick, health, illness, appointment
 * - ACADEMIC: exam, study, school, university, lecture, homework
 * - MILITARY: army, reserve, miluim, IDF, service
 * - FAMILY: wedding, funeral, baby, birth, family, child
 * - PERSONAL: vacation, travel, holiday, rest
 * - URGENT: emergency, urgent, critical, immediate
 * - OTHER: default
 */
@Service
public class RequestTriageService {
    
    @Autowired(required = false)
    private EmployeeAccountRepository employeeRepo;
    
    @Autowired(required = false)
    private ScheduleAssignmentRepository assignmentRepo;
    
    @Autowired(required = false)
    private ScheduleConstraintRepository constraintRepo;
    
    @Autowired(required = false)
    private EmployeeAvailabilityRepository availabilityRepo;

    // Priority levels
    public enum Priority {
        HIGH(3),      // Medical, Military, Family emergencies
        MEDIUM(2),    // Academic, planned family events
        LOW(1),       // Personal, vacation
        NORMAL(0);    // Default
        
        public final int score;
        Priority(int score) { this.score = score; }
    }

    // Category patterns (case-insensitive)
    private static final Map<String, List<Pattern>> CATEGORY_PATTERNS = Map.of(
        "MEDICAL", List.of(
            Pattern.compile("(?i)\\b(doctor|hospital|sick|ill|health|medical|clinic|appointment|surgery|checkup|dentist|nurse|medicine|pain|fever)\\b"),
            Pattern.compile("(?i)\\b(רופא|בית\\s*חולים|חולה|בדיקה)\\b")
        ),
        "ACADEMIC", List.of(
            Pattern.compile("(?i)\\b(exam|study|school|university|college|lecture|homework|test|class|course|degree|graduation|semester)\\b"),
            Pattern.compile("(?i)\\b(מבחן|לימודים|אוניברסיטה|בחינה)\\b")
        ),
        "MILITARY", List.of(
            Pattern.compile("(?i)\\b(army|military|reserve|miluim|idf|service|soldier|base|duty|training)\\b"),
            Pattern.compile("(?i)\\b(צבא|מילואים|צה\"ל|שירות)\\b")
        ),
        "FAMILY", List.of(
            Pattern.compile("(?i)\\b(wedding|funeral|baby|birth|family|child|parent|mother|father|spouse|wife|husband|kid|son|daughter|grandmother|grandfather|bar\\s*mitzvah|bat\\s*mitzvah)\\b"),
            Pattern.compile("(?i)\\b(חתונה|לוויה|תינוק|משפחה|בר\\s*מצווה)\\b")
        ),
        "URGENT", List.of(
            Pattern.compile("(?i)\\b(emergency|urgent|critical|immediate|asap|crisis|accident)\\b"),
            Pattern.compile("(?i)\\b(חירום|דחוף|מיידי|תאונה)\\b")
        ),
        "PERSONAL", List.of(
            Pattern.compile("(?i)\\b(vacation|travel|holiday|rest|trip|flight|abroad|personal|private)\\b"),
            Pattern.compile("(?i)\\b(חופשה|נסיעה|טיול|חו\"ל)\\b")
        )
    );

    public record TriageResult(
        String category,
        Priority priority,
        double confidence,
        List<String> detectedKeywords,
        String suggestion,
        Map<String, Object> staffingAnalysis // New: real staffing impact analysis
    ) {}
    
    /**
     * Enhanced analysis with real staffing conflict detection
     */
    public TriageResult analyzeRequestWithContext(
            String reason,
            Integer employeeId,
            Integer branchId,
            LocalDate requestDate,
            ShiftEnums.ShiftType shiftType
    ) {
        // First do basic categorization
        TriageResult baseResult = analyzeRequest(reason);
        
        // Then add real staffing analysis if context provided
        Map<String, Object> staffingAnalysis = new HashMap<>();
        if (employeeId != null && branchId != null && requestDate != null && shiftType != null) {
            staffingAnalysis = analyzeStaffingImpact(employeeId, branchId, requestDate, shiftType);
        }
        
        // Enhance suggestion based on staffing analysis
        String enhancedSuggestion = enhanceSuggestionWithStaffing(baseResult.suggestion(), staffingAnalysis);
        
        return new TriageResult(
            baseResult.category(),
            baseResult.priority(),
            baseResult.confidence(),
            baseResult.detectedKeywords(),
            enhancedSuggestion,
            staffingAnalysis
        );
    }

    /**
     * Analyze a time-off request reason and categorize it (basic analysis without context)
     */
    public TriageResult analyzeRequest(String reason) {
        if (reason == null || reason.isBlank()) {
            return new TriageResult("OTHER", Priority.NORMAL, 0.0, List.of(), 
                "No reason provided. Consider asking for more details.", Map.of());
        }

        String bestCategory = "OTHER";
        int maxMatches = 0;
        List<String> keywords = new java.util.ArrayList<>();

        for (var entry : CATEGORY_PATTERNS.entrySet()) {
            String category = entry.getKey();
            int matches = 0;
            
            for (Pattern pattern : entry.getValue()) {
                var matcher = pattern.matcher(reason);
                while (matcher.find()) {
                    matches++;
                    keywords.add(matcher.group());
                }
            }
            
            if (matches > maxMatches) {
                maxMatches = matches;
                bestCategory = category;
            }
        }

        double confidence = Math.min(1.0, maxMatches * 0.25);
        Priority priority = determinePriority(bestCategory, reason);
        String suggestion = generateSuggestion(bestCategory, priority);

        return new TriageResult(bestCategory, priority, confidence, keywords, suggestion, Map.of());
    }

    private Priority determinePriority(String category, String reason) {
        // Check for urgent indicators regardless of category
        if (CATEGORY_PATTERNS.get("URGENT").stream()
                .anyMatch(p -> p.matcher(reason).find())) {
            return Priority.HIGH;
        }

        return switch (category) {
            case "MEDICAL" -> Priority.HIGH;
            case "MILITARY" -> Priority.HIGH;
            case "FAMILY" -> {
                // Funeral/emergency = high, wedding/planned = medium
                if (reason.toLowerCase().contains("funeral") || 
                    reason.toLowerCase().contains("emergency") ||
                    reason.toLowerCase().contains("לוויה")) {
                    yield Priority.HIGH;
                }
                yield Priority.MEDIUM;
            }
            case "ACADEMIC" -> Priority.MEDIUM;
            case "PERSONAL" -> Priority.LOW;
            default -> Priority.NORMAL;
        };
    }

    private String generateSuggestion(String category, Priority priority) {
        return switch (category) {
            case "MEDICAL" -> "Medical request - consider approving. May require documentation.";
            case "MILITARY" -> "Military duty - usually mandatory. Approve recommended.";
            case "FAMILY" -> priority == Priority.HIGH 
                ? "Family emergency - consider immediate approval."
                : "Family event - standard review process.";
            case "ACADEMIC" -> "Academic request - verify exam schedule if needed.";
            case "PERSONAL" -> "Personal time - check staffing levels before approving.";
            case "URGENT" -> "⚠️ Urgent request - requires immediate attention.";
            default -> "Standard request - apply normal review process.";
        };
    }
    
    /**
     * Analyze the staffing impact if this time-off request is approved
     */
    private Map<String, Object> analyzeStaffingImpact(
            Integer employeeId,
            Integer branchId,
            LocalDate requestDate,
            ShiftEnums.ShiftType shiftType
    ) {
        Map<String, Object> analysis = new HashMap<>();
        
        if (employeeRepo == null || assignmentRepo == null || constraintRepo == null) {
            return analysis; // Service not fully initialized, skip real analysis
        }
        
        try {
            // Get employee info
            Optional<EmployeeAccount> employeeOpt = employeeRepo.findByEmployeeId(employeeId);
            if (employeeOpt.isEmpty()) {
                return analysis;
            }
            EmployeeAccount employee = employeeOpt.get();
            
            // Check if employee is already assigned to this shift
            List<ScheduleAssignment> existingAssignments = assignmentRepo
                    .findByBranchIdAndShiftDateAndShiftType(branchId, requestDate, shiftType);
            boolean hasExistingAssignment = existingAssignments.stream()
                    .anyMatch(a -> a.getEmployeeId().equals(employeeId));
            
            analysis.put("hasExistingAssignment", hasExistingAssignment);
            analysis.put("employeeRole", employee.getPrimaryRole());
            analysis.put("employeeRoles", employee.getRoles());
            
            // Get constraints for this shift
            LocalDate weekStart = requestDate.with(DayOfWeek.SUNDAY);
            List<ScheduleConstraint> constraints = constraintRepo
                    .findByBranchIdAndWeekStart(branchId, weekStart)
                    .stream()
                    .filter(c -> c.getShiftType() == shiftType)
                    .toList();
            
            // Count how many employees are already assigned for each role
            Map<String, Integer> roleAssignments = new HashMap<>();
            for (ScheduleAssignment assignment : existingAssignments) {
                String role = assignment.getRole();
                if (role != null) {
                    roleAssignments.merge(role, 1, (a, b) -> a + b);
                }
            }
            
            // Check staffing adequacy
            Map<String, Object> roleAnalysis = new HashMap<>();
            boolean willCauseShortage = false;
            int criticalShortages = 0;
            
            for (ScheduleConstraint constraint : constraints) {
                String role = constraint.getRoleRequired();
                int assigned = roleAssignments.getOrDefault(role, 0);
                int minRequired = constraint.getMinRequired();
                int idealCount = constraint.getIdealCount() != null ? constraint.getIdealCount() : minRequired;
                
                // If this employee has this role and is assigned, removing them could cause shortage
                boolean employeeHasRole = employee.getRoles() != null && employee.getRoles().contains(role);
                boolean employeeAssigned = existingAssignments.stream()
                        .anyMatch(a -> a.getEmployeeId().equals(employeeId) && 
                                     (a.getRole() == null || a.getRole().equals(role)));
                
                int wouldBeAssigned = employeeAssigned ? assigned - 1 : assigned;
                boolean wouldBeShort = wouldBeAssigned < minRequired;
                boolean wouldBeBelowIdeal = wouldBeAssigned < idealCount;
                
                if (employeeHasRole && wouldBeShort) {
                    willCauseShortage = true;
                    criticalShortages++;
                }
                
                Map<String, Object> roleInfo = new HashMap<>();
                roleInfo.put("minRequired", minRequired);
                roleInfo.put("idealCount", idealCount);
                roleInfo.put("currentlyAssigned", assigned);
                roleInfo.put("wouldBeAssigned", wouldBeAssigned);
                roleInfo.put("wouldBeShort", wouldBeShort);
                roleInfo.put("wouldBeBelowIdeal", wouldBeBelowIdeal);
                roleInfo.put("employeeHasRole", employeeHasRole);
                roleInfo.put("employeeAssigned", employeeAssigned);
                
                roleAnalysis.put(role, roleInfo);
            }
            
            // Count available replacements
            List<EmployeeAccount> branchEmployees = employeeRepo.findByBranchId(branchId);
            int availableReplacements = 0;
            for (EmployeeAccount emp : branchEmployees) {
                if (emp.getEmployeeId().equals(employeeId)) continue; // Skip requesting employee
                
                // Check if employee is available (has availability record for this day/shift)
                List<EmployeeAvailabilityEntity> availability = availabilityRepo
                        .findByEmployeeIdAndWeekStart(emp.getEmployeeId(), weekStart);
                
                boolean isAvailable = availability.stream()
                        .anyMatch(a -> {
                            LocalDate slotDate = weekStart.plusDays(dayOffset(a.getDayOfWeek()));
                            return slotDate.equals(requestDate) && 
                                   a.getShiftType() == shiftType && 
                                   a.isAvailable();
                        });
                
                // If no availability record, default to available
                if (availability.isEmpty()) {
                    isAvailable = true;
                }
                
                // Check if employee has overlapping roles
                boolean hasMatchingRole = false;
                for (ScheduleConstraint constraint : constraints) {
                    if (emp.getRoles() != null && emp.getRoles().contains(constraint.getRoleRequired())) {
                        hasMatchingRole = true;
                        break;
                    }
                }
                
                // Check if already assigned to this shift
                boolean alreadyAssigned = existingAssignments.stream()
                        .anyMatch(a -> a.getEmployeeId().equals(emp.getEmployeeId()));
                
                if (isAvailable && hasMatchingRole && !alreadyAssigned) {
                    availableReplacements++;
                }
            }
            
            analysis.put("willCauseShortage", willCauseShortage);
            analysis.put("criticalShortages", criticalShortages);
            analysis.put("availableReplacements", availableReplacements);
            analysis.put("roleAnalysis", roleAnalysis);
            analysis.put("riskLevel", calculateRiskLevel(willCauseShortage, criticalShortages, availableReplacements));
            
        } catch (Exception e) {
            // If analysis fails, just return empty map - don't break the request
            analysis.put("error", "Analysis unavailable");
        }
        
        return analysis;
    }
    
    private int dayOffset(ShiftEnums.DayOfWeekCode dayCode) {
        return switch (dayCode) {
            case SUNDAY -> 0;
            case MONDAY -> 1;
            case TUESDAY -> 2;
            case WEDNESDAY -> 3;
            case THURSDAY -> 4;
            case FRIDAY -> 5;
            case SATURDAY -> 6;
        };
    }
    
    private String calculateRiskLevel(boolean willCauseShortage, int criticalShortages, int availableReplacements) {
        if (willCauseShortage && criticalShortages > 1) {
            return "HIGH";
        } else if (willCauseShortage) {
            return "MEDIUM";
        } else if (availableReplacements == 0) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    private String enhanceSuggestionWithStaffing(String baseSuggestion, Map<String, Object> staffingAnalysis) {
        if (staffingAnalysis.isEmpty()) {
            return baseSuggestion;
        }
        
        String riskLevel = (String) staffingAnalysis.getOrDefault("riskLevel", "UNKNOWN");
        Boolean willCauseShortage = (Boolean) staffingAnalysis.getOrDefault("willCauseShortage", false);
        Integer availableReplacements = (Integer) staffingAnalysis.getOrDefault("availableReplacements", 0);
        Integer criticalShortages = (Integer) staffingAnalysis.getOrDefault("criticalShortages", 0);
        
        StringBuilder enhanced = new StringBuilder(baseSuggestion);
        enhanced.append(" ");
        
        if ("HIGH".equals(riskLevel)) {
            enhanced.append("⚠️ HIGH RISK: Approving will cause ").append(criticalShortages)
                    .append(" critical staffing shortage(s). Only ").append(availableReplacements)
                    .append(" replacement(s) available. Consider denying or finding coverage first.");
        } else if ("MEDIUM".equals(riskLevel)) {
            if (willCauseShortage) {
                enhanced.append("⚠️ MEDIUM RISK: Approving will cause staffing shortage. ")
                        .append(availableReplacements).append(" potential replacement(s) available.");
            } else {
                enhanced.append("ℹ️ MEDIUM RISK: Limited replacement options (").append(availableReplacements)
                        .append("). Review carefully.");
            }
        } else {
            enhanced.append("✅ LOW RISK: ").append(availableReplacements)
                    .append(" potential replacement(s) available. Safe to approve.");
        }
        
        return enhanced.toString();
    }
}
