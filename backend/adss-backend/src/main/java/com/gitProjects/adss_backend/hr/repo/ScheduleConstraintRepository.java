package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.ScheduleConstraint;
import com.gitProjects.adss_backend.hr.model.ShiftEnums;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleConstraintRepository extends JpaRepository<ScheduleConstraint, Long> {
    List<ScheduleConstraint> findByBranchIdAndWeekStartAndShiftType(Integer branchId, LocalDate weekStart, ShiftEnums.ShiftType shiftType);
    List<ScheduleConstraint> findByBranchIdAndWeekStart(Integer branchId, LocalDate weekStart);
    List<ScheduleConstraint> findByBranchIdAndWeekStartIsNull(Integer branchId);

    @Modifying
    @Transactional
    void deleteByBranchIdAndWeekStart(Integer branchId, LocalDate weekStart);
}
