package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.ShiftAssignmentEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ShiftAssignmentRepository
        extends JpaRepository<ShiftAssignmentEntity, Long> {

    List<ShiftAssignmentEntity> findByEmployeeIdAndDateBetween(
            Integer employeeId, LocalDate start, LocalDate end
    );

    List<ShiftAssignmentEntity> findByBranchIdAndDateBetween(
            Integer branchId, LocalDate start, LocalDate end
    );

    List<ShiftAssignmentEntity> findByBranchIdAndDateAndShiftType(
            Integer branchId, LocalDate date, ShiftEnums.ShiftType shiftType
    );

    List<ShiftAssignmentEntity> findByBranchIdAndDate(
            Integer branchId, LocalDate date
    );

    List<ShiftAssignmentEntity> findByEmployeeIdAndDate(
            Integer employeeId, LocalDate date
    );

    long countByBranchIdAndDate(Integer branchId, LocalDate date);

    // Find specific shift assignment for an employee
    Optional<ShiftAssignmentEntity> findByEmployeeIdAndDateAndShiftType(
            Integer employeeId, LocalDate date, ShiftEnums.ShiftType shiftType
    );

    // Delete shift assignment for an employee on a specific date/shift
    void deleteByEmployeeIdAndDateAndShiftType(
            Integer employeeId, LocalDate date, ShiftEnums.ShiftType shiftType
    );
}
