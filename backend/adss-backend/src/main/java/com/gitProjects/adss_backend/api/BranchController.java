package com.gitProjects.adss_backend.api;

import com.gitProjects.adss_backend.auth.EmployeeAccount;
import com.gitProjects.adss_backend.auth.EmployeeAccountRepository;
import com.gitProjects.adss_backend.hr.model.BranchEntity;
import com.gitProjects.adss_backend.hr.model.BranchRoleEntity;
import com.gitProjects.adss_backend.hr.model.BranchShiftTemplateEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import com.gitProjects.adss_backend.hr.repo.BranchRepository;
import com.gitProjects.adss_backend.hr.repo.BranchRoleRepository;
import com.gitProjects.adss_backend.service.HrAccessValidationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/branches")
public class BranchController {

    private final BranchRepository branchRepo;
    private final BranchRoleRepository roleRepo;
    private final EmployeeAccountRepository accountRepo;
    private final HrAccessValidationService accessValidation;

    public BranchController(BranchRepository branchRepo, BranchRoleRepository roleRepo, 
                            EmployeeAccountRepository accountRepo,
                            HrAccessValidationService accessValidation) {
        this.branchRepo = branchRepo;
        this.roleRepo = roleRepo;
        this.accountRepo = accountRepo;
        this.accessValidation = accessValidation;
    }

    // ---------- DTOs ----------

    public record ShiftTemplateDto(
            Long id,
            String shiftType,
            String startTime,
            String endTime,
            Double shiftHours
    ) {}

    public record BranchDto(
            Integer id,
            String name,
            String address,
            String city,
            String phone,
            String timezone,
            boolean active,
            List<ShiftTemplateDto> shiftTemplates
    ) {}

    public record CreateBranchRequest(
            String name,
            String address,
            String city,
            String phone,
            String timezone
    ) {}

    public record UpdateBranchRequest(
            String name,
            String address,
            String city,
            String phone,
            String timezone,
            Boolean active
    ) {}

    public record ShiftTemplateRequest(
            String shiftType,
            String startTime,
            String endTime
    ) {}

    // Role DTOs - baseHourlyRate is in agorot (cents), 3350 = ₪33.50
    public record BranchRoleDto(
            Long id,
            String code,
            String displayName,
            String description,
            String color,
            String icon,
            Integer baseHourlyRate,
            boolean requiresCertification,
            boolean canSupervise,
            int sortOrder,
            boolean active
    ) {}

    public record CreateRoleRequest(
            String code,
            String displayName,
            String description,
            String color,
            String icon,
            Integer baseHourlyRate,
            Boolean requiresCertification,
            Boolean canSupervise,
            Integer sortOrder
    ) {}

    public record UpdateRoleRequest(
            String displayName,
            String description,
            String color,
            String icon,
            Integer baseHourlyRate,
            Boolean requiresCertification,
            Boolean canSupervise,
            Integer sortOrder,
            Boolean active
    ) {}

    // ---------- Endpoints ----------

    @GetMapping
    public ResponseEntity<List<BranchDto>> getAllBranches() {
        List<BranchDto> branches = branchRepo.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(branches);
    }

    @GetMapping("/active")
    public ResponseEntity<List<BranchDto>> getActiveBranches(Authentication auth) {
        // Get the current user to check their restaurant assignment
        Integer currentEmployeeId = currentEmployeeId(auth);
        
        List<BranchEntity> allActive = branchRepo.findByActiveTrue();
        
        // If user is super admin, return all active branches
        if (isSuperAdmin(auth)) {
            return ResponseEntity.ok(allActive.stream().map(this::toDto).toList());
        }
        
        // If HR manager, filter to only their restaurant's branches
        if (currentEmployeeId != null && isHrManager(auth)) {
            Optional<EmployeeAccount> account = accountRepo.findByEmployeeId(currentEmployeeId);
            if (account.isPresent() && account.get().getRestaurantId() != null) {
                Long restaurantId = account.get().getRestaurantId();
                List<BranchDto> filtered = allActive.stream()
                        .filter(b -> b.getRestaurant() != null && 
                                    b.getRestaurant().getId().equals(restaurantId))
                        .map(this::toDto)
                        .toList();
                return ResponseEntity.ok(filtered);
            }
            // HR manager with no restaurant assignment - return empty list
            return ResponseEntity.ok(List.of());
        }
        
        // Regular employees - return only their branch
        if (currentEmployeeId != null) {
            Optional<EmployeeAccount> account = accountRepo.findByEmployeeId(currentEmployeeId);
            if (account.isPresent() && account.get().getBranchId() != null) {
                Integer branchId = account.get().getBranchId();
                List<BranchDto> filtered = allActive.stream()
                        .filter(b -> b.getId().equals(branchId))
                        .map(this::toDto)
                        .toList();
                return ResponseEntity.ok(filtered);
            }
        }
        
        return ResponseEntity.ok(List.of());
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
    
    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBranch(@PathVariable Integer id) {
        return branchRepo.findById(id)
                .map(b -> ResponseEntity.ok(toDto(b)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createBranch(
            @RequestBody CreateBranchRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        BranchEntity branch = new BranchEntity();
        branch.setName(request.name());
        branch.setAddress(request.address());
        branch.setCity(request.city());
        branch.setPhone(request.phone());
        if (request.timezone() != null) {
            branch.setTimezone(request.timezone());
        }

        // Add default shift templates
        addDefaultShiftTemplates(branch);

        BranchEntity saved = branchRepo.save(branch);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBranch(
            @PathVariable Integer id,
            @RequestBody UpdateBranchRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, id);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return branchRepo.findById(id)
                .map(branch -> {
                    if (request.name() != null) branch.setName(request.name());
                    if (request.address() != null) branch.setAddress(request.address());
                    if (request.city() != null) branch.setCity(request.city());
                    if (request.phone() != null) branch.setPhone(request.phone());
                    if (request.timezone() != null) branch.setTimezone(request.timezone());
                    if (request.active() != null) branch.setActive(request.active());

                    BranchEntity saved = branchRepo.save(branch);
                    return ResponseEntity.ok(toDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/shift-templates")
    public ResponseEntity<?> addShiftTemplate(
            @PathVariable Integer id,
            @RequestBody ShiftTemplateRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, id);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return branchRepo.findById(id)
                .map(branch -> {
                    ShiftEnums.ShiftType shiftType = ShiftEnums.ShiftType.valueOf(request.shiftType());
                    LocalTime startTime = LocalTime.parse(request.startTime());
                    LocalTime endTime = LocalTime.parse(request.endTime());

                    BranchShiftTemplateEntity template = new BranchShiftTemplateEntity(shiftType, startTime, endTime);
                    branch.addShiftTemplate(template);

                    branchRepo.save(branch);
                    return ResponseEntity.ok(toDto(branch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{branchId}/shift-templates/{templateId}")
    public ResponseEntity<?> updateShiftTemplate(
            @PathVariable Integer branchId,
            @PathVariable Long templateId,
            @RequestBody ShiftTemplateRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return branchRepo.findById(branchId)
                .map(branch -> {
                    branch.getShiftTemplates().stream()
                            .filter(t -> t.getId().equals(templateId))
                            .findFirst()
                            .ifPresent(template -> {
                                if (request.shiftType() != null) {
                                    template.setShiftType(ShiftEnums.ShiftType.valueOf(request.shiftType()));
                                }
                                if (request.startTime() != null) {
                                    template.setStartTime(LocalTime.parse(request.startTime()));
                                }
                                if (request.endTime() != null) {
                                    template.setEndTime(LocalTime.parse(request.endTime()));
                                }
                            });

                    branchRepo.save(branch);
                    return ResponseEntity.ok(toDto(branch));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- Role Endpoints ----------

    @GetMapping("/{id}/roles")
    public ResponseEntity<?> getBranchRoles(
            @PathVariable Integer id,
            @RequestParam(required = false) Boolean active
    ) {
        if (!branchRepo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<BranchRoleDto> roles;
        if (Boolean.TRUE.equals(active)) {
            roles = roleRepo.findByBranchIdAndActiveOrderBySortOrderAsc(id, true).stream()
                    .map(this::toRoleDto)
                    .toList();
        } else {
            roles = roleRepo.findByBranchIdOrderBySortOrderAsc(id).stream()
                    .map(this::toRoleDto)
                    .toList();
        }
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{branchId}/roles/{roleId}")
    public ResponseEntity<?> getBranchRole(
            @PathVariable Integer branchId,
            @PathVariable Long roleId
    ) {
        return roleRepo.findById(roleId)
                .filter(role -> role.getBranch().getId().equals(branchId))
                .map(role -> ResponseEntity.ok(toRoleDto(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/roles")
    public ResponseEntity<?> createRole(
            @PathVariable Integer id,
            @RequestBody CreateRoleRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, id);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return branchRepo.findById(id)
                .map(branch -> {
                    // Check for duplicate code
                    if (roleRepo.existsByBranchIdAndCode(id, request.code())) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Role code already exists for this branch"));
                    }

                    // Validate minimum wage (Israeli min wage = 3350 agorot = ₪33.50/hour)
                    int minWageAgorot = 3350;
                    if (request.baseHourlyRate() != null && request.baseHourlyRate() < minWageAgorot) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Hourly rate cannot be below minimum wage (₪33.50)"));
                    }

                    BranchRoleEntity role = new BranchRoleEntity();
                    role.setBranch(branch);
                    role.setCode(request.code().toUpperCase());
                    role.setDisplayName(request.displayName());
                    role.setDescription(request.description());
                    role.setColor(request.color() != null ? request.color() : "#6B7280");
                    role.setIcon(request.icon());
                    role.setBaseHourlyRate(request.baseHourlyRate() != null ? request.baseHourlyRate() : minWageAgorot);
                    role.setRequiresCertification(Boolean.TRUE.equals(request.requiresCertification()));
                    role.setCanSupervise(Boolean.TRUE.equals(request.canSupervise()));
                    role.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

                    BranchRoleEntity saved = roleRepo.save(role);
                    return ResponseEntity.status(HttpStatus.CREATED).body(toRoleDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{branchId}/roles/{roleId}")
    public ResponseEntity<?> updateRole(
            @PathVariable Integer branchId,
            @PathVariable Long roleId,
            @RequestBody UpdateRoleRequest request,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return roleRepo.findById(roleId)
                .filter(role -> role.getBranch().getId().equals(branchId))
                .map(role -> {
                    // Validate minimum wage (3350 agorot = ₪33.50/hour)
                    int minWageAgorot = 3350;
                    if (request.baseHourlyRate() != null && request.baseHourlyRate() < minWageAgorot) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Hourly rate cannot be below minimum wage (₪33.50)"));
                    }

                    if (request.displayName() != null) role.setDisplayName(request.displayName());
                    if (request.description() != null) role.setDescription(request.description());
                    if (request.color() != null) role.setColor(request.color());
                    if (request.icon() != null) role.setIcon(request.icon());
                    if (request.baseHourlyRate() != null) role.setBaseHourlyRate(request.baseHourlyRate());
                    if (request.requiresCertification() != null) role.setRequiresCertification(request.requiresCertification());
                    if (request.canSupervise() != null) role.setCanSupervise(request.canSupervise());
                    if (request.sortOrder() != null) role.setSortOrder(request.sortOrder());
                    if (request.active() != null) role.setActive(request.active());

                    BranchRoleEntity saved = roleRepo.save(role);
                    return ResponseEntity.ok(toRoleDto(saved));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{branchId}/roles/{roleId}")
    public ResponseEntity<?> deleteRole(
            @PathVariable Integer branchId,
            @PathVariable Long roleId,
            Authentication auth
    ) {
        if (!isHrManager(auth)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "HR manager access required"));
        }

        // Validate HR manager has access to this branch
        String accessError = accessValidation.validateBranchAccess(auth, branchId);
        if (accessError != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", accessError));
        }

        return roleRepo.findById(roleId)
                .filter(role -> role.getBranch().getId().equals(branchId))
                .map(role -> {
                    // Instead of deleting, mark as inactive (soft delete)
                    role.setActive(false);
                    roleRepo.save(role);
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ---------- Helper methods ----------

    private void addDefaultShiftTemplates(BranchEntity branch) {
        branch.addShiftTemplate(new BranchShiftTemplateEntity(
                ShiftEnums.ShiftType.MORNING,
                LocalTime.of(6, 0),
                LocalTime.of(14, 0)
        ));
        branch.addShiftTemplate(new BranchShiftTemplateEntity(
                ShiftEnums.ShiftType.EVENING,
                LocalTime.of(14, 0),
                LocalTime.of(22, 0)
        ));
    }

    private BranchDto toDto(BranchEntity entity) {
        List<ShiftTemplateDto> templates = entity.getShiftTemplates().stream()
                .map(t -> new ShiftTemplateDto(
                        t.getId(),
                        t.getShiftType().name(),
                        t.getStartTime().toString(),
                        t.getEndTime().toString(),
                        t.getShiftHours()
                ))
                .toList();

        return new BranchDto(
                entity.getId(),
                entity.getName(),
                entity.getAddress(),
                entity.getCity(),
                entity.getPhone(),
                entity.getTimezone(),
                entity.isActive(),
                templates
        );
    }

    private boolean isHrManager(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_HR_MANAGER".equals(a));
    }

    private BranchRoleDto toRoleDto(BranchRoleEntity entity) {
        return new BranchRoleDto(
                entity.getId(),
                entity.getCode(),
                entity.getDisplayName(),
                entity.getDescription(),
                entity.getColor(),
                entity.getIcon(),
                entity.getBaseHourlyRate(),
                entity.isRequiresCertification(),
                entity.isCanSupervise(),
                entity.getSortOrder() != null ? entity.getSortOrder() : 0,
                entity.isActive()
        );
    }
}
