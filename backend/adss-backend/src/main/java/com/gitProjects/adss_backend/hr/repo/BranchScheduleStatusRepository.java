package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.BranchScheduleStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface BranchScheduleStatusRepository
        extends JpaRepository<BranchScheduleStatusEntity, Long> {

    Optional<BranchScheduleStatusEntity> findByBranchIdAndWeekStart(
            Integer branchId, LocalDate weekStart
    );

    boolean existsByBranchIdAndWeekStartAndPublishedTrue(
            Integer branchId, LocalDate weekStart
    );
}
