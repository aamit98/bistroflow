package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.ShiftAssignmentEntity;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

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
}
