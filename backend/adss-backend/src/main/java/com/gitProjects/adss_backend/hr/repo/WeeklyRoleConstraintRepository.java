package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.WeeklyRoleConstraintEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface WeeklyRoleConstraintRepository
        extends JpaRepository<WeeklyRoleConstraintEntity, Long> {

    List<WeeklyRoleConstraintEntity> findByBranchIdAndWeekStart(
            Integer branchId, LocalDate weekStart
    );
}
