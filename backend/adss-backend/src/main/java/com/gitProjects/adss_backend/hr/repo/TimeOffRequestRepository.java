package com.gitProjects.adss_backend.hr.repo;

import com.gitProjects.adss_backend.hr.model.TimeOffRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TimeOffRequestRepository extends JpaRepository<TimeOffRequestEntity, Long> {

    List<TimeOffRequestEntity> findByEmployeeIdOrderByCreatedAtDesc(Integer employeeId);

    List<TimeOffRequestEntity> findByBranchIdAndStatusOrderByCreatedAtAsc(
            Integer branchId,
            TimeOffRequestEntity.Status status
    );

    List<TimeOffRequestEntity> findByBranchIdAndDateBetween(
            Integer branchId,
            LocalDate start,
            LocalDate end
    );
}
